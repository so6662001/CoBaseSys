package com.cobasesys.module.points.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_point_gift_approval")
public class PointGiftApproval extends TenantEntity {

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "customer_name", length = 128)
    private String customerName;

    @Column(name = "points", nullable = false)
    private Long points;

    @Column(name = "gift_reason", nullable = false, length = 512)
    private String giftReason;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType = "GIFT_MANUAL";

    @Column(name = "applicant_id", nullable = false, length = 64)
    private String applicantId;

    @Column(name = "applicant_name", length = 128)
    private String applicantName;

    @Column(name = "apply_time", nullable = false)
    private LocalDateTime applyTime;

    /** PENDING / APPROVED / REJECTED */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "approver_id", length = 64)
    private String approverId;

    @Column(name = "approver_name", length = 128)
    private String approverName;

    @Column(name = "approve_time")
    private LocalDateTime approveTime;

    @Column(name = "approve_remark", length = 512)
    private String approveRemark;

    @Column(name = "transaction_no", length = 64)
    private String transactionNo;
}
