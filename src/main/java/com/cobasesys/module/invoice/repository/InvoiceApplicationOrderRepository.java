package com.cobasesys.module.invoice.repository;

import com.cobasesys.module.invoice.entity.InvoiceApplicationOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceApplicationOrderRepository extends JpaRepository<InvoiceApplicationOrder, Long> {
    List<InvoiceApplicationOrder> findByApplicationId(Long applicationId);
}
