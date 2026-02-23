package com.cobasesys.module.notification.repository;

import com.cobasesys.module.notification.entity.NotificationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long> {

    Page<NotificationRecord> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Page<NotificationRecord> findByTenantIdAndUserIdOrderByCreatedAtDesc(Long tenantId, String userId, Pageable pageable);
}
