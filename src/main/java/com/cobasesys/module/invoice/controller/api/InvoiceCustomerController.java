package com.cobasesys.module.invoice.controller.api;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.service.BillingOrderService;
import com.cobasesys.module.invoice.dto.InvoiceDTO;
import com.cobasesys.module.invoice.service.InvoiceService;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "发票-客户端")
@RestController
@RequestMapping("/admin/invoice/customer")
@RequiredArgsConstructor
public class InvoiceCustomerController {

    private final InvoiceService invoiceService;
    private final BillingOrderService orderService;

    @GetMapping("/available-orders")
    @Operation(summary = "可开票订单列表(已付款+未开票)")
    public ApiResponse<PageResult<BillingDTO.OrderResp>> availableOrders(
            @RequestParam String customerId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int pageSize) {
        return ApiResponse.ok(orderService.listAvailableForInvoice(customerId, PageRequest.of(page - 1, pageSize)));
    }

    @PostMapping("/apply")
    @Operation(summary = "提交开票申请")
    public ApiResponse<InvoiceDTO.ApplicationResponse> apply(@Valid @RequestBody InvoiceDTO.ApplyRequest req) {
        return ApiResponse.ok(invoiceService.apply(req));
    }

    @GetMapping("/my")
    @Operation(summary = "我的开票申请")
    public ApiResponse<PageResult<InvoiceDTO.ApplicationResponse>> myApplications(
            @RequestParam String customerId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(invoiceService.list(customerId, null, PageRequest.of(page - 1, pageSize)));
    }

    @GetMapping("/my/{id}")
    @Operation(summary = "申请详情")
    public ApiResponse<InvoiceDTO.ApplicationResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(invoiceService.getById(id));
    }

    @PostMapping("/callback")
    @Operation(summary = "数电API异步回调")
    public ApiResponse<Void> callback(@RequestBody java.util.Map<String, Object> body) {
        String requestId = body.get("requestId") != null ? body.get("requestId").toString() : null;
        if (requestId != null) {
            // TODO: 根据requestId查找并更新开票申请
            log.info("Invoice callback received: {}", requestId);
        }
        return ApiResponse.ok();
    }
}
