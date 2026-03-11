package com.cobasesys.module.billing.repository;

import com.cobasesys.module.billing.entity.BillingUsageLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingUsageLedgerRepository extends JpaRepository<BillingUsageLedger, Long> {

    Page<BillingUsageLedger> findBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId, Pageable pageable);

    Page<BillingUsageLedger> findByTenantIdAndCustomerIdOrderByCreatedAtDesc(
            Long tenantId, String customerId, Pageable pageable);

    Optional<BillingUsageLedger> findByIdempotentKey(String idempotentKey);

    Optional<BillingUsageLedger> findTopBySubscriptionIdOrderByIdDesc(Long subscriptionId);
}
