package com.cobasesys.module.system.controller;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.module.system.dto.TenantDTO;
import com.cobasesys.module.system.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "租户管理")
@RestController
@RequestMapping("/admin/tenants")
@RequiredArgsConstructor
public class TenantAdminController {

    private final TenantService tenantService;

    @Operation(summary = "创建租户")
    @PostMapping
    public ApiResponse<TenantDTO.Response> create(@Valid @RequestBody TenantDTO.CreateRequest request) {
        return ApiResponse.ok(tenantService.create(request));
    }

    @Operation(summary = "更新租户")
    @PutMapping("/{id}")
    public ApiResponse<TenantDTO.Response> update(@PathVariable Long id,
                                                   @Valid @RequestBody TenantDTO.UpdateRequest request) {
        return ApiResponse.ok(tenantService.update(id, request));
    }

    @Operation(summary = "租户列表")
    @GetMapping
    public ApiResponse<PageResult<TenantDTO.Response>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(PageResult.from(tenantService.list(PageRequest.of(page - 1, pageSize))));
    }

    @Operation(summary = "租户详情")
    @GetMapping("/{id}")
    public ApiResponse<TenantDTO.Response> getById(@PathVariable Long id) {
        return ApiResponse.ok(tenantService.getById(id));
    }

    @Operation(summary = "删除租户")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tenantService.delete(id);
        return ApiResponse.ok();
    }
}
