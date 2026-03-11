package com.cobasesys.module.billing.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.entity.BillingPricingPlan;
import com.cobasesys.module.billing.repository.BillingPricingPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BillingPricingService {

    private final BillingPricingPlanRepository planRepository;

    @Transactional("billingTransactionManager")
    public BillingDTO.PricingPlanResp create(BillingDTO.PricingPlanCreateReq req) {
        BillingPricingPlan plan = new BillingPricingPlan();
        BeanUtils.copyProperties(req, plan);
        plan.setTenantId(TenantContext.requireTenantId());
        if (plan.getEffectiveFrom() == null) plan.setEffectiveFrom(LocalDateTime.now());
        planRepository.save(plan);
        return toResp(plan);
    }

    @Transactional("billingTransactionManager")
    public BillingDTO.PricingPlanResp update(Long id, BillingDTO.PricingPlanCreateReq req) {
        BillingPricingPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        if (req.getPlanName() != null) plan.setPlanName(req.getPlanName());
        if (req.getSoftwareFee() != null) plan.setSoftwareFee(req.getSoftwareFee());
        if (req.getAnnualServiceFee() != null) plan.setAnnualServiceFee(req.getAnnualServiceFee());
        if (req.getPeriodType() != null) plan.setPeriodType(req.getPeriodType());
        if (req.getPeriodPrice() != null) plan.setPeriodPrice(req.getPeriodPrice());
        if (req.getIncludedQuantity() != null) plan.setIncludedQuantity(req.getIncludedQuantity());
        if (req.getOverageUnitName() != null) plan.setOverageUnitName(req.getOverageUnitName());
        if (req.getOverageUnitPrice() != null) plan.setOverageUnitPrice(req.getOverageUnitPrice());
        if (req.getUnitName() != null) plan.setUnitName(req.getUnitName());
        if (req.getUnitPrice() != null) plan.setUnitPrice(req.getUnitPrice());
        if (req.getTieredPricing() != null) plan.setTieredPricing(req.getTieredPricing());
        if (req.getRentalPeriodType() != null) plan.setRentalPeriodType(req.getRentalPeriodType());
        if (req.getRentalPrice() != null) plan.setRentalPrice(req.getRentalPrice());
        if (req.getSpaceUnit() != null) plan.setSpaceUnit(req.getSpaceUnit());
        if (req.getSpaceUnitPrice() != null) plan.setSpaceUnitPrice(req.getSpaceUnitPrice());
        if (req.getOneTimePrice() != null) plan.setOneTimePrice(req.getOneTimePrice());
        if (req.getValidityDays() != null) plan.setValidityDays(req.getValidityDays());
        if (req.getPriority() != null) plan.setPriority(req.getPriority());
        if (req.getEffectiveFrom() != null) plan.setEffectiveFrom(req.getEffectiveFrom());
        if (req.getEffectiveTo() != null) plan.setEffectiveTo(req.getEffectiveTo());
        planRepository.save(plan);
        return toResp(plan);
    }

    public PageResult<BillingDTO.PricingPlanResp> list(String targetType, Long targetId, Pageable pageable) {
        var page = targetType != null && targetId != null
                ? planRepository.findByTargetTypeAndTargetId(targetType, targetId, pageable)
                : planRepository.findByTenantId(TenantContext.requireTenantId(), pageable);
        return PageResult.from(page.map(this::toResp));
    }

    @Transactional("billingTransactionManager")
    public void delete(Long id) {
        planRepository.deleteById(id);
    }

    private BillingDTO.PricingPlanResp toResp(BillingPricingPlan p) {
        BillingDTO.PricingPlanResp resp = new BillingDTO.PricingPlanResp();
        BeanUtils.copyProperties(p, resp);
        return resp;
    }
}
