package com.cobasesys.module.billing.controller.admin;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.service.BillingSubscriptionService;
import com.cobasesys.module.billing.service.BillingUsageLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "计费-订阅管理")
@RestController
@RequestMapping("/admin/billing/subscriptions")
@RequiredArgsConstructor
public class BillingSubscriptionAdminController {

    private final BillingSubscriptionService subscriptionService;
    private final BillingUsageLedgerService usageLedgerService;

    @GetMapping
    @Operation(summary = "订阅列表")
    public ApiResponse<PageResult<BillingDTO.SubscriptionResp>> list(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(subscriptionService.list(customerId, status, PageRequest.of(page - 1, pageSize)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "订阅详情")
    public ApiResponse<BillingDTO.SubscriptionResp> getById(@PathVariable Long id) {
        return ApiResponse.ok(subscriptionService.getById(id));
    }

    @PostMapping("/{id}/extend")
    @Operation(summary = "手动延期")
    public ApiResponse<BillingDTO.SubscriptionResp> extend(@PathVariable Long id, @RequestParam int days) {
        return ApiResponse.ok(subscriptionService.extend(id, days));
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "暂停订阅")
    public ApiResponse<Void> suspend(@PathVariable Long id) {
        subscriptionService.suspend(id); return ApiResponse.ok();
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "恢复订阅")
    public ApiResponse<Void> resume(@PathVariable Long id) {
        subscriptionService.resume(id); return ApiResponse.ok();
    }

    @GetMapping("/{id}/usage-ledger")
    @Operation(summary = "用量流水")
    public ApiResponse<PageResult<BillingDTO.UsageLedgerResp>> usageLedger(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(usageLedgerService.listBySubscription(id, PageRequest.of(page - 1, pageSize)));
    }
}
