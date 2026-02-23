package com.cobasesys.module.notification.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_notification_template")
public class NotificationTemplate extends TenantEntity {

    @Column(name = "template_code", nullable = false, length = 64)
    private String templateCode;

    @Column(name = "template_name", nullable = false, length = 128)
    private String templateName;

    /** email / sms / webhook / in_app */
    @Column(name = "channel", nullable = false, length = 32)
    private String channel;

    @Column(name = "subject", length = 256)
    private String subject;

    /** 模板内容，支持 ${variable} 占位符 */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
