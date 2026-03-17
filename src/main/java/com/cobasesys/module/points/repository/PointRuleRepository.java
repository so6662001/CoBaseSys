package com.cobasesys.module.points.repository;

import com.cobasesys.module.points.entity.PointRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PointRuleRepository extends JpaRepository<PointRule, Long> {

    @Query("SELECT r FROM PointRule r WHERE r.actionId = :actionId AND r.status = 1 " +
            "AND r.effectiveFrom <= :now AND (r.effectiveTo IS NULL OR r.effectiveTo >= :now) " +
            "ORDER BY r.priority DESC")
    List<PointRule> findActiveRules(@Param("actionId") Long actionId, @Param("now") LocalDateTime now);

    Page<PointRule> findByTenantId(Long tenantId, Pageable pageable);

    Page<PointRule> findByActionId(Long actionId, Pageable pageable);
}
