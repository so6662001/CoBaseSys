package com.cobasesys.module.wallet.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_consume_action", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "system_id", "action_code"})
})
public class ConsumeAction extends TenantEntity {

    @Column(name = "system_id", nullable = false)
    private Long systemId;

    @Column(name = "action_code", nullable = false, length = 64)
    private String actionCode;

    @Column(name = "action_name", nullable = false, length = 128)
    private String actionName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
