package com.cobasesys.module.points.repository;

import com.cobasesys.module.points.entity.PointGiftApproval;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointGiftApprovalRepository extends JpaRepository<PointGiftApproval, Long> {

    Page<PointGiftApproval> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, String status, Pageable pageable);

    Page<PointGiftApproval> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Page<PointGiftApproval> findByTenantIdAndApplicantIdOrderByCreatedAtDesc(Long tenantId, String applicantId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(a.points), 0) FROM PointGiftApproval a WHERE a.tenantId = :tenantId AND a.status = 'APPROVED'")
    Long sumApprovedPoints(@Param("tenantId") Long tenantId);

    @Query("SELECT COALESCE(SUM(a.points), 0) FROM PointGiftApproval a WHERE a.tenantId = :tenantId AND a.status = 'APPROVED' AND a.sourceType = :sourceType")
    Long sumApprovedPointsBySourceType(@Param("tenantId") Long tenantId, @Param("sourceType") String sourceType);

    long countByTenantIdAndStatus(Long tenantId, String status);
}
