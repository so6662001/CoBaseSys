package com.cobasesys.module.webhook.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_webhook_config")
public class WebhookConfig extends TenantEntity {

    @Column(name = "system_id")
    private Long systemId;

    @Column(name = "webhook_name", nullable = false, length = 128)
    private String webhookName;

    @Column(name = "url", nullable = false, length = 512)
    private String url;

    @Column(name = "secret", length = 128)
    private String secret;

    /** 订阅的事件类型（逗号分隔）：point.earned, point.deducted, wallet.recharged, wallet.consumed, member.upgraded */
    @Column(name = "events", columnDefinition = "TEXT", nullable = false)
    private String events;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
