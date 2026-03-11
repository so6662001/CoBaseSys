package com.cobasesys.module.billing.controller.api;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "计费-客户商城")
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingShopController {

    private final BillingProductService productService;
    private final BillingPackageService packageService;
    private final PriceCalculator priceCalculator;
    private final BillingOrderService orderService;
    private final BillingSubscriptionService subscriptionService;
    private final BillingTrialService trialService;
    private final BillingUsageLedgerService usageLedgerService;

    @GetMapping("/products")
    @Operation(summary = "产品列表(上架)")
    public ApiResponse<PageResult<BillingDTO.ProductResp>> products(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(productService.list(1, PageRequest.of(page - 1, pageSize)));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<BillingDTO.ProductResp> productDetail(@PathVariable Long id) {
        return ApiResponse.ok(productService.getById(id));
    }

    @GetMapping("/packages")
    @Operation(summary = "套餐列表(上架)")
    public ApiResponse<PageResult<BillingDTO.PackageResp>> packages(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(packageService.list(1, PageRequest.of(page - 1, pageSize)));
    }

    @GetMapping("/packages/{id}")
    public ApiResponse<BillingDTO.PackageResp> packageDetail(@PathVariable Long id) {
        return ApiResponse.ok(packageService.getById(id));
    }

    @PostMapping("/calculate-price")
    @Operation(summary = "价格计算预览")
    public ApiResponse<BillingDTO.PriceCalculateResp> calculatePrice(
            @Valid @RequestBody BillingDTO.PriceCalculateReq req) {
        return ApiResponse.ok(priceCalculator.calculate(req));
    }

    @PostMapping("/orders")
    @Operation(summary = "创建订单")
    public ApiResponse<BillingDTO.OrderResp> createOrder(@Valid @RequestBody BillingDTO.CreateOrderReq req) {
        return ApiResponse.ok(orderService.createOrder(req, "CUSTOMER", null, null));
    }

    @PostMapping("/orders/pay-callback")
    @Operation(summary = "支付回调")
    public ApiResponse<BillingDTO.OrderResp> payCallback(@RequestParam String orderNo,
                                                           @RequestParam(required = false) String paymentNo,
                                                           @RequestParam(defaultValue = "true") boolean success) {
        return ApiResponse.ok(orderService.payCallback(orderNo, paymentNo, success));
    }

    @GetMapping("/orders/{orderNo}")
    @Operation(summary = "查询订单")
    public ApiResponse<BillingDTO.OrderResp> getOrder(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.getByIdOrNo(orderNo));
    }

    @GetMapping("/orders")
    @Operation(summary = "我的订单")
    public ApiResponse<PageResult<BillingDTO.OrderResp>> myOrders(
            @RequestParam String customerId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(orderService.list(customerId, null, PageRequest.of(page - 1, pageSize)));
    }

    @GetMapping("/my/subscriptions")
    @Operation(summary = "我的订阅")
    public ApiResponse<PageResult<BillingDTO.SubscriptionResp>> mySubscriptions(
            @RequestParam String customerId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(subscriptionService.list(customerId, null, PageRequest.of(page - 1, pageSize)));
    }

    @GetMapping("/my/subscriptions/{id}")
    @Operation(summary = "订阅详情")
    public ApiResponse<BillingDTO.SubscriptionResp> subscriptionDetail(@PathVariable Long id) {
        return ApiResponse.ok(subscriptionService.getById(id));
    }

    @PostMapping("/my/subscriptions/{id}/renew")
    @Operation(summary = "续费")
    public ApiResponse<BillingDTO.SubscriptionResp> renew(@PathVariable Long id,
                                                            @RequestParam(defaultValue = "YEAR") String periodType,
                                                            @RequestParam(defaultValue = "1") int periodCount) {
        return ApiResponse.ok(subscriptionService.renew(id, periodType, periodCount));
    }

    @GetMapping("/my/trials")
    @Operation(summary = "我的试用列表")
    public ApiResponse<?> myTrials(@RequestParam String customerId) {
        Long tenantId = TenantContext.requireTenantId();
        return ApiResponse.ok(trialService.listByCustomer(tenantId, customerId));
    }

    @GetMapping("/my/expiring-alerts")
    @Operation(summary = "到期提醒(登录时调用)")
    public ApiResponse<?> expiringAlerts(@RequestParam String customerId) {
        return ApiResponse.ok(subscriptionService.getExpiringAlerts(TenantContext.requireTenantId(), customerId));
    }

    @PostMapping("/trial/apply")
    @Operation(summary = "申请试用")
    public ApiResponse<BillingDTO.TrialResp> applyTrial(@Valid @RequestBody BillingDTO.TrialApplyReq req) {
        return ApiResponse.ok(trialService.applyTrial(req));
    }

    @PostMapping("/trial/{id}/extend")
    @Operation(summary = "申请延长试用(走审批)")
    public ApiResponse<BillingDTO.TrialExtendApprovalResp> extendTrial(
            @PathVariable Long id, @Valid @RequestBody BillingDTO.TrialExtendReq req) {
        return ApiResponse.ok(trialService.submitExtendApply(id, req));
    }

    @GetMapping("/my/usage-ledger/{subscriptionId}")
    @Operation(summary = "用量流水明细")
    public ApiResponse<PageResult<BillingDTO.UsageLedgerResp>> usageLedger(
            @PathVariable Long subscriptionId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(usageLedgerService.listBySubscription(subscriptionId, PageRequest.of(page - 1, pageSize)));
    }
}
