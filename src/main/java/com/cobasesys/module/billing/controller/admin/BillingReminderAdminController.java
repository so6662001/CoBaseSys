package com.cobasesys.module.billing.controller.admin;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.billing.entity.BillingRenewalReminder;
import com.cobasesys.module.billing.repository.BillingRenewalReminderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "计费-提醒记录")
@RestController
@RequestMapping("/admin/billing/reminders")
@RequiredArgsConstructor
public class BillingReminderAdminController {

    private final BillingRenewalReminderRepository reminderRepository;

    @GetMapping
    @Operation(summary = "提醒记录列表")
    public ApiResponse<PageResult<BillingRenewalReminder>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        var p = reminderRepository.findByTenantIdOrderByCreatedAtDesc(
                TenantContext.requireTenantId(), PageRequest.of(page - 1, pageSize));
        return ApiResponse.ok(PageResult.from(p));
    }
}
