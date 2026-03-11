package com.cobasesys.module.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_billing_trial_extend_approval")
public class BillingTrialExtendApproval extends BillingBaseEntity {

    @Column(name = "trial_id", nullable = false)
    private Long trialId;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "customer_name", length = 128)
    private String customerName;

    @Column(name = "source_name", length = 200)
    private String sourceName;

    @Column(name = "extend_days", nullable = false)
    private Integer extendDays;

    @Column(name = "apply_reason", length = 512)
    private String applyReason;

    @Column(name = "applicant_id", nullable = false, length = 64)
    private String applicantId;

    @Column(name = "applicant_name", length = 128)
    private String applicantName;

    @Column(name = "apply_time", nullable = false)
    private LocalDateTime applyTime;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "approver_id", length = 64)
    private String approverId;

    @Column(name = "approver_name", length = 128)
    private String approverName;

    @Column(name = "approve_time")
    private LocalDateTime approveTime;

    @Column(name = "approve_remark", length = 512)
    private String approveRemark;
}
