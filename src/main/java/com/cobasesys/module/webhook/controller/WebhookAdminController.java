package com.cobasesys.module.webhook.controller;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.module.webhook.dto.WebhookDTO;
import com.cobasesys.module.webhook.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Webhook管理")
@RestController
@RequestMapping("/admin/webhooks")
@RequiredArgsConstructor
public class WebhookAdminController {

    private final WebhookService webhookService;

    @Operation(summary = "创建Webhook")
    @PostMapping
    public ApiResponse<WebhookDTO.ConfigResponse> create(
            @Valid @RequestBody WebhookDTO.ConfigCreateRequest request) {
        return ApiResponse.ok(webhookService.createConfig(request));
    }

    @Operation(summary = "更新Webhook")
    @PutMapping("/{id}")
    public ApiResponse<WebhookDTO.ConfigResponse> update(
            @PathVariable Long id, @Valid @RequestBody WebhookDTO.ConfigUpdateRequest request) {
        return ApiResponse.ok(webhookService.updateConfig(id, request));
    }

    @Operation(summary = "Webhook列表")
    @GetMapping
    public ApiResponse<PageResult<WebhookDTO.ConfigResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(webhookService.listConfigs(PageRequest.of(page - 1, pageSize)));
    }

    @Operation(summary = "删除Webhook")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        webhookService.deleteConfig(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "Webhook日志")
    @GetMapping("/logs")
    public ApiResponse<PageResult<WebhookDTO.LogResponse>> logs(
            @RequestParam(required = false) Long configId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(webhookService.listLogs(configId, PageRequest.of(page - 1, pageSize)));
    }
}
