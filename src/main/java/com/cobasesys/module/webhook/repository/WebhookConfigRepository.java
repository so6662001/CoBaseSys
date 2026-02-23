package com.cobasesys.module.webhook.repository;

import com.cobasesys.module.webhook.entity.WebhookConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, Long> {

    List<WebhookConfig> findByTenantIdAndStatus(Long tenantId, Integer status);

    Page<WebhookConfig> findByTenantId(Long tenantId, Pageable pageable);
}
