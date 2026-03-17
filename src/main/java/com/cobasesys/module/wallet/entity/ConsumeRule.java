package com.cobasesys.module.wallet.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_consume_rule")
public class ConsumeRule extends TenantEntity {

    @Column(name = "action_id", nullable = false)
    private Long actionId;

    @Column(name = "rule_name", nullable = false, length = 128)
    private String ruleName;

    /** fixed / unit_price / tiered / custom */
    @Column(name = "calc_type", nullable = false, length = 20)
    private String calcType;

    /** 单价或固定金额（单位：元） */
    @Column(name = "calc_value", precision = 12, scale = 4)
    private BigDecimal calcValue;

    @Column(name = "calc_expression", columnDefinition = "TEXT")
    private String calcExpression;

    @Column(name = "unit_name", length = 32)
    private String unitName;

    /** 最低收费（分） */
    @Column(name = "min_charge")
    private Integer minCharge;

    /** 最高收费（分） */
    @Column(name = "max_charge")
    private Integer maxCharge;

    @Column(name = "free_quota", nullable = false)
    private Integer freeQuota = 0;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
