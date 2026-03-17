package com.cobasesys.module.billing.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.entity.*;
import com.cobasesys.module.billing.repository.*;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceCalculator {

    private final BillingProductRepository productRepository;
    private final BillingPackageRepository packageRepository;
    private final BillingPackageItemRepository packageItemRepository;
    private final BillingPricingPlanRepository pricingPlanRepository;
    private final BillingDiscountRuleRepository discountRuleRepository;
    private final BillingGiftRuleRepository giftRuleRepository;

    public BillingDTO.PriceCalculateResp calculate(BillingDTO.PriceCalculateReq req) {
        Long tenantId = TenantContext.requireTenantId();
        LocalDateTime now = LocalDateTime.now();

        List<BillingDTO.ItemPriceDetail> itemDetails = new ArrayList<>();
        long totalAmount = 0;
        long totalDiscount = 0;

        List<BillingDiscountRule> discountRules = discountRuleRepository.findActiveRules(tenantId, now);
        List<BillingGiftRule> giftRules = giftRuleRepository.findActiveRules(tenantId, now);

        for (BillingDTO.OrderItemReq item : req.getItems()) {
            BillingDTO.ItemPriceDetail detail = calculateItem(item, discountRules, now);
            itemDetails.add(detail);
            totalAmount += detail.getOriginalAmount();
            totalDiscount += detail.getDiscountAmount();
        }

        // Order-level discounts (AMOUNT_DISCOUNT / AMOUNT_POINTS_BONUS)
        long orderSubtotal = totalAmount - totalDiscount;
        long orderLevelDiscount = 0;
        long bonusPoints = 0;
        for (BillingDiscountRule rule : discountRules) {
            if ("AMOUNT_DISCOUNT".equals(rule.getDiscountType())
                    && rule.getThresholdAmount() != null && orderSubtotal >= rule.getThresholdAmount()
                    && rule.getDiscountRate() != null) {
                long d = orderSubtotal - java.math.BigDecimal.valueOf(orderSubtotal)
                        .multiply(rule.getDiscountRate()).setScale(0, java.math.RoundingMode.FLOOR).longValue();
                orderLevelDiscount = Math.max(orderLevelDiscount, d);
            }
            if ("AMOUNT_POINTS_BONUS".equals(rule.getDiscountType())
                    && rule.getThresholdAmount() != null && orderSubtotal >= rule.getThresholdAmount()
                    && rule.getBonusPoints() != null) {
                bonusPoints = Math.max(bonusPoints, rule.getBonusPoints());
            }
        }
        totalDiscount += orderLevelDiscount;

        List<BillingDTO.GiftPreview> gifts = matchGifts(req.getItems(), giftRules, totalAmount - totalDiscount);
        if (bonusPoints > 0) {
            BillingDTO.GiftPreview pointsGift = new BillingDTO.GiftPreview();
            pointsGift.setGiftType("POINTS");
            pointsGift.setGiftPoints(bonusPoints);
            pointsGift.setRuleName("满额送积分");
            gifts.add(pointsGift);
        }

        long afterDiscount = totalAmount - totalDiscount;
        long pointsDeduct = 0;
        long pointsNeeded = 0;

        if (Boolean.TRUE.equals(req.getUsePoints())) {
            var pointsResult = calculatePointsDeduction(req.getItems(), afterDiscount);
            pointsDeduct = pointsResult[0];
            pointsNeeded = pointsResult[1];
        }

        long actualAmount = Math.max(0, afterDiscount - pointsDeduct);

        BillingDTO.PriceCalculateResp resp = new BillingDTO.PriceCalculateResp();
        resp.setTotalAmount(totalAmount);
        resp.setDiscountAmount(totalDiscount);
        resp.setPointsDeductAmount(pointsDeduct);
        resp.setPointsNeeded(pointsNeeded);
        resp.setActualAmount(actualAmount);
        resp.setActualAmountDisplay(BillingDTO.formatAmount(actualAmount));
        resp.setGifts(gifts);
        resp.setItemDetails(itemDetails);
        return resp;
    }

    private BillingDTO.ItemPriceDetail calculateItem(BillingDTO.OrderItemReq item,
                                                       List<BillingDiscountRule> discountRules,
                                                       LocalDateTime now) {
        String itemName;
        String pricingModel;
        if ("PRODUCT".equals(item.getItemType())) {
            BillingProduct product = productRepository.findById(item.getItemId())
                    .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "产品不存在"));
            itemName = product.getProductName();
            pricingModel = product.getPricingModel();
        } else {
            BillingPackage pkg = packageRepository.findById(item.getItemId())
                    .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "套餐不存在"));
            itemName = pkg.getPackageName();
            pricingModel = pkg.getPricingModel();
        }

        List<BillingPricingPlan> plans = pricingPlanRepository.findActivePlans(
                item.getItemType(), item.getItemId(), now);
        if (plans.isEmpty()) throw new BizException(ErrorCode.NOT_FOUND, itemName + " 没有可用的定价方案");
        BillingPricingPlan plan = plans.get(0);

        long unitPrice = calculateUnitPrice(plan);
        long originalAmount = unitPrice * item.getQuantity() * item.getPeriodCount();

        long discountAmount = 0;
        for (BillingDiscountRule rule : discountRules) {
            if (matchesDiscount(rule, item)) {
                if (rule.getDiscountRate() != null) {
                    long discounted = originalAmount - BigDecimal.valueOf(originalAmount)
                            .multiply(rule.getDiscountRate()).setScale(0, RoundingMode.FLOOR).longValue();
                    discountAmount = Math.max(discountAmount, discounted);
                }
            }
        }

        BillingDTO.ItemPriceDetail detail = new BillingDTO.ItemPriceDetail();
        detail.setItemType(item.getItemType());
        detail.setItemId(item.getItemId());
        detail.setItemName(itemName);
        detail.setQuantity(item.getQuantity());
        detail.setUnitPrice(unitPrice);
        detail.setOriginalAmount(originalAmount);
        detail.setDiscountAmount(discountAmount);
        detail.setActualAmount(originalAmount - discountAmount);

        if ("PACKAGE".equals(item.getItemType())) {
            calculateBundleSavings(detail, item, now);
        }

        return detail;
    }

    private void calculateBundleSavings(BillingDTO.ItemPriceDetail detail,
                                          BillingDTO.OrderItemReq item, LocalDateTime now) {
        var pkgItems = packageItemRepository.findByPackageIdOrderBySortOrderAsc(item.getItemId());
        if (pkgItems.isEmpty()) return;

        long individualTotal = 0;
        List<BillingDTO.PackageProductDetail> productDetails = new ArrayList<>();

        for (var pkgItem : pkgItems) {
            BillingDTO.PackageProductDetail pd = new BillingDTO.PackageProductDetail();
            pd.setProductId(pkgItem.getProductId());
            pd.setQuantity(pkgItem.getQuantity());

            productRepository.findById(pkgItem.getProductId()).ifPresent(p -> pd.setProductName(p.getProductName()));

            var productPlans = pricingPlanRepository.findActivePlans("PRODUCT", pkgItem.getProductId(), now);
            if (!productPlans.isEmpty()) {
                long productUnitPrice = calculateUnitPrice(productPlans.get(0));
                pd.setUnitPrice(productUnitPrice);
                long subtotal = productUnitPrice * pkgItem.getQuantity() * item.getPeriodCount();
                pd.setSubtotal(subtotal);
                individualTotal += subtotal;
            }
            productDetails.add(pd);
        }

        long bundleTotal = detail.getOriginalAmount();
        long savings = (individualTotal * item.getQuantity()) - bundleTotal;

        detail.setIndividualTotal(individualTotal * item.getQuantity());
        detail.setIndividualTotalDisplay(BillingDTO.formatAmount(individualTotal * item.getQuantity()));
        detail.setBundleSavings(Math.max(savings, 0));
        detail.setBundleSavingsDisplay(savings > 0 ? BillingDTO.formatAmount(savings) : null);
        detail.setPackageProducts(productDetails);
    }

    private long calculateUnitPrice(BillingPricingPlan plan) {
        return switch (plan.getPricingModel()) {
            case "ONE_TIME_ANNUAL" -> plan.getSoftwareFee() + plan.getAnnualServiceFee();
            case "SUBSCRIPTION" -> plan.getPeriodPrice();
            case "USAGE_BASED", "TIERED_PROGRESSIVE" -> plan.getUnitPrice();
            case "CLOUD_RENTAL" -> plan.getRentalPrice();
            case "SPACE_RENTAL" -> plan.getSpaceUnitPrice();
            case "ONE_TIME" -> plan.getOneTimePrice();
            case "QUOTA_PLAN" -> plan.getPeriodPrice();
            default -> 0L;
        };
    }

    private boolean matchesDiscount(BillingDiscountRule rule, BillingDTO.OrderItemReq item) {
        if ("PRODUCT_DISCOUNT".equals(rule.getDiscountType()) && "PRODUCT".equals(item.getItemType())) {
            return rule.getTargetId() != null && rule.getTargetId().equals(item.getItemId())
                    && item.getQuantity() >= rule.getMinQuantity();
        }
        if ("PACKAGE_DISCOUNT".equals(rule.getDiscountType()) && "PACKAGE".equals(item.getItemType())) {
            return rule.getTargetId() != null && rule.getTargetId().equals(item.getItemId())
                    && item.getQuantity() >= rule.getMinQuantity();
        }
        return false;
    }

    private List<BillingDTO.GiftPreview> matchGifts(List<BillingDTO.OrderItemReq> items,
                                                      List<BillingGiftRule> giftRules, long orderAmount) {
        List<BillingDTO.GiftPreview> gifts = new ArrayList<>();
        for (BillingGiftRule rule : giftRules) {
            boolean matched = switch (rule.getConditionType()) {
                case "BUY_PRODUCT" -> items.stream().anyMatch(i ->
                        "PRODUCT".equals(i.getItemType())
                                && i.getItemId().equals(rule.getConditionTargetId())
                                && i.getQuantity() >= rule.getConditionQuantity());
                case "BUY_PACKAGE" -> items.stream().anyMatch(i ->
                        "PACKAGE".equals(i.getItemType())
                                && i.getItemId().equals(rule.getConditionTargetId())
                                && i.getQuantity() >= rule.getConditionQuantity());
                case "SPEND_AMOUNT" -> orderAmount >= rule.getConditionQuantity() * 100L;
                default -> false;
            };

            if (matched) {
                BillingDTO.GiftPreview gift = new BillingDTO.GiftPreview();
                gift.setGiftType(rule.getGiftType());
                gift.setGiftQuantity(rule.getGiftQuantity());
                gift.setGiftPoints(rule.getGiftPoints());
                gift.setRuleName(rule.getRuleName());
                if (rule.getGiftTargetId() != null) {
                    if ("PRODUCT".equals(rule.getGiftType())) {
                        productRepository.findById(rule.getGiftTargetId())
                                .ifPresent(p -> gift.setGiftName(p.getProductName()));
                    } else if ("PACKAGE".equals(rule.getGiftType())) {
                        packageRepository.findById(rule.getGiftTargetId())
                                .ifPresent(p -> gift.setGiftName(p.getPackageName()));
                    }
                }
                gifts.add(gift);
            }
        }
        return gifts;
    }

    private long[] calculatePointsDeduction(List<BillingDTO.OrderItemReq> items, long afterDiscount) {
        BigDecimal maxRatio = BigDecimal.ZERO;
        BigDecimal exchangeRate = BigDecimal.ONE;

        for (BillingDTO.OrderItemReq item : items) {
            BigDecimal ratio;
            BigDecimal rate;
            if ("PRODUCT".equals(item.getItemType())) {
                BillingProduct p = productRepository.findById(item.getItemId()).orElse(null);
                if (p == null || p.getPointsPayable() != 1) continue;
                ratio = p.getMaxPointsRatio();
                rate = p.getPointsExchangeRate();
            } else {
                BillingPackage p = packageRepository.findById(item.getItemId()).orElse(null);
                if (p == null || p.getPointsPayable() != 1) continue;
                ratio = p.getMaxPointsRatio();
                rate = p.getPointsExchangeRate();
            }
            if (ratio.compareTo(maxRatio) > 0) {
                maxRatio = ratio;
                exchangeRate = rate;
            }
        }

        if (maxRatio.compareTo(BigDecimal.ZERO) <= 0) return new long[]{0, 0};

        long maxDeduct = BigDecimal.valueOf(afterDiscount).multiply(maxRatio)
                .setScale(0, RoundingMode.FLOOR).longValue();
        long pointsNeeded = BigDecimal.valueOf(maxDeduct).divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING)
                .multiply(exchangeRate).setScale(0, RoundingMode.CEILING).longValue();

        return new long[]{maxDeduct, pointsNeeded};
    }
}
