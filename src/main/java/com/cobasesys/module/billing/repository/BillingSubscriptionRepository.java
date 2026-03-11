package com.cobasesys.module.billing.repository;

import com.cobasesys.module.billing.entity.BillingSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillingSubscriptionRepository extends JpaRepository<BillingSubscription, Long> {

    Optional<BillingSubscription> findBySubscriptionNo(String subscriptionNo);

    Page<BillingSubscription> findByTenantIdAndCustomerIdOrderByCreatedAtDesc(
            Long tenantId, String customerId, Pageable pageable);

    Page<BillingSubscription> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Page<BillingSubscription> findByTenantIdAndStatus(Long tenantId, String status, Pageable pageable);

    @Query("SELECT s FROM BillingSubscription s WHERE s.tenantId = :tenantId " +
            "AND s.status IN ('ACTIVE','TRIAL') AND s.endDate <= :deadline")
    List<BillingSubscription> findExpiringSoon(@Param("tenantId") Long tenantId,
                                                @Param("deadline") LocalDate deadline);

    @Query("SELECT s FROM BillingSubscription s WHERE s.tenantId = :tenantId " +
            "AND s.status IN ('ACTIVE','TRIAL') AND s.endDate < :today")
    List<BillingSubscription> findExpired(@Param("tenantId") Long tenantId,
                                           @Param("today") LocalDate today);

    List<BillingSubscription> findByTenantIdAndCustomerIdAndSourceTypeAndSourceIdAndStatusIn(
            Long tenantId, String customerId, String sourceType, Long sourceId, List<String> statuses);

    @Query("SELECT s FROM BillingSubscription s WHERE s.tenantId = :tenantId " +
            "AND s.customerId = :customerId AND s.status IN ('ACTIVE','EXPIRING','TRIAL') " +
            "AND s.endDate <= :deadline")
    List<BillingSubscription> findCustomerExpiringAlerts(@Param("tenantId") Long tenantId,
                                                          @Param("customerId") String customerId,
                                                          @Param("deadline") LocalDate deadline);

    @Modifying
    @Query("UPDATE BillingSubscription s SET s.daysUsed = DATEDIFF(CURRENT_DATE, s.startDate) " +
            "WHERE s.status IN ('ACTIVE','TRIAL','EXPIRING')")
    int updateDaysUsedBatch();
}
