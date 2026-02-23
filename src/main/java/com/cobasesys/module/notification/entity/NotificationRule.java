package com.cobasesys.module.notification.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_notification_rule")
public class NotificationRule extends TenantEntity {

    @Column(name = "rule_name", nullable = false, length = 128)
    private String ruleName;

    /** low_balance / point_expiry / recharge_success / level_upgrade / custom */
    @Column(name = "trigger_type", nullable = false, length = 32)
    private String triggerType;

    /** 触发阈值（如余额低于多少分时触发） */
    @Column(name = "threshold_value")
    private Long thresholdValue;

    /** email / sms / webhook / in_app */
    @Column(name = "channel", nullable = false, length = 32)
    private String channel;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
