package com.cobasesys.module.security.service;

import com.cobasesys.common.model.PageResult;
import com.cobasesys.module.security.entity.AuditLog;
import com.cobasesys.module.security.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }

    public AuditLog createLog(Long tenantId, String operatorType, String operatorId, String operatorName,
                               String operatorIp, String module, String action,
                               String targetType, String targetId, String description) {
        AuditLog log = new AuditLog();
        log.setTenantId(tenantId);
        log.setTraceId(java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        log.setOperatorType(operatorType);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperatorIp(operatorIp);
        log.setModule(module);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDescription(description);
        log.setStatus("SUCCESS");
        return log;
    }

    public PageResult<AuditLog> list(String module, String operatorId, Pageable pageable) {
        if (module != null && !module.isBlank()) {
            return PageResult.from(auditLogRepository.findByModuleOrderByCreatedAtDesc(module, pageable));
        }
        if (operatorId != null && !operatorId.isBlank()) {
            return PageResult.from(auditLogRepository.findByOperatorIdOrderByCreatedAtDesc(operatorId, pageable));
        }
        return PageResult.from(auditLogRepository.findAllByOrderByCreatedAtDesc(pageable));
    }
}
