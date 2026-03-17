package com.cobasesys.module.invoice.repository;

import com.cobasesys.module.invoice.entity.InvoiceApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface InvoiceApplicationRepository extends JpaRepository<InvoiceApplication, Long> {

    Page<InvoiceApplication> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, String status, Pageable pageable);

    Page<InvoiceApplication> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Page<InvoiceApplication> findByTenantIdAndCustomerIdOrderByCreatedAtDesc(Long tenantId, String customerId, Pageable pageable);

    long countByTenantIdAndCustomerIdAndCreatedAtAfter(Long tenantId, String customerId, LocalDateTime after);

    @Query("SELECT MAX(a.createdAt) FROM InvoiceApplication a WHERE a.tenantId = :tenantId AND a.customerId = :customerId")
    LocalDateTime findLastApplyTime(@Param("tenantId") Long tenantId, @Param("customerId") String customerId);

    @Query("SELECT COALESCE(SUM(a.totalAmount), 0) FROM InvoiceApplication a WHERE a.tenantId = :tenantId AND a.status = 'ISSUED'")
    Long sumIssuedAmount(@Param("tenantId") Long tenantId);

    long countByTenantIdAndStatus(Long tenantId, String status);
}
