package com.cobasesys.module.wallet.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.common.util.IdGenerator;
import com.cobasesys.module.system.entity.ExternalSystem;
import com.cobasesys.module.wallet.dto.WalletDTO;
import com.cobasesys.module.wallet.entity.*;
import com.cobasesys.module.wallet.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletAccountRepository accountRepository;
    private final ConsumeActionRepository actionRepository;
    private final ConsumeRuleRepository ruleRepository;
    private final RechargeOrderRepository orderRepository;
    private final WalletTransactionRepository transactionRepository;
    private final RechargePromotionRepository promotionRepository;
    private final ConsumeRuleEngine ruleEngine;
    private final ApplicationEventPublisher eventPublisher;

    // ==================== Open API ====================

    @Transactional
    public WalletDTO.RechargeOrderResponse createRechargeOrder(Long tenantId, WalletDTO.RechargeRequest request) {
        WalletAccount account = getOrCreateAccount(tenantId, request.getUserId());

        long giftAmount = calculateGift(tenantId, request.getAmount());

        RechargeOrder order = new RechargeOrder();
        order.setTenantId(tenantId);
        order.setOrderNo(IdGenerator.rechargeOrderNo());
        order.setAccountId(account.getId());
        order.setUserId(request.getUserId());
        order.setAmount(request.getAmount());
        order.setActualAmount(request.getAmount() + giftAmount);
        order.setGiftAmount(giftAmount);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setRemark(request.getRemark());
        order.setStatus(0);
        orderRepository.save(order);

        return toRechargeResponse(order);
    }

    @Transactional
    public WalletDTO.RechargeOrderResponse processRechargeCallback(WalletDTO.RechargeCallbackRequest request) {
        RechargeOrder order = orderRepository.findByOrderNo(request.getOrderNo())
                .orElseThrow(() -> new BizException(ErrorCode.RECHARGE_ORDER_NOT_FOUND));

        if (order.getStatus() != 0) {
            throw new BizException(ErrorCode.RECHARGE_ORDER_PAID);
        }

        if (!request.isSuccess()) {
            order.setStatus(2);
            orderRepository.save(order);
            return toRechargeResponse(order);
        }

        order.setStatus(1);
        order.setPaidAt(LocalDateTime.now());
        order.setPaymentNo(request.getPaymentNo());
        orderRepository.save(order);

        WalletAccount account = accountRepository.findById(order.getAccountId()).orElseThrow();
        int updated = accountRepository.recharge(account.getId(), order.getActualAmount(), account.getVersion());
        if (updated == 0) {
            account = accountRepository.findById(account.getId()).orElseThrow();
            accountRepository.recharge(account.getId(), order.getActualAmount(), account.getVersion());
        }

        WalletTransaction tx = new WalletTransaction();
        tx.setTenantId(order.getTenantId());
        tx.setTransactionNo(IdGenerator.walletTransactionNo());
        tx.setAccountId(account.getId());
        tx.setType(1);
        tx.setAmount(order.getActualAmount());
        tx.setBalanceBefore(account.getBalance());
        tx.setBalanceAfter(account.getBalance() + order.getActualAmount());
        tx.setBizOrderNo(order.getOrderNo());
        tx.setRemark("充值" + WalletDTO.formatAmount(order.getAmount())
                + (order.getGiftAmount() > 0 ? ", 赠送" + WalletDTO.formatAmount(order.getGiftAmount()) : ""));
        tx.setIdempotentKey("recharge:" + order.getOrderNo());
        transactionRepository.save(tx);

        eventPublisher.publishEvent(new WalletChangeEvent(this, order.getTenantId(),
                order.getUserId(), order.getActualAmount(), 1,
                account.getBalance() + order.getActualAmount()));

        return toRechargeResponse(order);
    }

    @Transactional
    public WalletDTO.TransactionResult consume(ExternalSystem system, WalletDTO.ConsumeRequest request) {
        Long tenantId = system.getTenantId();
        String idempotentKey = system.getId() + ":" + request.getBizOrderNo() + ":" + request.getActionCode();

        Optional<WalletTransaction> existing = transactionRepository.findByIdempotentKey(idempotentKey);
        if (existing.isPresent()) return buildTransactionResult(existing.get(), null);

        ConsumeAction action = actionRepository.findBySystemIdAndActionCode(system.getId(), request.getActionCode())
                .orElseThrow(() -> new BizException(ErrorCode.CONSUME_ACTION_NOT_FOUND));
        if (action.getStatus() != 1) throw new BizException(ErrorCode.CONSUME_ACTION_DISABLED);

        List<ConsumeRule> rules = ruleRepository.findActiveRules(action.getId(), LocalDateTime.now());
        if (rules.isEmpty()) throw new BizException(ErrorCode.CONSUME_RULE_NOT_FOUND);
        ConsumeRule rule = rules.get(0);

        long amountInCents = ruleEngine.calculate(rule, request.getBizQuantity());
        if (amountInCents <= 0) {
            WalletDTO.TransactionResult result = new WalletDTO.TransactionResult();
            result.setAmount(0L);
            result.setAmountDisplay(WalletDTO.formatAmount(0));
            result.setRuleName(rule.getRuleName());
            return result;
        }

        WalletAccount account = accountRepository.findByTenantIdAndUserId(tenantId, request.getUserId())
                .orElseThrow(() -> new BizException(ErrorCode.WALLET_ACCOUNT_NOT_FOUND));

        if (account.getBalance() < amountInCents) {
            throw new BizException(ErrorCode.WALLET_BALANCE_INSUFFICIENT);
        }

        int updated = accountRepository.consume(account.getId(), amountInCents, account.getVersion());
        if (updated == 0) throw new BizException(ErrorCode.SYSTEM_ERROR, "并发冲突，请重试");

        WalletTransaction tx = new WalletTransaction();
        tx.setTenantId(tenantId);
        tx.setTransactionNo(IdGenerator.walletTransactionNo());
        tx.setAccountId(account.getId());
        tx.setType(2);
        tx.setSystemId(system.getId());
        tx.setActionId(action.getId());
        tx.setRuleId(rule.getId());
        tx.setAmount(amountInCents);
        tx.setBalanceBefore(account.getBalance());
        tx.setBalanceAfter(account.getBalance() - amountInCents);
        tx.setBizOrderNo(request.getBizOrderNo());
        tx.setBizQuantity(request.getBizQuantity());
        tx.setRemark(request.getRemark());
        tx.setIdempotentKey(idempotentKey);
        transactionRepository.save(tx);

        eventPublisher.publishEvent(new WalletChangeEvent(this, tenantId,
                request.getUserId(), amountInCents, -1,
                account.getBalance() - amountInCents));

        return buildTransactionResult(tx, rule.getRuleName());
    }

    @Transactional
    public WalletDTO.TransactionResult freezeBalance(ExternalSystem system, WalletDTO.FreezeRequest request) {
        Long tenantId = system.getTenantId();
        WalletAccount account = accountRepository.findByTenantIdAndUserId(tenantId, request.getUserId())
                .orElseThrow(() -> new BizException(ErrorCode.WALLET_ACCOUNT_NOT_FOUND));

        if (account.getBalance() < request.getAmount()) {
            throw new BizException(ErrorCode.WALLET_BALANCE_INSUFFICIENT);
        }

        int updated = accountRepository.freeze(account.getId(), request.getAmount(), account.getVersion());
        if (updated == 0) throw new BizException(ErrorCode.SYSTEM_ERROR, "并发冲突，请重试");

        WalletTransaction tx = new WalletTransaction();
        tx.setTenantId(tenantId);
        tx.setTransactionNo(IdGenerator.walletTransactionNo());
        tx.setAccountId(account.getId());
        tx.setType(4);
        tx.setSystemId(system.getId());
        tx.setAmount(request.getAmount());
        tx.setBalanceBefore(account.getBalance());
        tx.setBalanceAfter(account.getBalance() - request.getAmount());
        tx.setBizOrderNo(request.getBizOrderNo());
        tx.setRemark("冻结: " + (request.getRemark() != null ? request.getRemark() : ""));
        tx.setIdempotentKey(system.getId() + ":freeze:" + request.getBizOrderNo());
        transactionRepository.save(tx);

        return buildTransactionResult(tx, null);
    }

    @Transactional
    public WalletDTO.TransactionResult refund(ExternalSystem system, WalletDTO.RefundRequest request) {
        Long tenantId = system.getTenantId();
        WalletAccount account = accountRepository.findByTenantIdAndUserId(tenantId, request.getUserId())
                .orElseThrow(() -> new BizException(ErrorCode.WALLET_ACCOUNT_NOT_FOUND));

        int updated = accountRepository.recharge(account.getId(), request.getAmount(), account.getVersion());
        if (updated == 0) throw new BizException(ErrorCode.SYSTEM_ERROR, "并发冲突，请重试");

        WalletTransaction tx = new WalletTransaction();
        tx.setTenantId(tenantId);
        tx.setTransactionNo(IdGenerator.walletTransactionNo());
        tx.setAccountId(account.getId());
        tx.setType(3);
        tx.setSystemId(system.getId());
        tx.setAmount(request.getAmount());
        tx.setBalanceBefore(account.getBalance());
        tx.setBalanceAfter(account.getBalance() + request.getAmount());
        tx.setBizOrderNo(request.getBizOrderNo());
        tx.setRemark("退款: " + (request.getRemark() != null ? request.getRemark() : ""));
        tx.setIdempotentKey(system.getId() + ":refund:" + request.getBizOrderNo());
        transactionRepository.save(tx);

        return buildTransactionResult(tx, null);
    }

    public WalletDTO.BalanceResponse getBalance(Long tenantId, String userId) {
        WalletAccount account = accountRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.WALLET_ACCOUNT_NOT_FOUND));
        WalletDTO.BalanceResponse resp = new WalletDTO.BalanceResponse();
        resp.setUserId(userId);
        resp.setBalance(account.getBalance());
        resp.setBalanceDisplay(WalletDTO.formatAmount(account.getBalance()));
        resp.setFrozen(account.getFrozen());
        resp.setTotalRecharged(account.getTotalRecharged());
        resp.setTotalConsumed(account.getTotalConsumed());
        return resp;
    }

    public PageResult<WalletDTO.TransactionResponse> getTransactions(Long tenantId, String userId,
                                                                       int page, int pageSize) {
        WalletAccount account = accountRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.WALLET_ACCOUNT_NOT_FOUND));
        Page<WalletTransaction> txPage = transactionRepository
                .findByAccountIdOrderByCreatedAtDesc(account.getId(), PageRequest.of(page - 1, pageSize));
        List<WalletDTO.TransactionResponse> items = txPage.getContent().stream()
                .map(this::toTransactionResponse).toList();
        return new PageResult<>(txPage.getTotalElements(), page, pageSize, items);
    }

    public WalletDTO.BalanceCheckResponse checkBalance(ExternalSystem system, WalletDTO.BalanceCheckRequest request) {
        Long tenantId = system.getTenantId();
        ConsumeAction action = actionRepository.findBySystemIdAndActionCode(system.getId(), request.getActionCode())
                .orElseThrow(() -> new BizException(ErrorCode.CONSUME_ACTION_NOT_FOUND));
        List<ConsumeRule> rules = ruleRepository.findActiveRules(action.getId(), LocalDateTime.now());
        if (rules.isEmpty()) throw new BizException(ErrorCode.CONSUME_RULE_NOT_FOUND);

        long estimatedAmount = ruleEngine.calculate(rules.get(0), request.getBizQuantity());

        WalletAccount account = accountRepository.findByTenantIdAndUserId(tenantId, request.getUserId())
                .orElse(null);

        WalletDTO.BalanceCheckResponse resp = new WalletDTO.BalanceCheckResponse();
        resp.setEstimatedAmount(estimatedAmount);
        resp.setEstimatedAmountDisplay(WalletDTO.formatAmount(estimatedAmount));
        resp.setCurrentBalance(account != null ? account.getBalance() : 0L);
        resp.setSufficient(account != null && account.getBalance() >= estimatedAmount);
        return resp;
    }

    // ==================== Admin API ====================

    @Transactional
    public WalletDTO.ActionResponse createAction(WalletDTO.ActionCreateRequest request) {
        ConsumeAction action = new ConsumeAction();
        BeanUtils.copyProperties(request, action);
        action.setTenantId(TenantContext.requireTenantId());
        actionRepository.save(action);
        return toActionResponse(action);
    }

    @Transactional
    public WalletDTO.ActionResponse updateAction(Long id, WalletDTO.ActionUpdateRequest request) {
        ConsumeAction action = actionRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.CONSUME_ACTION_NOT_FOUND));
        if (request.getActionName() != null) action.setActionName(request.getActionName());
        if (request.getDescription() != null) action.setDescription(request.getDescription());
        if (request.getStatus() != null) action.setStatus(request.getStatus());
        actionRepository.save(action);
        return toActionResponse(action);
    }

    public PageResult<WalletDTO.ActionResponse> listActions(Long systemId, Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        Page<ConsumeAction> page = systemId != null
                ? actionRepository.findByTenantIdAndSystemId(tenantId, systemId, pageable)
                : actionRepository.findByTenantId(tenantId, pageable);
        return PageResult.from(page.map(this::toActionResponse));
    }

    @Transactional
    public WalletDTO.RuleResponse createRule(WalletDTO.RuleCreateRequest request) {
        ConsumeRule rule = new ConsumeRule();
        BeanUtils.copyProperties(request, rule);
        rule.setTenantId(TenantContext.requireTenantId());
        if (rule.getEffectiveFrom() == null) rule.setEffectiveFrom(LocalDateTime.now());
        if (rule.getFreeQuota() == null) rule.setFreeQuota(0);
        ruleRepository.save(rule);
        return toRuleResponse(rule);
    }

    @Transactional
    public WalletDTO.RuleResponse updateRule(Long id, WalletDTO.RuleUpdateRequest request) {
        ConsumeRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.CONSUME_RULE_NOT_FOUND));
        if (request.getRuleName() != null) rule.setRuleName(request.getRuleName());
        if (request.getCalcType() != null) rule.setCalcType(request.getCalcType());
        if (request.getCalcValue() != null) rule.setCalcValue(request.getCalcValue());
        if (request.getCalcExpression() != null) rule.setCalcExpression(request.getCalcExpression());
        if (request.getUnitName() != null) rule.setUnitName(request.getUnitName());
        if (request.getMinCharge() != null) rule.setMinCharge(request.getMinCharge());
        if (request.getMaxCharge() != null) rule.setMaxCharge(request.getMaxCharge());
        if (request.getFreeQuota() != null) rule.setFreeQuota(request.getFreeQuota());
        if (request.getEffectiveFrom() != null) rule.setEffectiveFrom(request.getEffectiveFrom());
        if (request.getEffectiveTo() != null) rule.setEffectiveTo(request.getEffectiveTo());
        if (request.getPriority() != null) rule.setPriority(request.getPriority());
        if (request.getStatus() != null) rule.setStatus(request.getStatus());
        ruleRepository.save(rule);
        return toRuleResponse(rule);
    }

    public PageResult<WalletDTO.RuleResponse> listRules(Long actionId, Pageable pageable) {
        Page<ConsumeRule> page = actionId != null
                ? ruleRepository.findByActionId(actionId, pageable)
                : ruleRepository.findByTenantId(TenantContext.requireTenantId(), pageable);
        return PageResult.from(page.map(this::toRuleResponse));
    }

    @Transactional
    public WalletDTO.PromotionResponse createPromotion(WalletDTO.PromotionCreateRequest request) {
        RechargePromotion promo = new RechargePromotion();
        BeanUtils.copyProperties(request, promo);
        promo.setTenantId(TenantContext.requireTenantId());
        if (promo.getEffectiveFrom() == null) promo.setEffectiveFrom(LocalDateTime.now());
        promotionRepository.save(promo);
        return toPromotionResponse(promo);
    }

    public PageResult<WalletDTO.PromotionResponse> listPromotions(Pageable pageable) {
        Page<RechargePromotion> page = promotionRepository.findByTenantId(
                TenantContext.requireTenantId(), pageable);
        return PageResult.from(page.map(this::toPromotionResponse));
    }

    @Transactional
    public WalletDTO.TransactionResult adminAdjust(Long tenantId, String userId, long amount, String remark) {
        WalletAccount account = getOrCreateAccount(tenantId, userId);
        int updated = accountRepository.recharge(account.getId(), amount, account.getVersion());
        if (updated == 0) throw new BizException(ErrorCode.SYSTEM_ERROR, "并发冲突");

        WalletTransaction tx = new WalletTransaction();
        tx.setTenantId(tenantId);
        tx.setTransactionNo(IdGenerator.walletTransactionNo());
        tx.setAccountId(account.getId());
        tx.setType(6);
        tx.setAmount(amount);
        tx.setBalanceBefore(account.getBalance());
        tx.setBalanceAfter(account.getBalance() + amount);
        tx.setRemark("后台调账: " + remark);
        tx.setIdempotentKey("adjust:" + tx.getTransactionNo());
        transactionRepository.save(tx);

        return buildTransactionResult(tx, null);
    }

    // ==================== Helpers ====================

    private WalletAccount getOrCreateAccount(Long tenantId, String userId) {
        return accountRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseGet(() -> {
                    WalletAccount newAccount = new WalletAccount();
                    newAccount.setTenantId(tenantId);
                    newAccount.setUserId(userId);
                    return accountRepository.save(newAccount);
                });
    }

    private long calculateGift(Long tenantId, long amountInCents) {
        List<RechargePromotion> promotions = promotionRepository
                .findActivePromotions(tenantId, amountInCents, LocalDateTime.now());
        if (promotions.isEmpty()) return 0;

        RechargePromotion promo = promotions.get(0);
        return switch (promo.getGiftType()) {
            case "fixed" -> promo.getGiftValue().multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.FLOOR).longValue();
            case "rate" -> BigDecimal.valueOf(amountInCents)
                    .multiply(promo.getGiftValue())
                    .setScale(0, RoundingMode.FLOOR).longValue();
            default -> 0;
        };
    }

    private WalletDTO.TransactionResult buildTransactionResult(WalletTransaction tx, String ruleName) {
        WalletDTO.TransactionResult result = new WalletDTO.TransactionResult();
        result.setTransactionNo(tx.getTransactionNo());
        result.setAmount(tx.getAmount());
        result.setAmountDisplay(WalletDTO.formatAmount(tx.getAmount()));
        result.setBalance(tx.getBalanceAfter());
        result.setBalanceDisplay(WalletDTO.formatAmount(tx.getBalanceAfter()));
        result.setRuleName(ruleName);
        return result;
    }

    private WalletDTO.ActionResponse toActionResponse(ConsumeAction action) {
        WalletDTO.ActionResponse resp = new WalletDTO.ActionResponse();
        BeanUtils.copyProperties(action, resp);
        return resp;
    }

    private WalletDTO.RuleResponse toRuleResponse(ConsumeRule rule) {
        WalletDTO.RuleResponse resp = new WalletDTO.RuleResponse();
        BeanUtils.copyProperties(rule, resp);
        return resp;
    }

    private WalletDTO.PromotionResponse toPromotionResponse(RechargePromotion promo) {
        WalletDTO.PromotionResponse resp = new WalletDTO.PromotionResponse();
        BeanUtils.copyProperties(promo, resp);
        return resp;
    }

    private WalletDTO.RechargeOrderResponse toRechargeResponse(RechargeOrder order) {
        WalletDTO.RechargeOrderResponse resp = new WalletDTO.RechargeOrderResponse();
        BeanUtils.copyProperties(order, resp);
        String statusText = switch (order.getStatus()) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已取消";
            case 3 -> "已退款";
            default -> "未知";
        };
        resp.setStatusText(statusText);
        return resp;
    }

    private WalletDTO.TransactionResponse toTransactionResponse(WalletTransaction tx) {
        WalletDTO.TransactionResponse resp = new WalletDTO.TransactionResponse();
        resp.setTransactionNo(tx.getTransactionNo());
        resp.setType(tx.getType());
        resp.setTypeText(WalletDTO.typeText(tx.getType()));
        resp.setAmount(tx.getAmount());
        resp.setAmountDisplay(WalletDTO.formatAmount(tx.getAmount()));
        resp.setBalanceAfter(tx.getBalanceAfter());
        resp.setBizOrderNo(tx.getBizOrderNo());
        resp.setRemark(tx.getRemark());
        resp.setCreatedAt(tx.getCreatedAt());
        return resp;
    }

    public record WalletChangeEvent(Object source, Long tenantId, String userId,
                                     long amount, int direction, long newBalance) {}
}
