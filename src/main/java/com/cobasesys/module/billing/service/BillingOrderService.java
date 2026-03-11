package com.cobasesys.module.billing.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.common.util.IdGenerator;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.entity.*;
import com.cobasesys.module.billing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingOrderService {

    private final BillingOrderRepository orderRepository;
    private final BillingOrderItemRepository orderItemRepository;
    private final BillingSubscriptionRepository subscriptionRepository;
    private final BillingProductRepository productRepository;
    private final BillingPackageRepository packageRepository;
    private final BillingPricingPlanRepository pricingPlanRepository;
    private final PriceCalculator priceCalculator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional("billingTransactionManager")
    public BillingDTO.OrderResp createOrder(BillingDTO.CreateOrderReq req, String orderSource,
                                             String operatorId, String operatorName) {
        Long tenantId = TenantContext.requireTenantId();

        BillingDTO.PriceCalculateReq calcReq = new BillingDTO.PriceCalculateReq();
        calcReq.setCustomerId(req.getCustomerId());
        calcReq.setItems(req.getItems());
        calcReq.setUsePoints(req.getUsePoints());
        BillingDTO.PriceCalculateResp priceResult = priceCalculator.calculate(calcReq);

        BillingOrder order = new BillingOrder();
        order.setTenantId(tenantId);
        order.setOrderNo(IdGenerator.generate("BO"));
        order.setCustomerId(req.getCustomerId());
        order.setCustomerName(req.getCustomerName());
        order.setOrderType("NEW_PURCHASE");
        order.setOrderSource(orderSource);
        order.setOperatorId(operatorId);
        order.setOperatorName(operatorName);
        order.setTotalAmount(priceResult.getTotalAmount());
        order.setDiscountAmount(priceResult.getDiscountAmount());
        order.setPointsDeductAmount(priceResult.getPointsDeductAmount());
        order.setPointsUsed(priceResult.getPointsNeeded());
        order.setActualAmount(priceResult.getActualAmount());
        order.setPaymentStatus(0);
        order.setStatus(0);
        order.setRemark(req.getRemark());
        orderRepository.save(order);

        for (int i = 0; i < req.getItems().size(); i++) {
            BillingDTO.OrderItemReq itemReq = req.getItems().get(i);
            BillingDTO.ItemPriceDetail detail = priceResult.getItemDetails().get(i);
            saveOrderItem(order.getId(), itemReq, detail, 0, null);
        }

        if (priceResult.getGifts() != null) {
            long giftTotal = 0;
            for (BillingDTO.GiftPreview gift : priceResult.getGifts()) {
                if ("PRODUCT".equals(gift.getGiftType()) || "PACKAGE".equals(gift.getGiftType())) {
                    BillingOrderItem giftItem = new BillingOrderItem();
                    giftItem.setOrderId(order.getId());
                    giftItem.setItemType(gift.getGiftType());
                    giftItem.setItemId(0L);
                    giftItem.setItemName(gift.getGiftName() + " (赠品)");
                    giftItem.setQuantity(gift.getGiftQuantity());
                    giftItem.setUnitPrice(0L);
                    giftItem.setOriginalAmount(0L);
                    giftItem.setActualAmount(0L);
                    giftItem.setIsGift(1);
                    orderItemRepository.save(giftItem);
                }
            }
            order.setGiftAmount(giftTotal);
            orderRepository.save(order);
        }

        return toOrderResp(order);
    }

    @Transactional("billingTransactionManager")
    public BillingDTO.OrderResp confirmPayment(Long orderId, String paymentMethod, String paymentNo) {
        BillingOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        if (order.getPaymentStatus() != 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "订单状态不允许确认付款");
        }

        order.setPaymentStatus(1);
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : "offline");
        order.setPaymentNo(paymentNo);
        order.setPaidAt(LocalDateTime.now());
        order.setStatus(1);
        orderRepository.save(order);

        activateSubscriptions(order);

        return toOrderResp(order);
    }

    @Transactional("billingTransactionManager")
    public void cancelOrder(Long orderId) {
        BillingOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        if (order.getPaymentStatus() == 1) {
            throw new BizException(ErrorCode.PARAM_INVALID, "已支付的订单不能取消");
        }
        order.setPaymentStatus(2);
        order.setStatus(2);
        orderRepository.save(order);
    }

    public PageResult<BillingDTO.OrderResp> list(String customerId, Integer paymentStatus, Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        Page<BillingOrder> page;
        if (customerId != null) {
            page = orderRepository.findByTenantIdAndCustomerIdOrderByCreatedAtDesc(tenantId, customerId, pageable);
        } else if (paymentStatus != null) {
            page = orderRepository.findByTenantIdAndPaymentStatus(tenantId, paymentStatus, pageable);
        } else {
            page = orderRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        }
        return PageResult.from(page.map(this::toOrderResp));
    }

    public BillingDTO.OrderResp getByIdOrNo(String idOrNo) {
        BillingOrder order;
        try {
            order = orderRepository.findById(Long.parseLong(idOrNo)).orElse(null);
        } catch (NumberFormatException e) {
            order = null;
        }
        if (order == null) {
            order = orderRepository.findByOrderNo(idOrNo).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        }
        return toOrderResp(order);
    }

    private void activateSubscriptions(BillingOrder order) {
        List<BillingOrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (BillingOrderItem item : items) {
            LocalDate start = item.getStartDate() != null ? item.getStartDate() : LocalDate.now();
            LocalDate end = item.getEndDate();
            if (end == null) {
                int days = item.getPeriodCount() * periodToDays(item.getPeriodType());
                end = days > 0 ? start.plusDays(days) : start.plusYears(100);
            }

            BillingSubscription sub = new BillingSubscription();
            sub.setTenantId(order.getTenantId());
            sub.setCustomerId(order.getCustomerId());
            sub.setSubscriptionNo(IdGenerator.generate("SUB"));
            sub.setSourceType(item.getItemType());
            sub.setSourceId(item.getItemId());
            sub.setSourceName(item.getItemName());
            sub.setOrderId(order.getId());
            sub.setOrderItemId(item.getId());
            sub.setPricingModel(item.getPricingModel());
            sub.setPricingPlanId(item.getPricingPlanId());
            sub.setQuantity(item.getQuantity());
            sub.setStatus(item.getIsGift() == 1 ? "ACTIVE" : "ACTIVE");
            sub.setIsTrial(0);
            sub.setStartDate(start);
            sub.setEndDate(end);
            sub.setTotalDays((int) (end.toEpochDay() - start.toEpochDay()));
            sub.setOriginalUnitPrice(item.getUnitPrice());
            sub.setRenewalPrice(item.getActualAmount());
            sub.setPriceLockedUntil(end);
            subscriptionRepository.save(sub);

            item.setStartDate(start);
            item.setEndDate(end);
            orderItemRepository.save(item);
        }
    }

    private int periodToDays(String periodType) {
        if (periodType == null) return 365;
        return switch (periodType) {
            case "YEAR" -> 365;
            case "QUARTER" -> 90;
            case "MONTH" -> 30;
            default -> 365;
        };
    }

    private void saveOrderItem(Long orderId, BillingDTO.OrderItemReq itemReq,
                                BillingDTO.ItemPriceDetail detail, int isGift, Long giftRuleId) {
        List<BillingPricingPlan> plans = pricingPlanRepository.findActivePlans(
                itemReq.getItemType(), itemReq.getItemId(), LocalDateTime.now());

        BillingOrderItem item = new BillingOrderItem();
        item.setOrderId(orderId);
        item.setItemType(itemReq.getItemType());
        item.setItemId(itemReq.getItemId());
        item.setItemName(detail.getItemName());
        item.setPricingPlanId(plans.isEmpty() ? null : plans.get(0).getId());
        item.setPricingModel(plans.isEmpty() ? null : plans.get(0).getPricingModel());
        item.setQuantity(itemReq.getQuantity());
        item.setUnitPrice(detail.getUnitPrice());
        item.setOriginalAmount(detail.getOriginalAmount());
        item.setDiscountAmount(detail.getDiscountAmount());
        item.setActualAmount(detail.getActualAmount());
        item.setPeriodType(itemReq.getPeriodType());
        item.setPeriodCount(itemReq.getPeriodCount());
        item.setIsGift(isGift);
        item.setGiftRuleId(giftRuleId);
        orderItemRepository.save(item);
    }

    private BillingDTO.OrderResp toOrderResp(BillingOrder order) {
        BillingDTO.OrderResp resp = new BillingDTO.OrderResp();
        BeanUtils.copyProperties(order, resp);
        resp.setActualAmountDisplay(BillingDTO.formatAmount(order.getActualAmount()));
        resp.setPaymentStatusText(BillingDTO.paymentStatusText(order.getPaymentStatus()));
        List<BillingOrderItem> items = orderItemRepository.findByOrderId(order.getId());
        resp.setItems(items.stream().map(item -> {
            BillingDTO.OrderItemResp ir = new BillingDTO.OrderItemResp();
            BeanUtils.copyProperties(item, ir);
            return ir;
        }).toList());
        return resp;
    }
}
