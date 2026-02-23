package com.cobasesys.module.wallet.repository;

import com.cobasesys.module.wallet.entity.RechargePromotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RechargePromotionRepository extends JpaRepository<RechargePromotion, Long> {

    @Query("SELECT p FROM RechargePromotion p WHERE p.tenantId = :tenantId AND p.status = 1 " +
            "AND p.effectiveFrom <= :now AND (p.effectiveTo IS NULL OR p.effectiveTo >= :now) " +
            "AND p.minAmount <= :amount ORDER BY p.minAmount DESC")
    List<RechargePromotion> findActivePromotions(@Param("tenantId") Long tenantId,
                                                  @Param("amount") long amount,
                                                  @Param("now") LocalDateTime now);

    Page<RechargePromotion> findByTenantId(Long tenantId, Pageable pageable);
}
