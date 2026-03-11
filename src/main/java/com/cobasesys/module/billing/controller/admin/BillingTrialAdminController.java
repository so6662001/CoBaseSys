package com.cobasesys.module.billing.controller.admin;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.module.billing.dto.BillingDTO;
import com.cobasesys.module.billing.service.BillingTrialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "计费-试用与延长审批")
@RestController
@RequestMapping("/admin/billing")
@RequiredArgsConstructor
public class BillingTrialAdminController {

    private final BillingTrialService trialService;

    @GetMapping("/trials")
    @Operation(summary = "试用列表")
    public ApiResponse<PageResult<BillingDTO.TrialResp>> listTrials(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(trialService.listTrials(status, PageRequest.of(page - 1, pageSize)));
    }

    @PostMapping("/trial-extend/apply")
    @Operation(summary = "提交延长试用申请")
    public ApiResponse<BillingDTO.TrialExtendApprovalResp> applyExtend(
            @RequestParam Long trialId, @Valid @RequestBody BillingDTO.TrialExtendReq req) {
        return ApiResponse.ok(trialService.submitExtendApply(trialId, req));
    }

    @GetMapping("/trial-extend/pending")
    @Operation(summary = "待审批列表")
    public ApiResponse<PageResult<BillingDTO.TrialExtendApprovalResp>> pendingApprovals(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(trialService.listApprovals("PENDING", PageRequest.of(page - 1, pageSize)));
    }

    @PostMapping("/trial-extend/{id}/approve")
    @Operation(summary = "审批通过")
    public ApiResponse<BillingDTO.TrialExtendApprovalResp> approve(
            @PathVariable Long id, @RequestParam String approverId,
            @RequestParam String approverName, @RequestParam(defaultValue = "") String remark) {
        return ApiResponse.ok(trialService.approve(id, approverId, approverName, remark));
    }

    @PostMapping("/trial-extend/{id}/reject")
    @Operation(summary = "审批驳回")
    public ApiResponse<BillingDTO.TrialExtendApprovalResp> reject(
            @PathVariable Long id, @RequestParam String approverId,
            @RequestParam String approverName, @RequestParam String remark) {
        return ApiResponse.ok(trialService.reject(id, approverId, approverName, remark));
    }

    @GetMapping("/trial-extend")
    @Operation(summary = "全部审批记录")
    public ApiResponse<PageResult<BillingDTO.TrialExtendApprovalResp>> allApprovals(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(trialService.listApprovals(status, PageRequest.of(page - 1, pageSize)));
    }
}
