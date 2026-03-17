package com.cobasesys.module.billing.repository;

import com.cobasesys.module.billing.entity.BillingPackageItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingPackageItemRepository extends JpaRepository<BillingPackageItem, Long> {
    List<BillingPackageItem> findByPackageIdOrderBySortOrderAsc(Long packageId);
    void deleteByPackageId(Long packageId);
}
