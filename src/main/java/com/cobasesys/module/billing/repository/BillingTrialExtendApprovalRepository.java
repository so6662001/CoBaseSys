package com.cobasesys.module.billing.repository;

import com.cobasesys.module.billing.entity.BillingTrialExtendApproval;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingTrialExtendApprovalRepository extends JpaRepository<BillingTrialExtendApproval, Long> {

    Page<BillingTrialExtendApproval> findByTenantIdAndStatus(Long tenantId, String status, Pageable pageable);

    Page<BillingTrialExtendApproval> findByTenantIdAndApplicantIdOrderByCreatedAtDesc(
            Long tenantId, String applicantId, Pageable pageable);

    Page<BillingTrialExtendApproval> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);
}
