package com.cobasesys.module.wallet.controller;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.wallet.dto.WalletDTO;
import com.cobasesys.module.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "钱包管理后台")
@RestController
@RequestMapping("/admin/wallet")
@RequiredArgsConstructor
public class WalletAdminController {

    private final WalletService walletService;

    // ===== Actions =====

    @Operation(summary = "创建消费动作")
    @PostMapping("/actions")
    public ApiResponse<WalletDTO.ActionResponse> createAction(
            @Valid @RequestBody WalletDTO.ActionCreateRequest request) {
        return ApiResponse.ok(walletService.createAction(request));
    }

    @Operation(summary = "更新消费动作")
    @PutMapping("/actions/{id}")
    public ApiResponse<WalletDTO.ActionResponse> updateAction(
            @PathVariable Long id, @Valid @RequestBody WalletDTO.ActionUpdateRequest request) {
        return ApiResponse.ok(walletService.updateAction(id, request));
    }

    @Operation(summary = "消费动作列表")
    @GetMapping("/actions")
    public ApiResponse<PageResult<WalletDTO.ActionResponse>> listActions(
            @RequestParam(required = false) Long systemId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(walletService.listActions(systemId, PageRequest.of(page - 1, pageSize)));
    }

    @Operation(summary = "删除消费动作")
    @DeleteMapping("/actions/{id}")
    public ApiResponse<Void> deleteAction(@PathVariable Long id) {
        walletService.deleteAction(id);
        return ApiResponse.ok();
    }

    // ===== Rules =====

    @Operation(summary = "创建消费规则")
    @PostMapping("/rules")
    public ApiResponse<WalletDTO.RuleResponse> createRule(
            @Valid @RequestBody WalletDTO.RuleCreateRequest request) {
        return ApiResponse.ok(walletService.createRule(request));
    }

    @Operation(summary = "更新消费规则")
    @PutMapping("/rules/{id}")
    public ApiResponse<WalletDTO.RuleResponse> updateRule(
            @PathVariable Long id, @Valid @RequestBody WalletDTO.RuleUpdateRequest request) {
        return ApiResponse.ok(walletService.updateRule(id, request));
    }

    @Operation(summary = "消费规则列表")
    @GetMapping("/rules")
    public ApiResponse<PageResult<WalletDTO.RuleResponse>> listRules(
            @RequestParam(required = false) Long actionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(walletService.listRules(actionId, PageRequest.of(page - 1, pageSize)));
    }

    @Operation(summary = "删除消费规则")
    @DeleteMapping("/rules/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        walletService.deleteRule(id);
        return ApiResponse.ok();
    }

    // ===== Promotions =====

    @Operation(summary = "创建充值促销")
    @PostMapping("/promotions")
    public ApiResponse<WalletDTO.PromotionResponse> createPromotion(
            @Valid @RequestBody WalletDTO.PromotionCreateRequest request) {
        return ApiResponse.ok(walletService.createPromotion(request));
    }

    @Operation(summary = "充值促销列表")
    @GetMapping("/promotions")
    public ApiResponse<PageResult<WalletDTO.PromotionResponse>> listPromotions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(walletService.listPromotions(PageRequest.of(page - 1, pageSize)));
    }

    @Operation(summary = "删除充值促销")
    @DeleteMapping("/promotions/{id}")
    public ApiResponse<Void> deletePromotion(@PathVariable Long id) {
        walletService.deletePromotion(id);
        return ApiResponse.ok();
    }

    // ===== Accounts & Transactions =====

    @Operation(summary = "钱包账户列表")
    @GetMapping("/accounts")
    public ApiResponse<PageResult<WalletDTO.BalanceResponse>> listAccounts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(walletService.listAccounts(PageRequest.of(page - 1, pageSize)));
    }

    @Operation(summary = "钱包流水列表")
    @GetMapping("/transactions")
    public ApiResponse<PageResult<WalletDTO.TransactionResponse>> listTransactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(walletService.listAllTransactions(PageRequest.of(page - 1, pageSize)));
    }

    // ===== Manual Adjust =====

    @Operation(summary = "后台手动调账")
    @PostMapping("/adjust")
    public ApiResponse<WalletDTO.TransactionResult> adjust(
            @RequestParam String userId,
            @RequestParam long amount,
            @RequestParam(defaultValue = "") String remark) {
        return ApiResponse.ok(walletService.adminAdjust(
                TenantContext.requireTenantId(), userId, amount, remark));
    }
}
