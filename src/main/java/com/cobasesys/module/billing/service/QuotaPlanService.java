package com.cobasesys.module.billing.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.billing.entity.BillingSubscription;
import com.cobasesys.module.billing.entity.BillingSubscriptionQuota;
import com.cobasesys.module.billing.repository.BillingSubscriptionQuotaRepository;
import com.cobasesys.module.billing.repository.BillingSubscriptionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaPlanService {

    private final BillingSubscriptionRepository subscriptionRepository;
    private final BillingSubscriptionQuotaRepository quotaRepository;
    private final ObjectMapper objectMapper;

    @Transactional("billingTransactionManager")
    public void initQuotasForSubscription(BillingSubscription sub) {
        if (sub.getQuotaConfig() == null || sub.getQuotaConfig().isBlank()) return;
        if (!"QUOTA_PLAN".equals(sub.getPricingModel())) return;

        try {
            JsonNode config = objectMapper.readTree(sub.getQuotaConfig());
            JsonNode quotas = config.get("quotas");
            if (quotas == null || !quotas.isArray()) return;

            LocalDate periodStart = sub.getStartDate();
            LocalDate periodEnd = periodStart.plusMonths(1);

            for (JsonNode q : quotas) {
                BillingSubscriptionQuota quota = new BillingSubscriptionQuota();
                quota.setTenantId(sub.getTenantId());
                quota.setSubscriptionId(sub.getId());
                quota.setCustomerId(sub.getCustomerId());
                quota.setDimension(q.get("dimension").asText());
                quota.setDimensionLabel(q.has("label") ? q.get("label").asText() : quota.getDimension());
                quota.setQuotaLimit(q.get("limit").asLong());
                quota.setQuotaUsed(0L);
                quota.setPeriodStart(periodStart);
                quota.setPeriodEnd(periodEnd);
                quotaRepository.save(quota);
            }
        } catch (Exception e) {
            log.error("Failed to init quotas for subscription {}: {}", sub.getId(), e.getMessage());
        }
    }

    public QuotaCheckResult checkQuota(String customerId, Long subscriptionId, String dimension) {
        Long tenantId = TenantContext.requireTenantId();
        BillingSubscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        if (!tenantId.equals(sub.getTenantId())) throw new BizException(ErrorCode.FORBIDDEN);

        LocalDate now = LocalDate.now();
        var quota = quotaRepository.findBySubscriptionIdAndDimensionAndPeriodStartAndPeriodEnd(
                subscriptionId, dimension, sub.getStartDate(),
                sub.getStartDate().plusMonths(1));

        if (quota.isEmpty()) {
            // Try current month period
            LocalDate monthStart = now.withDayOfMonth(1);
            quota = quotaRepository.findBySubscriptionIdAndDimensionAndPeriodStartAndPeriodEnd(
                    subscriptionId, dimension, monthStart, monthStart.plusMonths(1));
        }

        QuotaCheckResult result = new QuotaCheckResult();
        result.setDimension(dimension);
        result.setCustomerId(customerId);

        if (quota.isEmpty()) {
            result.setAllowed(true);
            result.setLimit(-1);
            result.setUsed(0);
            result.setRemaining(-1);
            return result;
        }

        BillingSubscriptionQuota q = quota.get();
        result.setLimit(q.getQuotaLimit());
        result.setUsed(q.getQuotaUsed());
        result.setDimensionLabel(q.getDimensionLabel());

        if (q.getQuotaLimit() == -1) {
            result.setAllowed(true);
            result.setRemaining(-1);
        } else {
            long remaining = q.getQuotaLimit() - q.getQuotaUsed();
            result.setRemaining(Math.max(remaining, 0));
            result.setAllowed(remaining > 0);
        }
        return result;
    }

    @Transactional("billingTransactionManager")
    public QuotaCheckResult consumeQuota(Long subscriptionId, String dimension, long delta) {
        Long tenantId = TenantContext.requireTenantId();
        BillingSubscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        if (!tenantId.equals(sub.getTenantId())) throw new BizException(ErrorCode.FORBIDDEN);

        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        var quotaOpt = quotaRepository.findBySubscriptionIdAndDimensionAndPeriodStartAndPeriodEnd(
                subscriptionId, dimension, monthStart, monthStart.plusMonths(1));

        if (quotaOpt.isEmpty()) {
            quotaOpt = quotaRepository.findBySubscriptionIdAndDimensionAndPeriodStartAndPeriodEnd(
                    subscriptionId, dimension, sub.getStartDate(), sub.getStartDate().plusMonths(1));
        }

        if (quotaOpt.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "未找到配额维度: " + dimension);
        }

        BillingSubscriptionQuota q = quotaOpt.get();

        if (q.getQuotaLimit() != -1 && q.getQuotaUsed() + delta > q.getQuotaLimit()) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    q.getDimensionLabel() + "已达上限(" + q.getQuotaLimit() + ")，请升级套餐");
        }

        int updated = quotaRepository.incrementUsage(q.getId(), delta, q.getVersion());
        if (updated == 0) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "配额更新冲突，请重试");
        }

        QuotaCheckResult result = new QuotaCheckResult();
        result.setDimension(dimension);
        result.setDimensionLabel(q.getDimensionLabel());
        result.setLimit(q.getQuotaLimit());
        result.setUsed(q.getQuotaUsed() + delta);
        result.setRemaining(q.getQuotaLimit() == -1 ? -1 : q.getQuotaLimit() - q.getQuotaUsed() - delta);
        result.setAllowed(true);
        result.setCustomerId(q.getCustomerId());
        return result;
    }

    public List<QuotaCheckResult> getAllQuotas(Long subscriptionId) {
        List<BillingSubscriptionQuota> quotas = quotaRepository.findBySubscriptionId(subscriptionId);
        return quotas.stream().map(q -> {
            QuotaCheckResult r = new QuotaCheckResult();
            r.setDimension(q.getDimension());
            r.setDimensionLabel(q.getDimensionLabel());
            r.setLimit(q.getQuotaLimit());
            r.setUsed(q.getQuotaUsed());
            r.setRemaining(q.getQuotaLimit() == -1 ? -1 : Math.max(q.getQuotaLimit() - q.getQuotaUsed(), 0));
            r.setAllowed(q.getQuotaLimit() == -1 || q.getQuotaUsed() < q.getQuotaLimit());
            r.setCustomerId(q.getCustomerId());
            return r;
        }).toList();
    }

    @Data
    public static class QuotaCheckResult {
        private String dimension;
        private String dimensionLabel;
        private String customerId;
        private long limit;
        private long used;
        private long remaining;
        private boolean allowed;
    }
}
