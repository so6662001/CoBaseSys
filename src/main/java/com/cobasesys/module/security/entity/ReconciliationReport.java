package com.cobasesys.module.security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_reconciliation_report")
public class ReconciliationReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "check_type", nullable = false, length = 30)
    private String checkType;

    @Column(name = "target_table", length = 64)
    private String targetTable;

    @Column(name = "total_records")
    private Long totalRecords;

    @Column(name = "pass_count")
    private Long passCount;

    @Column(name = "fail_count")
    private Long failCount;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "fail_details", columnDefinition = "TEXT")
    private String failDetails;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
