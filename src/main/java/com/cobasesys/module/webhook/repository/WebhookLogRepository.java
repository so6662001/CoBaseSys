package com.cobasesys.module.webhook.repository;

import com.cobasesys.module.webhook.entity.WebhookLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookLogRepository extends JpaRepository<WebhookLog, Long> {

    Page<WebhookLog> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Page<WebhookLog> findByConfigIdOrderByCreatedAtDesc(Long configId, Pageable pageable);

    List<WebhookLog> findByStatusAndRetryCountLessThan(Integer status, Integer maxRetry);
}
