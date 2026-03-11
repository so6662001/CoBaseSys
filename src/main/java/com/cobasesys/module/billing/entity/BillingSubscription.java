package com.cobasesys.module.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "t_billing_subscription")
public class BillingSubscription extends BillingBaseEntity {

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "subscription_no", nullable = false, unique = true, length = 64)
    private String subscriptionNo;

    @Column(name = "source_type", nullable = false, length = 10)
    private String sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "source_name", length = 200)
    private String sourceName;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "pricing_model", length = 30)
    private String pricingModel;

    @Column(name = "pricing_plan_id")
    private Long pricingPlanId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "usage_quota", nullable = false)
    private Long usageQuota = 0L;

    @Column(name = "usage_used", nullable = false)
    private Long usageUsed = 0L;

    @Column(name = "usage_unit", length = 32)
    private String usageUnit;

    @Column(name = "space_total", nullable = false)
    private Long spaceTotal = 0L;

    @Column(name = "space_used", nullable = false)
    private Long spaceUsed = 0L;

    @Column(name = "total_days", nullable = false)
    private Integer totalDays = 0;

    @Column(name = "days_used", nullable = false)
    private Integer daysUsed = 0;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "is_trial", nullable = false)
    private Integer isTrial = 0;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "auto_renew", nullable = false)
    private Integer autoRenew = 0;

    @Column(name = "original_unit_price")
    private Long originalUnitPrice;

    @Column(name = "renewal_price")
    private Long renewalPrice;

    @Column(name = "price_locked_until")
    private LocalDate priceLockedUntil;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
}
