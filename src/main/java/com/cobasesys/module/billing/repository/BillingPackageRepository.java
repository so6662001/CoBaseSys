package com.cobasesys.module.billing.repository;

import com.cobasesys.module.billing.entity.BillingPackage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingPackageRepository extends JpaRepository<BillingPackage, Long> {
    Page<BillingPackage> findByTenantIdAndStatus(Long tenantId, Integer status, Pageable pageable);
    Page<BillingPackage> findByTenantId(Long tenantId, Pageable pageable);
    Optional<BillingPackage> findByTenantIdAndPackageCode(Long tenantId, String packageCode);
}
