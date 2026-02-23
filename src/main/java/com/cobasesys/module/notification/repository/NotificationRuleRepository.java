package com.cobasesys.module.notification.repository;

import com.cobasesys.module.notification.entity.NotificationRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRuleRepository extends JpaRepository<NotificationRule, Long> {

    List<NotificationRule> findByTenantIdAndTriggerTypeAndStatus(Long tenantId, String triggerType, Integer status);

    Page<NotificationRule> findByTenantId(Long tenantId, Pageable pageable);
}
