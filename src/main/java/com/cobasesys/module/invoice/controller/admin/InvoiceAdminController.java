package com.cobasesys.module.invoice.controller.admin;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.module.invoice.dto.InvoiceDTO;
import com.cobasesys.module.invoice.service.InvoiceService;
import com.cobasesys.module.security.annotation.Auditable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "发票管理-后台")
@RestController
@RequestMapping("/admin/invoice/applications")
@RequiredArgsConstructor
public class InvoiceAdminController {

    private final InvoiceService invoiceService;

    @GetMapping
    @Operation(summary = "开票申请列表")
    public ApiResponse<PageResult<InvoiceDTO.ApplicationResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(invoiceService.list(null, status, PageRequest.of(page - 1, pageSize)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "申请详情")
    public ApiResponse<InvoiceDTO.ApplicationResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(invoiceService.getById(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审核通过并开票")
    @Auditable(module = "invoice", action = "approve", description = "审核通过并开具发票")
    public ApiResponse<InvoiceDTO.ApplicationResponse> approve(
            @PathVariable Long id, @Valid @RequestBody InvoiceDTO.ApproveRequest req) {
        return ApiResponse.ok(invoiceService.approve(id, req));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审核驳回")
    @Auditable(module = "invoice", action = "reject", description = "驳回开票申请")
    public ApiResponse<InvoiceDTO.ApplicationResponse> reject(
            @PathVariable Long id, @Valid @RequestBody InvoiceDTO.RejectRequest req) {
        return ApiResponse.ok(invoiceService.reject(id, req));
    }

    @PostMapping("/{id}/void")
    @Operation(summary = "红冲/作废")
    @Auditable(module = "invoice", action = "void", description = "红冲/作废发票")
    public ApiResponse<InvoiceDTO.ApplicationResponse> voidInvoice(
            @PathVariable Long id, @Valid @RequestBody InvoiceDTO.VoidRequest req) {
        return ApiResponse.ok(invoiceService.voidInvoice(id, req));
    }

    @PostMapping("/{id}/resend-email")
    @Operation(summary = "重发邮件")
    public ApiResponse<Void> resendEmail(@PathVariable Long id) {
        invoiceService.resendEmail(id);
        return ApiResponse.ok();
    }

    @GetMapping("/statistics")
    @Operation(summary = "开票统计")
    public ApiResponse<InvoiceDTO.Statistics> statistics() {
        return ApiResponse.ok(invoiceService.getStatistics());
    }
}
