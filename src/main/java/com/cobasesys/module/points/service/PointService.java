package com.cobasesys.module.points.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.common.util.IdGenerator;
import com.cobasesys.module.points.dto.PointDTO;
import com.cobasesys.module.points.entity.*;
import com.cobasesys.module.points.repository.*;
import com.cobasesys.module.system.entity.ExternalSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointActionRepository actionRepository;
    private final PointRuleRepository ruleRepository;
    private final PointAccountRepository accountRepository;
    private final PointTransactionRepository transactionRepository;
    private final PointRuleEngine ruleEngine;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    // ==================== Open API ====================

    @Transactional
    public PointDTO.TransactionResult earn(ExternalSystem system, PointDTO.EarnRequest request) {
        Long tenantId = system.getTenantId();
        String idempotentKey = system.getId() + ":" + request.getBizOrderNo() + ":" + request.getActionCode();

        Optional<PointTransaction> existing = transactionRepository.findByIdempotentKey(idempotentKey);
        if (existing.isPresent()) {
            return buildTransactionResult(existing.get(), null);
        }

        PointAction action = actionRepository.findBySystemIdAndActionCode(system.getId(), request.getActionCode())
                .orElseThrow(() -> new BizException(ErrorCode.POINT_ACTION_NOT_FOUND));
        if (action.getStatus() != 1) throw new BizException(ErrorCode.POINT_ACTION_DISABLED);

        List<PointRule> rules = ruleRepository.findActiveRules(action.getId(), LocalDateTime.now());
        if (rules.isEmpty()) throw new BizException(ErrorCode.POINT_RULE_NOT_FOUND);
        PointRule rule = rules.get(0);

        checkDailyMonthlyLimit(rule, tenantId, request.getUserId(), action.getId());

        long points = ruleEngine.calculate(rule, request.getBizAmount());
        if (points <= 0) throw new BizException(ErrorCode.PARAM_INVALID, "计算积分为0");

        PointAccount account = getOrCreateAccount(tenantId, request.getUserId());

        int updated = accountRepository.earnPoints(account.getId(), points, account.getVersion());
        if (updated == 0) {
            account = accountRepository.findById(account.getId()).orElseThrow();
            updated = accountRepository.earnPoints(account.getId(), points, account.getVersion());
            if (updated == 0) throw new BizException(ErrorCode.SYSTEM_ERROR, "并发冲突，请重试");
        }

        PointTransaction tx = new PointTransaction();
        tx.setTenantId(tenantId);
        tx.setTransactionNo(IdGenerator.pointTransactionNo());
        tx.setAccountId(account.getId());
        tx.setSystemId(system.getId());
        tx.setActionId(action.getId());
        tx.setRuleId(rule.getId());
        tx.setDirection(1);
        tx.setPoints(points);
        tx.setBalanceBefore(account.getBalance());
        tx.setBalanceAfter(account.getBalance() + points);
        tx.setBizOrderNo(request.getBizOrderNo());
        tx.setBizAmount(request.getBizAmount());
        tx.setRemark(request.getRemark());
        tx.setIdempotentKey(idempotentKey);
        transactionRepository.save(tx);

        incrementLimitCounter(tenantId, request.getUserId(), action.getId());

        eventPublisher.publishEvent(new PointChangeEvent(this, tenantId, request.getUserId(),
                points, 1, account.getBalance() + points));

        return buildTransactionResult(tx, rule.getRuleName());
    }

    @Transactional
    public PointDTO.TransactionResult deduct(ExternalSystem system, PointDTO.DeductRequest request) {
        Long tenantId = system.getTenantId();
        String idempotentKey = system.getId() + ":" + request.getBizOrderNo() + ":deduct:" + request.getActionCode();

        Optional<PointTransaction> existing = transactionRepository.findByIdempotentKey(idempotentKey);
        if (existing.isPresent()) return buildTransactionResult(existing.get(), null);

        PointAction action = actionRepository.findBySystemIdAndActionCode(system.getId(), request.getActionCode())
                .orElseThrow(() -> new BizException(ErrorCode.POINT_ACTION_NOT_FOUND));

        PointAccount account = accountRepository.findByTenantIdAndUserId(tenantId, request.getUserId())
                .orElseThrow(() -> new BizException(ErrorCode.POINT_ACCOUNT_NOT_FOUND));

        if (account.getBalance() < request.getPoints()) {
            throw new BizException(ErrorCode.POINT_BALANCE_INSUFFICIENT);
        }

        int updated = accountRepository.deductPoints(account.getId(), request.getPoints(), account.getVersion());
        if (updated == 0) throw new BizException(ErrorCode.SYSTEM_ERROR, "并发冲突，请重试");

        PointTransaction tx = new PointTransaction();
        tx.setTenantId(tenantId);
        tx.setTransactionNo(IdGenerator.pointTransactionNo());
        tx.setAccountId(account.getId());
        tx.setSystemId(system.getId());
        tx.setActionId(action.getId());
        tx.setDirection(-1);
        tx.setPoints(request.getPoints());
        tx.setBalanceBefore(account.getBalance());
        tx.setBalanceAfter(account.getBalance() - request.getPoints());
        tx.setBizOrderNo(request.getBizOrderNo());
        tx.setRemark(request.getRemark());
        tx.setIdempotentKey(idempotentKey);
        transactionRepository.save(tx);

        eventPublisher.publishEvent(new PointChangeEvent(this, tenantId, request.getUserId(),
                request.getPoints(), -1, account.getBalance() - request.getPoints()));

        return buildTransactionResult(tx, null);
    }

    @Transactional
    public PointDTO.TransactionResult freeze(ExternalSystem system, PointDTO.FreezeRequest request) {
        Long tenantId = system.getTenantId();
        PointAccount account = accountRepository.findByTenantIdAndUserId(tenantId, request.getUserId())
                .orElseThrow(() -> new BizException(ErrorCode.POINT_ACCOUNT_NOT_FOUND));

        if (account.getBalance() < request.getPoints()) {
            throw new BizException(ErrorCode.POINT_BALANCE_INSUFFICIENT);
        }

        int updated = accountRepository.freezePoints(account.getId(), request.getPoints(), account.getVersion());
        if (updated == 0) throw new BizException(ErrorCode.SYSTEM_ERROR, "并发冲突，请重试");

        PointTransaction tx = new PointTransaction();
        tx.setTenantId(tenantId);
        tx.setTransactionNo(IdGenerator.pointTransactionNo());
        tx.setAccountId(account.getId());
        tx.setSystemId(system.getId());
        tx.setActionId(0L);
        tx.setDirection(0);
        tx.setPoints(request.getPoints());
        tx.setBalanceBefore(account.getBalance());
        tx.setBalanceAfter(account.getBalance() - request.getPoints());
        tx.setBizOrderNo(request.getBizOrderNo());
        tx.setRemark("冻结积分: " + (request.getRemark() != null ? request.getRemark() : ""));
        tx.setIdempotentKey(system.getId() + ":freeze:" + request.getBizOrderNo());
        transactionRepository.save(tx);

        PointDTO.TransactionResult result = new PointDTO.TransactionResult();
        result.setTransactionNo(tx.getTransactionNo());
        result.setPoints(request.getPoints());
        result.setBalance(account.getBalance() - request.getPoints());
        return result;
    }

    public PointDTO.BalanceResponse getBalance(Long tenantId, String userId) {
        PointAccount account = accountRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.POINT_ACCOUNT_NOT_FOUND));
        PointDTO.BalanceResponse resp = new PointDTO.BalanceResponse();
        resp.setUserId(userId);
        resp.setBalance(account.getBalance());
        resp.setFrozen(account.getFrozen());
        resp.setTotalEarned(account.getTotalEarned());
        resp.setTotalConsumed(account.getTotalConsumed());
        return resp;
    }

    public PageResult<PointDTO.TransactionResponse> getTransactions(Long tenantId, String userId,
                                                                      int page, int pageSize) {
        PointAccount account = accountRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.POINT_ACCOUNT_NOT_FOUND));
        Page<PointTransaction> txPage = transactionRepository
                .findByAccountIdOrderByCreatedAtDesc(account.getId(), PageRequest.of(page - 1, pageSize));

        List<PointDTO.TransactionResponse> items = txPage.getContent().stream()
                .map(this::toTransactionResponse).toList();

        return new PageResult<>(txPage.getTotalElements(), page, pageSize, items);
    }

    // ==================== Admin API ====================

    @Transactional
    public PointDTO.ActionResponse createAction(PointDTO.ActionCreateRequest request) {
        PointAction action = new PointAction();
        BeanUtils.copyProperties(request, action);
        action.setTenantId(TenantContext.requireTenantId());
        actionRepository.save(action);
        return toActionResponse(action);
    }

    @Transactional
    public PointDTO.ActionResponse updateAction(Long id, PointDTO.ActionUpdateRequest request) {
        PointAction action = actionRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.POINT_ACTION_NOT_FOUND));
        if (request.getActionName() != null) action.setActionName(request.getActionName());
        if (request.getDescription() != null) action.setDescription(request.getDescription());
        if (request.getDirection() != null) action.setDirection(request.getDirection());
        if (request.getStatus() != null) action.setStatus(request.getStatus());
        actionRepository.save(action);
        return toActionResponse(action);
    }

    public PageResult<PointDTO.ActionResponse> listActions(Long systemId, Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        Page<PointAction> page = systemId != null
                ? actionRepository.findByTenantIdAndSystemId(tenantId, systemId, pageable)
                : actionRepository.findByTenantId(tenantId, pageable);
        return PageResult.from(page.map(this::toActionResponse));
    }

    @Transactional
    public PointDTO.RuleResponse createRule(PointDTO.RuleCreateRequest request) {
        PointRule rule = new PointRule();
        BeanUtils.copyProperties(request, rule);
        rule.setTenantId(TenantContext.requireTenantId());
        if (rule.getEffectiveFrom() == null) rule.setEffectiveFrom(LocalDateTime.now());
        ruleRepository.save(rule);
        return toRuleResponse(rule);
    }

    @Transactional
    public PointDTO.RuleResponse updateRule(Long id, PointDTO.RuleUpdateRequest request) {
        PointRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.POINT_RULE_NOT_FOUND));
        if (request.getRuleName() != null) rule.setRuleName(request.getRuleName());
        if (request.getCalcType() != null) rule.setCalcType(request.getCalcType());
        if (request.getCalcValue() != null) rule.setCalcValue(request.getCalcValue());
        if (request.getCalcExpression() != null) rule.setCalcExpression(request.getCalcExpression());
        if (request.getMinPoints() != null) rule.setMinPoints(request.getMinPoints());
        if (request.getMaxPoints() != null) rule.setMaxPoints(request.getMaxPoints());
        if (request.getDailyLimit() != null) rule.setDailyLimit(request.getDailyLimit());
        if (request.getMonthlyLimit() != null) rule.setMonthlyLimit(request.getMonthlyLimit());
        if (request.getEffectiveFrom() != null) rule.setEffectiveFrom(request.getEffectiveFrom());
        if (request.getEffectiveTo() != null) rule.setEffectiveTo(request.getEffectiveTo());
        if (request.getPriority() != null) rule.setPriority(request.getPriority());
        if (request.getStatus() != null) rule.setStatus(request.getStatus());
        ruleRepository.save(rule);
        return toRuleResponse(rule);
    }

    @Transactional
    public void deleteAction(Long id) {
        actionRepository.deleteById(id);
    }

    @Transactional
    public void deleteRule(Long id) {
        ruleRepository.deleteById(id);
    }

    public PageResult<PointDTO.RuleResponse> listRules(Long actionId, Pageable pageable) {
        Page<PointRule> page = actionId != null
                ? ruleRepository.findByActionId(actionId, pageable)
                : ruleRepository.findByTenantId(TenantContext.requireTenantId(), pageable);
        return PageResult.from(page.map(this::toRuleResponse));
    }

    public PageResult<PointDTO.BalanceResponse> listAccounts(Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        Page<PointAccount> page = accountRepository.findByTenantId(tenantId, pageable);
        return PageResult.from(page.map(a -> {
            PointDTO.BalanceResponse resp = new PointDTO.BalanceResponse();
            resp.setUserId(a.getUserId());
            resp.setBalance(a.getBalance());
            resp.setFrozen(a.getFrozen());
            resp.setTotalEarned(a.getTotalEarned());
            resp.setTotalConsumed(a.getTotalConsumed());
            return resp;
        }));
    }

    // ==================== Helpers ====================

    private PointAccount getOrCreateAccount(Long tenantId, String userId) {
        return accountRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseGet(() -> {
                    PointAccount newAccount = new PointAccount();
                    newAccount.setTenantId(tenantId);
                    newAccount.setUserId(userId);
                    return accountRepository.save(newAccount);
                });
    }

    private void checkDailyMonthlyLimit(PointRule rule, Long tenantId, String userId, Long actionId) {
        if (rule.getDailyLimit() != null) {
            String dailyKey = "point:daily:" + tenantId + ":" + userId + ":" + actionId + ":" + LocalDate.now();
            String count = redisTemplate.opsForValue().get(dailyKey);
            if (count != null && Long.parseLong(count) >= rule.getDailyLimit()) {
                throw new BizException(ErrorCode.POINT_DAILY_LIMIT_EXCEEDED);
            }
        }
        if (rule.getMonthlyLimit() != null) {
            String monthlyKey = "point:monthly:" + tenantId + ":" + userId + ":" + actionId + ":" + YearMonth.now();
            String count = redisTemplate.opsForValue().get(monthlyKey);
            if (count != null && Long.parseLong(count) >= rule.getMonthlyLimit()) {
                throw new BizException(ErrorCode.POINT_MONTHLY_LIMIT_EXCEEDED);
            }
        }
    }

    private void incrementLimitCounter(Long tenantId, String userId, Long actionId) {
        String dailyKey = "point:daily:" + tenantId + ":" + userId + ":" + actionId + ":" + LocalDate.now();
        redisTemplate.opsForValue().increment(dailyKey);
        redisTemplate.expire(dailyKey, Duration.ofDays(2));

        String monthlyKey = "point:monthly:" + tenantId + ":" + userId + ":" + actionId + ":" + YearMonth.now();
        redisTemplate.opsForValue().increment(monthlyKey);
        redisTemplate.expire(monthlyKey, Duration.ofDays(35));
    }

    private PointDTO.TransactionResult buildTransactionResult(PointTransaction tx, String ruleName) {
        PointDTO.TransactionResult result = new PointDTO.TransactionResult();
        result.setTransactionNo(tx.getTransactionNo());
        result.setPoints(tx.getPoints());
        result.setBalance(tx.getBalanceAfter());
        result.setRuleName(ruleName);
        return result;
    }

    private PointDTO.ActionResponse toActionResponse(PointAction action) {
        PointDTO.ActionResponse resp = new PointDTO.ActionResponse();
        BeanUtils.copyProperties(action, resp);
        return resp;
    }

    private PointDTO.RuleResponse toRuleResponse(PointRule rule) {
        PointDTO.RuleResponse resp = new PointDTO.RuleResponse();
        BeanUtils.copyProperties(rule, resp);
        return resp;
    }

    private PointDTO.TransactionResponse toTransactionResponse(PointTransaction tx) {
        PointDTO.TransactionResponse resp = new PointDTO.TransactionResponse();
        resp.setTransactionNo(tx.getTransactionNo());
        resp.setDirection(tx.getDirection());
        resp.setDirectionText(tx.getDirection() == 1 ? "收入" : "支出");
        resp.setPoints(tx.getPoints());
        resp.setBalanceAfter(tx.getBalanceAfter());
        resp.setBizOrderNo(tx.getBizOrderNo());
        resp.setRemark(tx.getRemark());
        resp.setCreatedAt(tx.getCreatedAt());
        return resp;
    }

    // Event for member level and webhook integration
    public record PointChangeEvent(Object source, Long tenantId, String userId,
                                    long points, int direction, long newBalance) {}
}
