package com.cobasesys.module.security.controller;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.module.security.entity.AuditLog;
import com.cobasesys.module.security.entity.ReconciliationReport;
import com.cobasesys.module.security.service.AuditService;
import com.cobasesys.module.security.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "安全-审计与对账")
@RestController
@RequestMapping("/admin/security")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final ReconciliationService reconciliationService;

    @GetMapping("/audit-logs")
    @Operation(summary = "审计日志列表")
    public ApiResponse<PageResult<AuditLog>> auditLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operatorId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(auditService.list(module, operatorId, PageRequest.of(page - 1, pageSize)));
    }

    @GetMapping("/reconciliation-reports")
    @Operation(summary = "对账报告列表")
    public ApiResponse<PageResult<ReconciliationReport>> reconciliationReports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(reconciliationService.listReports(status, PageRequest.of(page - 1, pageSize)));
    }
}
