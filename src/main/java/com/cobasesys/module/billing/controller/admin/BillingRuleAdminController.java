package com.cobasesys.module.billing.controller.admin;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.entity.BillingDiscountRule;
import com.cobasesys.module.billing.entity.BillingGiftRule;
import com.cobasesys.module.billing.service.BillingRuleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "计费-折扣与赠送规则")
@RestController
@RequestMapping("/admin/billing")
@RequiredArgsConstructor
public class BillingRuleAdminController {

    private final BillingRuleService ruleService;

    @PostMapping("/discount-rules")
    public ApiResponse<BillingDiscountRule> createDiscount(@Valid @RequestBody BillingDTO.DiscountRuleReq req) {
        return ApiResponse.ok(ruleService.createDiscount(req));
    }

    @PutMapping("/discount-rules/{id}")
    public ApiResponse<BillingDiscountRule> updateDiscount(@PathVariable Long id,
                                                            @Valid @RequestBody BillingDTO.DiscountRuleReq req) {
        return ApiResponse.ok(ruleService.updateDiscount(id, req));
    }

    @GetMapping("/discount-rules")
    public ApiResponse<PageResult<BillingDiscountRule>> listDiscounts(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(ruleService.listDiscounts(PageRequest.of(page - 1, pageSize)));
    }

    @DeleteMapping("/discount-rules/{id}")
    public ApiResponse<Void> deleteDiscount(@PathVariable Long id) {
        ruleService.deleteDiscount(id); return ApiResponse.ok();
    }

    @PostMapping("/gift-rules")
    public ApiResponse<BillingGiftRule> createGift(@Valid @RequestBody BillingDTO.GiftRuleReq req) {
        return ApiResponse.ok(ruleService.createGift(req));
    }

    @PutMapping("/gift-rules/{id}")
    public ApiResponse<BillingGiftRule> updateGift(@PathVariable Long id,
                                                     @Valid @RequestBody BillingDTO.GiftRuleReq req) {
        return ApiResponse.ok(ruleService.updateGift(id, req));
    }

    @GetMapping("/gift-rules")
    public ApiResponse<PageResult<BillingGiftRule>> listGifts(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(ruleService.listGifts(PageRequest.of(page - 1, pageSize)));
    }

    @DeleteMapping("/gift-rules/{id}")
    public ApiResponse<Void> deleteGift(@PathVariable Long id) {
        ruleService.deleteGift(id); return ApiResponse.ok();
    }
}
