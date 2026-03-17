package com.cobasesys.module.billing.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.entity.BillingSubscription;
import com.cobasesys.module.billing.repository.BillingProductRepository;
import com.cobasesys.module.billing.repository.BillingSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingSubscriptionService {

    private final BillingSubscriptionRepository subscriptionRepository;
    private final BillingProductRepository productRepository;

    public PageResult<BillingDTO.SubscriptionResp> list(String customerId, String status, Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        Page<BillingSubscription> page;
        if (customerId != null) {
            page = subscriptionRepository.findByTenantIdAndCustomerIdOrderByCreatedAtDesc(tenantId, customerId, pageable);
        } else if (status != null) {
            page = subscriptionRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else {
            page = subscriptionRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        }
        return PageResult.from(page.map(this::toResp));
    }

    public BillingDTO.SubscriptionResp getById(Long id) {
        BillingSubscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        verifyTenant(sub);
        return toResp(sub);
    }

    public List<BillingDTO.SubscriptionResp> getExpiringAlerts(Long tenantId, String customerId) {
        LocalDate deadline = LocalDate.now().plusDays(15);
        List<BillingSubscription> subs = subscriptionRepository
                .findCustomerExpiringAlerts(tenantId, customerId, deadline);
        return subs.stream().map(this::toResp).toList();
    }

    public List<BillingDTO.SubscriptionResp> getExpiringSoon(Long tenantId) {
        return subscriptionRepository.findExpiringSoon(tenantId, LocalDate.now().plusDays(15))
                .stream().map(this::toResp).toList();
    }

    public List<BillingDTO.SubscriptionResp> getExpired(Long tenantId) {
        return subscriptionRepository.findExpired(tenantId, LocalDate.now())
                .stream().map(this::toResp).toList();
    }

    @Transactional("billingTransactionManager")
    public BillingDTO.SubscriptionResp extend(Long id, int days) {
        if (days <= 0) throw new BizException(ErrorCode.PARAM_INVALID, "延期天数必须大于0");
        BillingSubscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        verifyTenant(sub);
        sub.setEndDate(sub.getEndDate().plusDays(days));
        sub.setTotalDays(sub.getTotalDays() + days);
        if (sub.getPriceLockedUntil() != null) {
            sub.setPriceLockedUntil(sub.getEndDate());
        }
        subscriptionRepository.save(sub);
        return toResp(sub);
    }

    @Transactional("billingTransactionManager")
    public void suspend(Long id) {
        BillingSubscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        verifyTenant(sub);
        sub.setStatus("SUSPENDED");
        subscriptionRepository.save(sub);
    }

    @Transactional("billingTransactionManager")
    public void resume(Long id) {
        BillingSubscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        verifyTenant(sub);
        sub.setStatus("ACTIVE");
        subscriptionRepository.save(sub);
    }

    public BillingDTO.SubscriptionResp getBySubscriptionNo(String subscriptionNo) {
        BillingSubscription sub = subscriptionRepository.findBySubscriptionNo(subscriptionNo)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        verifyTenant(sub);
        return toResp(sub);
    }

    @Transactional("billingTransactionManager")
    public BillingDTO.SubscriptionResp renew(Long id, String periodType, int periodCount) {
        BillingSubscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        verifyTenant(sub);
        if ("SUSPENDED".equals(sub.getStatus())) throw new BizException(ErrorCode.PARAM_INVALID, "已暂停的订阅不能续费");
        if ("ONE_TIME".equals(sub.getPricingModel())) throw new BizException(ErrorCode.PARAM_INVALID, "买断类订阅不需续费");

        int days = periodCount * periodToDays(periodType);
        LocalDate newEnd = sub.getEndDate().isBefore(LocalDate.now())
                ? LocalDate.now().plusDays(days) : sub.getEndDate().plusDays(days);

        sub.setEndDate(newEnd);
        sub.setTotalDays(sub.getTotalDays() + days);
        sub.setStatus("ACTIVE");
        sub.setPriceLockedUntil(newEnd);
        subscriptionRepository.save(sub);
        return toResp(sub);
    }

    public List<BillingDTO.ProductCheckResp> checkProducts(Long tenantId, BillingDTO.ProductCheckReq req) {
        return req.getProductCodes().stream().map(code -> {
            BillingDTO.ProductCheckResp resp = new BillingDTO.ProductCheckResp();
            resp.setProductCode(code);
            resp.setStatus("NOT_FOUND");
            resp.setNeedRenewal(false);

            var productOpt = productRepository.findByTenantIdAndProductCode(tenantId, code);
            if (productOpt.isEmpty()) return resp;
            var product = productOpt.get();
            resp.setProductName(product.getProductName());

            var subs = subscriptionRepository.findByTenantIdAndCustomerIdAndSourceTypeAndSourceIdAndStatusIn(
                    tenantId, req.getCustomerId(), "PRODUCT", product.getId(),
                    List.of("ACTIVE", "EXPIRING", "TRIAL", "EXPIRED"));

            if (subs.isEmpty()) return resp;
            var sub = subs.get(0);
            resp.setStatus(sub.getStatus());
            resp.setQuantity(sub.getQuantity());
            resp.setStartDate(sub.getStartDate());
            resp.setEndDate(sub.getEndDate());
            int daysRemaining = (int) ChronoUnit.DAYS.between(LocalDate.now(), sub.getEndDate());
            resp.setDaysRemaining(Math.max(daysRemaining, 0));
            resp.setNeedRenewal(daysRemaining <= 15);
            resp.setRenewalPrice(sub.getRenewalPrice());
            if (sub.getRenewalPrice() != null) resp.setRenewalPriceDisplay(BillingDTO.formatAmount(sub.getRenewalPrice()));
            resp.setUsageQuota(sub.getUsageQuota());
            resp.setUsageUsed(sub.getUsageUsed());
            resp.setUsageRemaining(Math.max(sub.getUsageQuota() - sub.getUsageUsed(), 0));
            return resp;
        }).toList();
    }

    private int periodToDays(String periodType) {
        if (periodType == null) return 365;
        return switch (periodType) { case "YEAR" -> 365; case "QUARTER" -> 90; case "MONTH" -> 30; default -> 365; };
    }

    private BillingDTO.SubscriptionResp toResp(BillingSubscription sub) {
        BillingDTO.SubscriptionResp resp = new BillingDTO.SubscriptionResp();
        resp.setId(sub.getId());
        resp.setSubscriptionNo(sub.getSubscriptionNo());
        resp.setCustomerId(sub.getCustomerId());
        resp.setSourceType(sub.getSourceType());
        resp.setSourceId(sub.getSourceId());
        resp.setSourceName(sub.getSourceName());
        resp.setPricingModel(sub.getPricingModel());
        resp.setQuantity(sub.getQuantity());
        resp.setStatus(sub.getStatus());
        resp.setIsTrial(sub.getIsTrial());
        resp.setStartDate(sub.getStartDate());
        resp.setEndDate(sub.getEndDate());
        resp.setCreatedAt(sub.getCreatedAt());

        resp.setTotalDays(sub.getTotalDays());
        resp.setDaysUsed(sub.getDaysUsed());
        int daysRemaining = (int) ChronoUnit.DAYS.between(LocalDate.now(), sub.getEndDate());
        resp.setDaysRemaining(Math.max(daysRemaining, 0));

        resp.setUsageQuota(sub.getUsageQuota());
        resp.setUsageUsed(sub.getUsageUsed());
        resp.setUsageRemaining(Math.max(sub.getUsageQuota() - sub.getUsageUsed(), 0));
        resp.setUsageUnit(sub.getUsageUnit());

        resp.setSpaceTotal(sub.getSpaceTotal());
        resp.setSpaceUsed(sub.getSpaceUsed());
        resp.setSpaceRemaining(Math.max(sub.getSpaceTotal() - sub.getSpaceUsed(), 0));
        if (sub.getSpaceTotal() > 0) {
            double rate = (double) sub.getSpaceUsed() / sub.getSpaceTotal() * 100;
            resp.setSpaceUsageRate(String.format("%.1f%%", rate));
        }

        resp.setRenewalPrice(sub.getRenewalPrice());
        if (sub.getRenewalPrice() != null) {
            resp.setRenewalPriceDisplay(BillingDTO.formatAmount(sub.getRenewalPrice()));
        }
        return resp;
    }

    private void verifyTenant(BillingSubscription sub) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(sub.getTenantId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该订阅");
        }
    }
}
