package com.cobasesys.module.points.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_point_rule")
public class PointRule extends TenantEntity {

    @Column(name = "action_id", nullable = false)
    private Long actionId;

    @Column(name = "rule_name", nullable = false, length = 128)
    private String ruleName;

    /** fixed / rate / tiered / custom */
    @Column(name = "calc_type", nullable = false, length = 20)
    private String calcType;

    @Column(name = "calc_value", precision = 12, scale = 2)
    private BigDecimal calcValue;

    @Column(name = "calc_expression", columnDefinition = "TEXT")
    private String calcExpression;

    @Column(name = "min_points")
    private Integer minPoints;

    @Column(name = "max_points")
    private Integer maxPoints;

    @Column(name = "daily_limit")
    private Integer dailyLimit;

    @Column(name = "monthly_limit")
    private Integer monthlyLimit;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
