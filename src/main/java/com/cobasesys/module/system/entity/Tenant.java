package com.cobasesys.module.system.entity;

import com.cobasesys.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_tenant")
public class Tenant extends BaseEntity {

    @Column(name = "tenant_code", nullable = false, unique = true, length = 64)
    private String tenantCode;

    @Column(name = "tenant_name", nullable = false, length = 128)
    private String tenantName;

    @Column(name = "contact_name", length = 64)
    private String contactName;

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Column(name = "contact_email", length = 128)
    private String contactEmail;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
