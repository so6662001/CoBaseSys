package com.cobasesys.module.notification.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_notification_record", indexes = {
        @Index(columnList = "tenant_id, user_id, created_at")
})
public class NotificationRecord extends TenantEntity {

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "channel", nullable = false, length = 32)
    private String channel;

    @Column(name = "subject", length = 256)
    private String subject;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "recipient", length = 256)
    private String recipient;

    /** 0:待发送 1:已发送 2:发送失败 */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
