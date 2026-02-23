package com.cobasesys.module.notification.controller;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.module.notification.dto.NotificationDTO;
import com.cobasesys.module.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "通知管理")
@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
public class NotificationAdminController {

    private final NotificationService notificationService;

    // ===== Templates =====

    @Operation(summary = "创建通知模板")
    @PostMapping("/templates")
    public ApiResponse<NotificationDTO.TemplateResponse> createTemplate(
            @Valid @RequestBody NotificationDTO.TemplateCreateRequest request) {
        return ApiResponse.ok(notificationService.createTemplate(request));
    }

    @Operation(summary = "更新通知模板")
    @PutMapping("/templates/{id}")
    public ApiResponse<NotificationDTO.TemplateResponse> updateTemplate(
            @PathVariable Long id, @Valid @RequestBody NotificationDTO.TemplateUpdateRequest request) {
        return ApiResponse.ok(notificationService.updateTemplate(id, request));
    }

    @Operation(summary = "通知模板列表")
    @GetMapping("/templates")
    public ApiResponse<PageResult<NotificationDTO.TemplateResponse>> listTemplates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(notificationService.listTemplates(PageRequest.of(page - 1, pageSize)));
    }

    // ===== Rules =====

    @Operation(summary = "创建通知规则")
    @PostMapping("/rules")
    public ApiResponse<NotificationDTO.RuleResponse> createRule(
            @Valid @RequestBody NotificationDTO.RuleCreateRequest request) {
        return ApiResponse.ok(notificationService.createRule(request));
    }

    @Operation(summary = "更新通知规则")
    @PutMapping("/rules/{id}")
    public ApiResponse<NotificationDTO.RuleResponse> updateRule(
            @PathVariable Long id, @Valid @RequestBody NotificationDTO.RuleUpdateRequest request) {
        return ApiResponse.ok(notificationService.updateRule(id, request));
    }

    @Operation(summary = "通知规则列表")
    @GetMapping("/rules")
    public ApiResponse<PageResult<NotificationDTO.RuleResponse>> listRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(notificationService.listRules(PageRequest.of(page - 1, pageSize)));
    }

    // ===== Records =====

    @Operation(summary = "通知发送记录")
    @GetMapping("/records")
    public ApiResponse<PageResult<NotificationDTO.RecordResponse>> listRecords(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(notificationService.listRecords(userId, PageRequest.of(page - 1, pageSize)));
    }
}
