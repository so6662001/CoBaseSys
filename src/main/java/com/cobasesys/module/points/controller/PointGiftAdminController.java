package com.cobasesys.module.points.controller;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.module.points.dto.PointDTO;
import com.cobasesys.module.points.service.PointGiftService;
import com.cobasesys.module.security.annotation.Auditable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "积分赠送管理")
@RestController
@RequestMapping("/admin/points/gift")
@RequiredArgsConstructor
public class PointGiftAdminController {

    private final PointGiftService giftService;

    @PostMapping("/apply")
    @Operation(summary = "提交赠送积分申请")
    @Auditable(module = "points", action = "gift_apply", description = "提交赠送积分申请")
    public ApiResponse<PointDTO.GiftApprovalResponse> apply(@Valid @RequestBody PointDTO.GiftApplyRequest req) {
        return ApiResponse.ok(giftService.submitGiftApply(req));
    }

    @GetMapping("/pending")
    @Operation(summary = "待审批列表")
    public ApiResponse<PageResult<PointDTO.GiftApprovalResponse>> pending(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(giftService.list("PENDING", PageRequest.of(page - 1, pageSize)));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    @Auditable(module = "points", action = "gift_approve", description = "审批通过积分赠送")
    public ApiResponse<PointDTO.GiftApprovalResponse> approve(
            @PathVariable Long id,
            @RequestParam String approverId,
            @RequestParam String approverName,
            @RequestParam(defaultValue = "") String remark) {
        return ApiResponse.ok(giftService.approve(id, approverId, approverName, remark));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批驳回")
    @Auditable(module = "points", action = "gift_reject", description = "驳回积分赠送申请")
    public ApiResponse<PointDTO.GiftApprovalResponse> reject(
            @PathVariable Long id,
            @RequestParam String approverId,
            @RequestParam String approverName,
            @RequestParam String remark) {
        return ApiResponse.ok(giftService.reject(id, approverId, approverName, remark));
    }

    @GetMapping("/list")
    @Operation(summary = "全部审批记录")
    public ApiResponse<PageResult<PointDTO.GiftApprovalResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(giftService.list(status, PageRequest.of(page - 1, pageSize)));
    }

    @GetMapping("/summary")
    @Operation(summary = "赠送统计报表")
    public ApiResponse<PointDTO.GiftSummary> summary() {
        return ApiResponse.ok(giftService.getSummary());
    }

    @GetMapping("/transactions")
    @Operation(summary = "赠送积分流水")
    public ApiResponse<PageResult<PointDTO.TransactionResponse>> giftTransactions(
            @RequestParam(required = false) String sourceType,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(giftService.listGiftTransactions(sourceType, PageRequest.of(page - 1, pageSize)));
    }
}
