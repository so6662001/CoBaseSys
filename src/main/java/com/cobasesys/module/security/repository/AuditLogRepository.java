package com.cobasesys.module.security.repository;

import com.cobasesys.module.security.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);
    Page<AuditLog> findByOperatorIdOrderByCreatedAtDesc(String operatorId, Pageable pageable);
    Page<AuditLog> findByModuleOrderByCreatedAtDesc(String module, Pageable pageable);
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
