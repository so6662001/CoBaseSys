package com.cobasesys.module.notification.repository;

import com.cobasesys.module.notification.entity.NotificationTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByTenantIdAndTemplateCode(Long tenantId, String templateCode);

    Page<NotificationTemplate> findByTenantId(Long tenantId, Pageable pageable);
}
