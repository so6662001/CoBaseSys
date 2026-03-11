package com.cobasesys.module.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_billing_pricing_plan")
public class BillingPricingPlan extends BillingBaseEntity {

    @Column(name = "target_type", nullable = false, length = 10)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "plan_name", nullable = false, length = 128)
    private String planName;

    @Column(name = "pricing_model", nullable = false, length = 30)
    private String pricingModel;

    @Column(name = "software_fee")
    private Long softwareFee = 0L;

    @Column(name = "annual_service_fee")
    private Long annualServiceFee = 0L;

    @Column(name = "period_type", length = 10)
    private String periodType;

    @Column(name = "period_price")
    private Long periodPrice = 0L;

    @Column(name = "included_quantity")
    private Integer includedQuantity = 0;

    @Column(name = "overage_unit_name", length = 32)
    private String overageUnitName;

    @Column(name = "overage_unit_price")
    private Long overageUnitPrice = 0L;

    @Column(name = "unit_name", length = 32)
    private String unitName;

    @Column(name = "unit_price")
    private Long unitPrice = 0L;

    @Column(name = "tiered_pricing", columnDefinition = "JSON")
    private String tieredPricing;

    @Column(name = "rental_period_type", length = 10)
    private String rentalPeriodType;

    @Column(name = "rental_price")
    private Long rentalPrice = 0L;

    @Column(name = "space_unit", length = 10)
    private String spaceUnit;

    @Column(name = "space_unit_price")
    private Long spaceUnitPrice = 0L;

    @Column(name = "one_time_price")
    private Long oneTimePrice = 0L;

    @Column(name = "validity_days")
    private Integer validityDays;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
