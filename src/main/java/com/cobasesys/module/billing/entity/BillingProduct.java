package com.cobasesys.module.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "t_billing_product")
public class BillingProduct extends BillingBaseEntity {

    @Column(name = "product_code", nullable = false, length = 64)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url", length = 512)
    private String iconUrl;

    @Column(name = "pricing_model", nullable = false, length = 30)
    private String pricingModel;

    @Column(name = "trial_enabled", nullable = false)
    private Integer trialEnabled = 0;

    @Column(name = "trial_days", nullable = false)
    private Integer trialDays = 0;

    @Column(name = "trial_extend_enabled", nullable = false)
    private Integer trialExtendEnabled = 0;

    @Column(name = "trial_max_extend_days", nullable = false)
    private Integer trialMaxExtendDays = 0;

    @Column(name = "points_payable", nullable = false)
    private Integer pointsPayable = 0;

    @Column(name = "max_points_ratio", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxPointsRatio = BigDecimal.ZERO;

    @Column(name = "points_exchange_rate", nullable = false, precision = 12, scale = 4)
    private BigDecimal pointsExchangeRate = BigDecimal.ZERO;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
