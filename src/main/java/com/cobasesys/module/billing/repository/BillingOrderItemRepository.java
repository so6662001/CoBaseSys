package com.cobasesys.module.billing.repository;

import com.cobasesys.module.billing.entity.BillingOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingOrderItemRepository extends JpaRepository<BillingOrderItem, Long> {
    List<BillingOrderItem> findByOrderId(Long orderId);
}
