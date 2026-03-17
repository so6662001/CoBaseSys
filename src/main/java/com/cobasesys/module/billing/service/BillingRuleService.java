package com.cobasesys.module.billing.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.entity.BillingDiscountRule;
import com.cobasesys.module.billing.entity.BillingGiftRule;
import com.cobasesys.module.billing.repository.BillingDiscountRuleRepository;
import com.cobasesys.module.billing.repository.BillingGiftRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingRuleService {

    private final BillingDiscountRuleRepository discountRepository;
    private final BillingGiftRuleRepository giftRepository;

    @Transactional("billingTransactionManager")
    public BillingDiscountRule createDiscount(BillingDTO.DiscountRuleReq req) {
        BillingDiscountRule rule = new BillingDiscountRule();
        BeanUtils.copyProperties(req, rule);
        rule.setTenantId(TenantContext.requireTenantId());
        return discountRepository.save(rule);
    }

    @Transactional("billingTransactionManager")
    public BillingDiscountRule updateDiscount(Long id, BillingDTO.DiscountRuleReq req) {
        BillingDiscountRule rule = discountRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        if (req.getRuleName() != null) rule.setRuleName(req.getRuleName());
        if (req.getDiscountRate() != null) rule.setDiscountRate(req.getDiscountRate());
        if (req.getThresholdAmount() != null) rule.setThresholdAmount(req.getThresholdAmount());
        if (req.getBonusPoints() != null) rule.setBonusPoints(req.getBonusPoints());
        if (req.getMinQuantity() != null) rule.setMinQuantity(req.getMinQuantity());
        if (req.getPriority() != null) rule.setPriority(req.getPriority());
        if (req.getEffectiveFrom() != null) rule.setEffectiveFrom(req.getEffectiveFrom());
        if (req.getEffectiveTo() != null) rule.setEffectiveTo(req.getEffectiveTo());
        return discountRepository.save(rule);
    }

    public PageResult<BillingDiscountRule> listDiscounts(Pageable pageable) {
        return PageResult.from(discountRepository.findByTenantId(TenantContext.requireTenantId(), pageable));
    }

    @Transactional("billingTransactionManager")
    public void deleteDiscount(Long id) { discountRepository.deleteById(id); }

    @Transactional("billingTransactionManager")
    public BillingGiftRule createGift(BillingDTO.GiftRuleReq req) {
        BillingGiftRule rule = new BillingGiftRule();
        BeanUtils.copyProperties(req, rule);
        rule.setTenantId(TenantContext.requireTenantId());
        return giftRepository.save(rule);
    }

    @Transactional("billingTransactionManager")
    public BillingGiftRule updateGift(Long id, BillingDTO.GiftRuleReq req) {
        BillingGiftRule rule = giftRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        if (req.getRuleName() != null) rule.setRuleName(req.getRuleName());
        if (req.getConditionQuantity() != null) rule.setConditionQuantity(req.getConditionQuantity());
        if (req.getGiftQuantity() != null) rule.setGiftQuantity(req.getGiftQuantity());
        if (req.getGiftPoints() != null) rule.setGiftPoints(req.getGiftPoints());
        if (req.getEffectiveFrom() != null) rule.setEffectiveFrom(req.getEffectiveFrom());
        if (req.getEffectiveTo() != null) rule.setEffectiveTo(req.getEffectiveTo());
        return giftRepository.save(rule);
    }

    public PageResult<BillingGiftRule> listGifts(Pageable pageable) {
        return PageResult.from(giftRepository.findByTenantId(TenantContext.requireTenantId(), pageable));
    }

    @Transactional("billingTransactionManager")
    public void deleteGift(Long id) { giftRepository.deleteById(id); }
}
