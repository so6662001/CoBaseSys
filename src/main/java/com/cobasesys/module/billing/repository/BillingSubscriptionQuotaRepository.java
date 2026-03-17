package com.cobasesys.module.billing.repository;

import com.cobasesys.module.billing.entity.BillingSubscriptionQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillingSubscriptionQuotaRepository extends JpaRepository<BillingSubscriptionQuota, Long> {

    List<BillingSubscriptionQuota> findBySubscriptionIdAndPeriodStartAndPeriodEnd(
            Long subscriptionId, LocalDate periodStart, LocalDate periodEnd);

    Optional<BillingSubscriptionQuota> findBySubscriptionIdAndDimensionAndPeriodStartAndPeriodEnd(
            Long subscriptionId, String dimension, LocalDate periodStart, LocalDate periodEnd);

    List<BillingSubscriptionQuota> findBySubscriptionId(Long subscriptionId);

    List<BillingSubscriptionQuota> findByTenantIdAndCustomerId(Long tenantId, String customerId);

    @Modifying
    @Query("UPDATE BillingSubscriptionQuota q SET q.quotaUsed = q.quotaUsed + :delta, " +
            "q.version = q.version + 1 WHERE q.id = :id AND q.version = :version " +
            "AND (q.quotaLimit = -1 OR q.quotaUsed + :delta <= q.quotaLimit)")
    int incrementUsage(@Param("id") Long id, @Param("delta") long delta, @Param("version") long version);
}
