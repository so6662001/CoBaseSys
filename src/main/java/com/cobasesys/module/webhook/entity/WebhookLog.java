package com.cobasesys.module.webhook.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_webhook_log", indexes = {
        @Index(columnList = "tenant_id, created_at"),
        @Index(columnList = "config_id, created_at")
})
public class WebhookLog extends TenantEntity {

    @Column(name = "config_id", nullable = false)
    private Long configId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    /** 0:待发送 1:成功 2:失败 */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;
}
