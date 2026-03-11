package com.cobasesys.module.billing.repository;

import com.cobasesys.module.billing.entity.BillingRenewalReminder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface BillingRenewalReminderRepository extends JpaRepository<BillingRenewalReminder, Long> {

    Page<BillingRenewalReminder> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM BillingRenewalReminder r WHERE r.subscriptionId = :subId " +
            "AND r.reminderType = :type AND r.createdAt >= :startOfDay AND r.createdAt < :endOfDay")
    long countTodayReminders(@Param("subId") Long subscriptionId, @Param("type") String reminderType,
                              @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
}
