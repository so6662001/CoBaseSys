package com.cobasesys.module.billing.repository;

import com.cobasesys.module.billing.entity.BillingDiscountRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BillingDiscountRuleRepository extends JpaRepository<BillingDiscountRule, Long> {
    @Query("SELECT r FROM BillingDiscountRule r WHERE r.tenantId = :tenantId AND r.status = 1 " +
            "AND (r.effectiveFrom IS NULL OR r.effectiveFrom <= :now) " +
            "AND (r.effectiveTo IS NULL OR r.effectiveTo >= :now) ORDER BY r.priority DESC")
    List<BillingDiscountRule> findActiveRules(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);

    Page<BillingDiscountRule> findByTenantId(Long tenantId, Pageable pageable);
}
