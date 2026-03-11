package com.cobasesys.module.security.repository;

import com.cobasesys.module.security.entity.ReconciliationReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationReportRepository extends JpaRepository<ReconciliationReport, Long> {
    Page<ReconciliationReport> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<ReconciliationReport> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
