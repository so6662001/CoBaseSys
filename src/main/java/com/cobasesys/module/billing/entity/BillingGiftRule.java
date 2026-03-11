package com.cobasesys.module.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_billing_gift_rule")
public class BillingGiftRule extends BillingBaseEntity {

    @Column(name = "rule_name", nullable = false, length = 128)
    private String ruleName;

    @Column(name = "condition_type", nullable = false, length = 20)
    private String conditionType;

    @Column(name = "condition_target_type", length = 10)
    private String conditionTargetType;

    @Column(name = "condition_target_id")
    private Long conditionTargetId;

    @Column(name = "condition_quantity", nullable = false)
    private Integer conditionQuantity = 1;

    @Column(name = "gift_type", nullable = false, length = 10)
    private String giftType;

    @Column(name = "gift_target_id")
    private Long giftTargetId;

    @Column(name = "gift_quantity", nullable = false)
    private Integer giftQuantity = 1;

    @Column(name = "gift_points")
    private Long giftPoints = 0L;

    @Column(name = "gift_validity_days")
    private Integer giftValidityDays;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
