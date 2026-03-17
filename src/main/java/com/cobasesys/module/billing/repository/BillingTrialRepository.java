package com.cobasesys.module.billing.repository;

import com.cobasesys.module.billing.entity.BillingTrial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingTrialRepository extends JpaRepository<BillingTrial, Long> {

    Optional<BillingTrial> findByTenantIdAndCustomerIdAndSourceTypeAndSourceId(
            Long tenantId, String customerId, String sourceType, Long sourceId);

    Page<BillingTrial> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Page<BillingTrial> findByTenantIdAndStatus(Long tenantId, String status, Pageable pageable);

    List<BillingTrial> findByTenantIdAndCustomerId(Long tenantId, String customerId);
}
