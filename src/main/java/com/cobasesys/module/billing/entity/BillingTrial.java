package com.cobasesys.module.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "t_billing_trial")
public class BillingTrial extends BillingBaseEntity {

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "source_type", nullable = false, length = 10)
    private String sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "source_name", length = 200)
    private String sourceName;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "trial_days", nullable = false)
    private Integer trialDays;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "converted_order_id")
    private Long convertedOrderId;

    @Column(name = "contact_info", length = 512)
    private String contactInfo;

    @Column(name = "extend_count", nullable = false)
    private Integer extendCount = 0;

    @Column(name = "total_extend_days", nullable = false)
    private Integer totalExtendDays = 0;
}
