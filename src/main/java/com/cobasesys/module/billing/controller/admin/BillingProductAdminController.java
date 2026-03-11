package com.cobasesys.module.billing.controller.admin;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.service.BillingProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "计费-产品管理")
@RestController
@RequestMapping("/admin/billing/products")
@RequiredArgsConstructor
public class BillingProductAdminController {

    private final BillingProductService productService;

    @PostMapping
    @Operation(summary = "创建产品")
    public ApiResponse<BillingDTO.ProductResp> create(@Valid @RequestBody BillingDTO.ProductCreateReq req) {
        return ApiResponse.ok(productService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新产品")
    public ApiResponse<BillingDTO.ProductResp> update(@PathVariable Long id,
                                                        @Valid @RequestBody BillingDTO.ProductCreateReq req) {
        return ApiResponse.ok(productService.update(id, req));
    }

    @GetMapping
    @Operation(summary = "产品列表")
    public ApiResponse<PageResult<BillingDTO.ProductResp>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(productService.list(status, PageRequest.of(page - 1, pageSize)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "产品详情")
    public ApiResponse<BillingDTO.ProductResp> getById(@PathVariable Long id) {
        return ApiResponse.ok(productService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除产品")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.ok();
    }
}
