package com.cobasesys.module.security.service;

import com.cobasesys.common.tenant.TenantContext;
import com.cobasesys.module.security.annotation.Auditable;
import com.cobasesys.module.security.entity.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        HttpServletRequest request = null;
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) request = attrs.getRequest();
        } catch (Exception ignored) {}

        String operatorId = "system";
        String operatorName = "system";
        String operatorIp = "";
        if (request != null) {
            Object uid = request.getAttribute("adminUserId");
            Object uname = request.getAttribute("adminUsername");
            operatorId = uid != null ? uid.toString() : "system";
            operatorName = uname != null ? uname.toString() : "system";
            operatorIp = request.getRemoteAddr();
        }

        AuditLog auditLog = auditService.createLog(
                TenantContext.getTenantId(), "ADMIN", operatorId, operatorName, operatorIp,
                auditable.module(), auditable.action(), "", "", auditable.description()
        );

        try {
            Object result = joinPoint.proceed();
            auditLog.setStatus("SUCCESS");
            auditService.log(auditLog);
            return result;
        } catch (Exception e) {
            auditLog.setStatus("FAIL");
            auditLog.setErrorMessage(e.getMessage() != null ?
                    e.getMessage().substring(0, Math.min(e.getMessage().length(), 500)) : "Unknown error");
            auditService.log(auditLog);
            throw e;
        }
    }
}
