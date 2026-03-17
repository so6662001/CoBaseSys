package com.cobasesys.module.billing.repository;

import com.cobasesys.module.billing.entity.BillingOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingOrderRepository extends JpaRepository<BillingOrder, Long> {
    Optional<BillingOrder> findByOrderNo(String orderNo);
    Page<BillingOrder> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);
    Page<BillingOrder> findByTenantIdAndCustomerIdOrderByCreatedAtDesc(Long tenantId, String customerId, Pageable pageable);
    Page<BillingOrder> findByTenantIdAndPaymentStatus(Long tenantId, Integer paymentStatus, Pageable pageable);

    Page<BillingOrder> findByTenantIdAndCustomerIdAndPaymentStatusAndInvoiceStatusOrderByCreatedAtDesc(
            Long tenantId, String customerId, Integer paymentStatus, Integer invoiceStatus, Pageable pageable);
}
