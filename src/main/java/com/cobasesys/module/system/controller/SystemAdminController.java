package com.cobasesys.module.system.controller;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.module.system.dto.ExternalSystemDTO;
import com.cobasesys.module.system.service.ExternalSystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "外部系统管理")
@RestController
@RequestMapping("/admin/systems")
@RequiredArgsConstructor
public class SystemAdminController {

    private final ExternalSystemService systemService;

    @Operation(summary = "注册外部系统")
    @PostMapping
    public ApiResponse<ExternalSystemDTO.DetailResponse> create(
            @Valid @RequestBody ExternalSystemDTO.CreateRequest request) {
        return ApiResponse.ok(systemService.create(request));
    }

    @Operation(summary = "更新外部系统")
    @PutMapping("/{id}")
    public ApiResponse<ExternalSystemDTO.Response> update(@PathVariable Long id,
                                                           @Valid @RequestBody ExternalSystemDTO.UpdateRequest request) {
        return ApiResponse.ok(systemService.update(id, request));
    }

    @Operation(summary = "外部系统列表")
    @GetMapping
    public ApiResponse<PageResult<ExternalSystemDTO.Response>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(PageResult.from(systemService.list(PageRequest.of(page - 1, pageSize))));
    }

    @Operation(summary = "外部系统详情")
    @GetMapping("/{id}")
    public ApiResponse<ExternalSystemDTO.Response> getById(@PathVariable Long id) {
        return ApiResponse.ok(systemService.getById(id));
    }

    @Operation(summary = "重置AppSecret")
    @PostMapping("/{id}/reset-secret")
    public ApiResponse<ExternalSystemDTO.DetailResponse> resetSecret(@PathVariable Long id) {
        return ApiResponse.ok(systemService.resetSecret(id));
    }

    @Operation(summary = "删除外部系统")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        systemService.delete(id);
        return ApiResponse.ok();
    }
}
