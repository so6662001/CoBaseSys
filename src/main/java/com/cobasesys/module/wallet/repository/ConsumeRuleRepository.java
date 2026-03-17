package com.cobasesys.module.wallet.repository;

import com.cobasesys.module.wallet.entity.ConsumeRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ConsumeRuleRepository extends JpaRepository<ConsumeRule, Long> {

    @Query("SELECT r FROM ConsumeRule r WHERE r.actionId = :actionId AND r.status = 1 " +
            "AND r.effectiveFrom <= :now AND (r.effectiveTo IS NULL OR r.effectiveTo >= :now) " +
            "ORDER BY r.priority DESC")
    List<ConsumeRule> findActiveRules(@Param("actionId") Long actionId, @Param("now") LocalDateTime now);

    Page<ConsumeRule> findByTenantId(Long tenantId, Pageable pageable);

    Page<ConsumeRule> findByActionId(Long actionId, Pageable pageable);
}
