package com.cobasesys.module.points.controller;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.points.dto.PointDTO;
import com.cobasesys.module.points.service.PointService;
import com.cobasesys.module.system.entity.ExternalSystem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "积分开放API")
@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointApiController {

    private final PointService pointService;

    @Operation(summary = "增加积分")
    @PostMapping("/earn")
    public ApiResponse<PointDTO.TransactionResult> earn(
            HttpServletRequest request,
            @Valid @RequestBody PointDTO.EarnRequest body) {
        ExternalSystem system = (ExternalSystem) request.getAttribute("currentSystem");
        return ApiResponse.ok(pointService.earn(system, body));
    }

    @Operation(summary = "扣减积分")
    @PostMapping("/deduct")
    public ApiResponse<PointDTO.TransactionResult> deduct(
            HttpServletRequest request,
            @Valid @RequestBody PointDTO.DeductRequest body) {
        ExternalSystem system = (ExternalSystem) request.getAttribute("currentSystem");
        return ApiResponse.ok(pointService.deduct(system, body));
    }

    @Operation(summary = "冻结积分")
    @PostMapping("/freeze")
    public ApiResponse<PointDTO.TransactionResult> freeze(
            HttpServletRequest request,
            @Valid @RequestBody PointDTO.FreezeRequest body) {
        ExternalSystem system = (ExternalSystem) request.getAttribute("currentSystem");
        return ApiResponse.ok(pointService.freeze(system, body));
    }

    @Operation(summary = "查询积分余额")
    @GetMapping("/balance/{userId}")
    public ApiResponse<PointDTO.BalanceResponse> balance(@PathVariable String userId) {
        return ApiResponse.ok(pointService.getBalance(TenantContext.requireTenantId(), userId));
    }

    @Operation(summary = "查询积分流水")
    @GetMapping("/transactions/{userId}")
    public ApiResponse<PageResult<PointDTO.TransactionResponse>> transactions(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(pointService.getTransactions(
                TenantContext.requireTenantId(), userId, page, pageSize));
    }
}
