package com.cobasesys.module.billing.controller.admin;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.service.BillingPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "计费-套餐管理")
@RestController
@RequestMapping("/admin/billing/packages")
@RequiredArgsConstructor
public class BillingPackageAdminController {

    private final BillingPackageService packageService;

    @PostMapping
    public ApiResponse<BillingDTO.PackageResp> create(@Valid @RequestBody BillingDTO.PackageCreateReq req) {
        return ApiResponse.ok(packageService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<BillingDTO.PackageResp> update(@PathVariable Long id,
                                                        @Valid @RequestBody BillingDTO.PackageCreateReq req) {
        return ApiResponse.ok(packageService.update(id, req));
    }

    @GetMapping
    public ApiResponse<PageResult<BillingDTO.PackageResp>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(packageService.list(status, PageRequest.of(page - 1, pageSize)));
    }

    @GetMapping("/{id}")
    public ApiResponse<BillingDTO.PackageResp> getById(@PathVariable Long id) {
        return ApiResponse.ok(packageService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        packageService.delete(id); return ApiResponse.ok();
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "添加套餐项")
    public ApiResponse<Void> addItem(@PathVariable Long id, @Valid @RequestBody BillingDTO.PackageItemReq req) {
        packageService.addItem(id, req); return ApiResponse.ok();
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "移除套餐项")
    public ApiResponse<Void> removeItem(@PathVariable Long id, @PathVariable Long itemId) {
        packageService.removeItem(itemId); return ApiResponse.ok();
    }
}
