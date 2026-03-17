package com.cobasesys.module.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_billing_renewal_reminder")
public class BillingRenewalReminder extends BillingBaseEntity {

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "reminder_type", nullable = false, length = 20)
    private String reminderType;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "recipient", length = 128)
    private String recipient;

    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
