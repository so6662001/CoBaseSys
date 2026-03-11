package com.cobasesys.module.billing.controller.admin;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.service.BillingSubscriptionService;
import com.cobasesys.module.billing.service.BillingTrialService;
import com.cobasesys.module.billing.service.BillingUsageLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "计费-运营报表")
@RestController
@RequestMapping("/admin/billing/reports")
@RequiredArgsConstructor
public class BillingReportAdminController {

    private final BillingSubscriptionService subscriptionService;
    private final BillingTrialService trialService;
    private final BillingUsageLedgerService usageLedgerService;

    @GetMapping("/expiring-subscriptions")
    @Operation(summary = "即将到期的订阅")
    public ApiResponse<List<BillingDTO.SubscriptionResp>> expiringSubscriptions() {
        return ApiResponse.ok(subscriptionService.getExpiringSoon(TenantContext.requireTenantId()));
    }

    @GetMapping("/expired-subscriptions")
    @Operation(summary = "已到期的订阅")
    public ApiResponse<List<BillingDTO.SubscriptionResp>> expiredSubscriptions() {
        return ApiResponse.ok(subscriptionService.getExpired(TenantContext.requireTenantId()));
    }

    @GetMapping("/expiring-trials")
    @Operation(summary = "即将到期的试用")
    public ApiResponse<PageResult<BillingDTO.TrialResp>> expiringTrials(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int pageSize) {
        return ApiResponse.ok(trialService.listTrials("ACTIVE", PageRequest.of(page - 1, pageSize)));
    }

    @GetMapping("/expired-trials")
    @Operation(summary = "已到期的试用")
    public ApiResponse<PageResult<BillingDTO.TrialResp>> expiredTrials(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int pageSize) {
        return ApiResponse.ok(trialService.listTrials("EXPIRED", PageRequest.of(page - 1, pageSize)));
    }

    @GetMapping("/subscription-detail/{id}")
    @Operation(summary = "订阅详情报表")
    public ApiResponse<BillingDTO.SubscriptionResp> subscriptionDetail(@PathVariable Long id) {
        return ApiResponse.ok(subscriptionService.getById(id));
    }

    @GetMapping("/usage-ledger")
    @Operation(summary = "用量流水明细")
    public ApiResponse<PageResult<BillingDTO.UsageLedgerResp>> usageLedger(
            @RequestParam(required = false) Long subscriptionId,
            @RequestParam(required = false) String customerId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        if (subscriptionId != null) {
            return ApiResponse.ok(usageLedgerService.listBySubscription(subscriptionId, PageRequest.of(page - 1, pageSize)));
        }
        return ApiResponse.ok(usageLedgerService.listByCustomer(
                TenantContext.requireTenantId(), customerId, PageRequest.of(page - 1, pageSize)));
    }

    @GetMapping("/customer-assets/{customerId}")
    @Operation(summary = "客户资产全景")
    public ApiResponse<PageResult<BillingDTO.SubscriptionResp>> customerAssets(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int pageSize) {
        return ApiResponse.ok(subscriptionService.list(customerId, null, PageRequest.of(page - 1, pageSize)));
    }
}
