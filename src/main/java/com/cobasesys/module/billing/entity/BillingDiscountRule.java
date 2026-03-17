package com.cobasesys.module.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_billing_discount_rule")
public class BillingDiscountRule extends BillingBaseEntity {

    @Column(name = "rule_name", nullable = false, length = 128)
    private String ruleName;

    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType;

    @Column(name = "target_type", length = 10)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity = 1;

    /** 最低购买周期数(PERIOD_DISCOUNT用) */
    @Column(name = "min_period_count")
    private Integer minPeriodCount;

    @Column(name = "discount_rate", precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Column(name = "threshold_amount")
    private Long thresholdAmount = 0L;

    @Column(name = "bonus_points")
    private Long bonusPoints = 0L;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
