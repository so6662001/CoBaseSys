package com.cobasesys.module.wallet.controller;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.system.entity.ExternalSystem;
import com.cobasesys.module.wallet.dto.WalletDTO;
import com.cobasesys.module.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "钱包开放API")
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletApiController {

    private final WalletService walletService;

    @Operation(summary = "创建充值订单")
    @PostMapping("/recharge")
    public ApiResponse<WalletDTO.RechargeOrderResponse> recharge(
            @Valid @RequestBody WalletDTO.RechargeRequest body) {
        return ApiResponse.ok(walletService.createRechargeOrder(TenantContext.requireTenantId(), body));
    }

    @Operation(summary = "充值回调")
    @PostMapping("/recharge/callback")
    public ApiResponse<WalletDTO.RechargeOrderResponse> rechargeCallback(
            @Valid @RequestBody WalletDTO.RechargeCallbackRequest body) {
        return ApiResponse.ok(walletService.processRechargeCallback(body));
    }

    @Operation(summary = "消费扣费")
    @PostMapping("/consume")
    public ApiResponse<WalletDTO.TransactionResult> consume(
            HttpServletRequest request,
            @Valid @RequestBody WalletDTO.ConsumeRequest body) {
        ExternalSystem system = (ExternalSystem) request.getAttribute("currentSystem");
        return ApiResponse.ok(walletService.consume(system, body));
    }

    @Operation(summary = "冻结金额")
    @PostMapping("/freeze")
    public ApiResponse<WalletDTO.TransactionResult> freeze(
            HttpServletRequest request,
            @Valid @RequestBody WalletDTO.FreezeRequest body) {
        ExternalSystem system = (ExternalSystem) request.getAttribute("currentSystem");
        return ApiResponse.ok(walletService.freezeBalance(system, body));
    }

    @Operation(summary = "解冻金额")
    @PostMapping("/unfreeze")
    public ApiResponse<WalletDTO.TransactionResult> unfreeze(
            HttpServletRequest request,
            @Valid @RequestBody WalletDTO.FreezeRequest body) {
        ExternalSystem system = (ExternalSystem) request.getAttribute("currentSystem");
        return ApiResponse.ok(walletService.unfreezeBalance(system, body));
    }

    @Operation(summary = "退款")
    @PostMapping("/refund")
    public ApiResponse<WalletDTO.TransactionResult> refund(
            HttpServletRequest request,
            @Valid @RequestBody WalletDTO.RefundRequest body) {
        ExternalSystem system = (ExternalSystem) request.getAttribute("currentSystem");
        return ApiResponse.ok(walletService.refund(system, body));
    }

    @Operation(summary = "查询充值订单")
    @GetMapping("/recharge/{orderNo}")
    public ApiResponse<WalletDTO.RechargeOrderResponse> getRechargeOrder(@PathVariable String orderNo) {
        return ApiResponse.ok(walletService.getRechargeOrder(orderNo));
    }

    @Operation(summary = "查询余额")
    @GetMapping("/balance/{userId}")
    public ApiResponse<WalletDTO.BalanceResponse> balance(@PathVariable String userId) {
        return ApiResponse.ok(walletService.getBalance(TenantContext.requireTenantId(), userId));
    }

    @Operation(summary = "查询流水")
    @GetMapping("/transactions/{userId}")
    public ApiResponse<PageResult<WalletDTO.TransactionResponse>> transactions(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(walletService.getTransactions(
                TenantContext.requireTenantId(), userId, page, pageSize));
    }

    @Operation(summary = "预检查余额")
    @PostMapping("/check-balance")
    public ApiResponse<WalletDTO.BalanceCheckResponse> checkBalance(
            HttpServletRequest request,
            @Valid @RequestBody WalletDTO.BalanceCheckRequest body) {
        ExternalSystem system = (ExternalSystem) request.getAttribute("currentSystem");
        return ApiResponse.ok(walletService.checkBalance(system, body));
    }
}
