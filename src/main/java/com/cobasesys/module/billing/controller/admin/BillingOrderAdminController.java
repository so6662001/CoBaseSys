package com.cobasesys.module.billing.controller.admin;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.service.BillingOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "计费-订单管理")
@RestController
@RequestMapping("/admin/billing/orders")
@RequiredArgsConstructor
public class BillingOrderAdminController {

    private final BillingOrderService orderService;

    @PostMapping("/proxy")
    @Operation(summary = "代客下单")
    public ApiResponse<BillingDTO.OrderResp> proxyOrder(@Valid @RequestBody BillingDTO.ProxyOrderReq req,
                                                          @RequestParam String operatorId,
                                                          @RequestParam String operatorName) {
        BillingDTO.CreateOrderReq createReq = new BillingDTO.CreateOrderReq();
        createReq.setCustomerId(req.getCustomerId());
        createReq.setCustomerName(req.getCustomerName());
        createReq.setItems(req.getItems());
        createReq.setUsePoints(req.getUsePoints());
        createReq.setRemark(req.getRemark());
        BillingDTO.OrderResp order = orderService.createOrder(createReq, "ADMIN", operatorId, operatorName);

        if (Boolean.TRUE.equals(req.getAutoConfirmPayment())) {
            order = orderService.confirmPayment(order.getId(), "offline", null);
        }
        return ApiResponse.ok(order);
    }

    @GetMapping
    @Operation(summary = "订单列表")
    public ApiResponse<PageResult<BillingDTO.OrderResp>> list(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) Integer paymentStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(orderService.list(customerId, paymentStatus, PageRequest.of(page - 1, pageSize)));
    }

    @GetMapping("/{idOrNo}")
    @Operation(summary = "订单详情")
    public ApiResponse<BillingDTO.OrderResp> getById(@PathVariable String idOrNo) {
        return ApiResponse.ok(orderService.getByIdOrNo(idOrNo));
    }

    @PostMapping("/{id}/confirm-payment")
    @Operation(summary = "确认付款")
    public ApiResponse<BillingDTO.OrderResp> confirmPayment(
            @PathVariable Long id,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String paymentNo) {
        return ApiResponse.ok(orderService.confirmPayment(id, paymentMethod, paymentNo));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消订单")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return ApiResponse.ok();
    }
}
