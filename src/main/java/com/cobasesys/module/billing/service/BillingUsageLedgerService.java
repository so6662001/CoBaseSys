package com.cobasesys.module.billing.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.util.SignatureUtil;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.entity.BillingSubscription;
import com.cobasesys.module.billing.entity.BillingUsageLedger;
import com.cobasesys.module.billing.repository.BillingSubscriptionRepository;
import com.cobasesys.module.billing.repository.BillingUsageLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingUsageLedgerService {

    private final BillingUsageLedgerRepository ledgerRepository;
    private final BillingSubscriptionRepository subscriptionRepository;

    @Value("${cobasesys.security.chain-secret:default-chain-secret-key}")
    private String chainSecret;

    @Transactional("billingTransactionManager")
    public BillingDTO.UsageLedgerResp reportUsage(BillingDTO.UsageReportReq req) {
        String idempotentKey = req.getSubscriptionNo() + ":" + req.getBizOrderNo();
        Optional<BillingUsageLedger> existing = ledgerRepository.findByIdempotentKey(idempotentKey);
        if (existing.isPresent()) {
            return toResp(existing.get());
        }

        BillingSubscription sub = subscriptionRepository.findBySubscriptionNo(req.getSubscriptionNo())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订阅不存在"));

        if (!"ACTIVE".equals(sub.getStatus()) && !"TRIAL".equals(sub.getStatus())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "订阅状态不允许上报用量，当前状态: " + sub.getStatus());
        }

        long remaining = sub.getUsageQuota() - sub.getUsageUsed();
        if (sub.getUsageQuota() > 0 && remaining <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "用量配额已耗尽");
        }

        long balanceBefore = remaining;
        long quantity = req.getQuantity();

        sub.setUsageUsed(sub.getUsageUsed() + Math.abs(quantity));
        subscriptionRepository.save(sub);

        long balanceAfter = sub.getUsageQuota() - sub.getUsageUsed();

        Optional<BillingUsageLedger> prevLedger = ledgerRepository
                .findTopBySubscriptionIdOrderByIdDesc(sub.getId());
        String prevHash = prevLedger.map(BillingUsageLedger::getChainHash).orElse("0000000000000000");

        BillingUsageLedger ledger = new BillingUsageLedger();
        ledger.setTenantId(sub.getTenantId());
        ledger.setSubscriptionId(sub.getId());
        ledger.setCustomerId(sub.getCustomerId());
        ledger.setAction(quantity < 0 ? "CONSUME" : "CONSUME");
        ledger.setQuantity(-Math.abs(quantity));
        ledger.setUnit(req.getUnit());
        ledger.setBalanceBefore(balanceBefore);
        ledger.setBalanceAfter(balanceAfter);
        ledger.setBizSystem(req.getBizSystem());
        ledger.setBizOrderNo(req.getBizOrderNo());
        ledger.setBizDescription(req.getBizDescription());
        ledger.setIdempotentKey(idempotentKey);

        String dataHash = SignatureUtil.md5(ledger.getSubscriptionId() + "|" + ledger.getQuantity()
                + "|" + ledger.getBalanceBefore() + "|" + ledger.getBalanceAfter());
        String chainHash = SignatureUtil.hmacSha256(chainSecret, dataHash + "|" + prevHash);
        ledger.setDataHash(dataHash);
        ledger.setPrevHash(prevHash);
        ledger.setChainHash(chainHash);

        ledgerRepository.save(ledger);
        return toResp(ledger);
    }

    public PageResult<BillingDTO.UsageLedgerResp> listBySubscription(Long subscriptionId, Pageable pageable) {
        Page<BillingUsageLedger> page = ledgerRepository
                .findBySubscriptionIdOrderByCreatedAtDesc(subscriptionId, pageable);
        return PageResult.from(page.map(this::toResp));
    }

    public PageResult<BillingDTO.UsageLedgerResp> listByCustomer(Long tenantId, String customerId, Pageable pageable) {
        Page<BillingUsageLedger> page = (customerId != null && !customerId.isBlank())
                ? ledgerRepository.findByTenantIdAndCustomerIdOrderByCreatedAtDesc(tenantId, customerId, pageable)
                : ledgerRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        return PageResult.from(page.map(this::toResp));
    }

    private BillingDTO.UsageLedgerResp toResp(BillingUsageLedger l) {
        BillingDTO.UsageLedgerResp resp = new BillingDTO.UsageLedgerResp();
        BeanUtils.copyProperties(l, resp);
        return resp;
    }
}
