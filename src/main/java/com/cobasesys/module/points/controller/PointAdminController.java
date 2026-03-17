package com.cobasesys.module.points.controller;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.module.points.dto.PointDTO;
import com.cobasesys.module.points.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "积分管理后台")
@RestController
@RequestMapping("/admin/points")
@RequiredArgsConstructor
public class PointAdminController {

    private final PointService pointService;

    // ===== Actions =====

    @Operation(summary = "创建积分动作")
    @PostMapping("/actions")
    public ApiResponse<PointDTO.ActionResponse> createAction(
            @Valid @RequestBody PointDTO.ActionCreateRequest request) {
        return ApiResponse.ok(pointService.createAction(request));
    }

    @Operation(summary = "更新积分动作")
    @PutMapping("/actions/{id}")
    public ApiResponse<PointDTO.ActionResponse> updateAction(
            @PathVariable Long id, @Valid @RequestBody PointDTO.ActionUpdateRequest request) {
        return ApiResponse.ok(pointService.updateAction(id, request));
    }

    @Operation(summary = "积分动作列表")
    @GetMapping("/actions")
    public ApiResponse<PageResult<PointDTO.ActionResponse>> listActions(
            @RequestParam(required = false) Long systemId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(pointService.listActions(systemId, PageRequest.of(page - 1, pageSize)));
    }

    @Operation(summary = "删除积分动作")
    @DeleteMapping("/actions/{id}")
    public ApiResponse<Void> deleteAction(@PathVariable Long id) {
        pointService.deleteAction(id);
        return ApiResponse.ok();
    }

    // ===== Rules =====

    @Operation(summary = "创建积分规则")
    @PostMapping("/rules")
    public ApiResponse<PointDTO.RuleResponse> createRule(
            @Valid @RequestBody PointDTO.RuleCreateRequest request) {
        return ApiResponse.ok(pointService.createRule(request));
    }

    @Operation(summary = "更新积分规则")
    @PutMapping("/rules/{id}")
    public ApiResponse<PointDTO.RuleResponse> updateRule(
            @PathVariable Long id, @Valid @RequestBody PointDTO.RuleUpdateRequest request) {
        return ApiResponse.ok(pointService.updateRule(id, request));
    }

    @Operation(summary = "积分规则列表")
    @GetMapping("/rules")
    public ApiResponse<PageResult<PointDTO.RuleResponse>> listRules(
            @RequestParam(required = false) Long actionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(pointService.listRules(actionId, PageRequest.of(page - 1, pageSize)));
    }

    @Operation(summary = "删除积分规则")
    @DeleteMapping("/rules/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        pointService.deleteRule(id);
        return ApiResponse.ok();
    }

    // ===== Accounts =====

    @Operation(summary = "积分账户列表")
    @GetMapping("/accounts")
    public ApiResponse<PageResult<PointDTO.BalanceResponse>> listAccounts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(pointService.listAccounts(PageRequest.of(page - 1, pageSize)));
    }

    // ===== Transactions =====

    @Operation(summary = "积分流水列表")
    @GetMapping("/transactions")
    public ApiResponse<PageResult<PointDTO.TransactionResponse>> listTransactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(pointService.listAllTransactions(PageRequest.of(page - 1, pageSize)));
    }
}
