package com.cobasesys.module.billing.repository;

import com.cobasesys.module.billing.entity.BillingPricingPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BillingPricingPlanRepository extends JpaRepository<BillingPricingPlan, Long> {
    @Query("SELECT p FROM BillingPricingPlan p WHERE p.targetType = :targetType AND p.targetId = :targetId " +
            "AND p.status = 1 AND p.effectiveFrom <= :now AND (p.effectiveTo IS NULL OR p.effectiveTo >= :now) " +
            "ORDER BY p.priority DESC")
    List<BillingPricingPlan> findActivePlans(@Param("targetType") String targetType,
                                              @Param("targetId") Long targetId,
                                              @Param("now") LocalDateTime now);

    Page<BillingPricingPlan> findByTenantId(Long tenantId, Pageable pageable);
    Page<BillingPricingPlan> findByTargetTypeAndTargetId(String targetType, Long targetId, Pageable pageable);
}
