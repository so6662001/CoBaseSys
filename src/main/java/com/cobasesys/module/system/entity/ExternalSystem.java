package com.cobasesys.module.system.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_external_system", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "system_code"}),
        @UniqueConstraint(columnNames = "app_key")
})
public class ExternalSystem extends TenantEntity {

    @Column(name = "system_code", nullable = false, length = 64)
    private String systemCode;

    @Column(name = "system_name", nullable = false, length = 128)
    private String systemName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "app_key", nullable = false, unique = true, length = 64)
    private String appKey;

    @Column(name = "app_secret", nullable = false, length = 128)
    private String appSecret;

    @Column(name = "callback_url", length = 512)
    private String callbackUrl;

    @Column(name = "ip_whitelist", columnDefinition = "TEXT")
    private String ipWhitelist;

    @Column(name = "rate_limit", nullable = false)
    private Integer rateLimit = 1000;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
