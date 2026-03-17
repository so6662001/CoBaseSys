package com.cobasesys.module.billing.repository;

import com.cobasesys.module.billing.entity.BillingProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingProductRepository extends JpaRepository<BillingProduct, Long> {
    Page<BillingProduct> findByTenantIdAndStatus(Long tenantId, Integer status, Pageable pageable);
    Page<BillingProduct> findByTenantId(Long tenantId, Pageable pageable);
    Optional<BillingProduct> findByTenantIdAndProductCode(Long tenantId, String productCode);
    boolean existsByTenantIdAndProductCode(Long tenantId, String productCode);
}
