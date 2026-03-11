package com.cobasesys.module.billing.controller.admin;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.service.BillingPricingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "计费-定价方案")
@RestController
@RequestMapping("/admin/billing/pricing-plans")
@RequiredArgsConstructor
public class BillingPricingAdminController {

    private final BillingPricingService pricingService;

    @PostMapping
    public ApiResponse<BillingDTO.PricingPlanResp> create(@Valid @RequestBody BillingDTO.PricingPlanCreateReq req) {
        return ApiResponse.ok(pricingService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<BillingDTO.PricingPlanResp> update(@PathVariable Long id,
                                                            @Valid @RequestBody BillingDTO.PricingPlanCreateReq req) {
        return ApiResponse.ok(pricingService.update(id, req));
    }

    @GetMapping
    public ApiResponse<PageResult<BillingDTO.PricingPlanResp>> list(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(pricingService.list(targetType, targetId, PageRequest.of(page - 1, pageSize)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        pricingService.delete(id); return ApiResponse.ok();
    }
}
