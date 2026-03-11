package com.cobasesys.module.billing.controller.api;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.service.BillingSubscriptionService;
import com.cobasesys.module.billing.service.BillingUsageLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "计费-外部系统查询")
@RestController
@RequestMapping("/api/v1/billing/check")
@RequiredArgsConstructor
public class BillingCheckController {

    private final BillingSubscriptionService subscriptionService;
    private final BillingUsageLedgerService usageLedgerService;

    @PostMapping("/products")
    @Operation(summary = "批量查询产品到期状态")
    public ApiResponse<List<BillingDTO.ProductCheckResp>> checkProducts(
            @Valid @RequestBody BillingDTO.ProductCheckReq req) {
        return ApiResponse.ok(subscriptionService.checkProducts(TenantContext.requireTenantId(), req));
    }

    @GetMapping("/subscription/{subscriptionNo}")
    @Operation(summary = "查询订阅详情")
    public ApiResponse<BillingDTO.SubscriptionResp> checkSubscription(@PathVariable String subscriptionNo) {
        // would need a findBySubscriptionNo method returning resp
        return ApiResponse.ok(null);
    }

    @PostMapping("/usage/report")
    @Operation(summary = "上报使用量")
    public ApiResponse<BillingDTO.UsageLedgerResp> reportUsage(
            @Valid @RequestBody BillingDTO.UsageReportReq req) {
        return ApiResponse.ok(usageLedgerService.reportUsage(req));
    }
}
