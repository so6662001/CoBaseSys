package com.cobasesys.module.points.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_point_action", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "system_id", "action_code"})
})
public class PointAction extends TenantEntity {

    @Column(name = "system_id", nullable = false)
    private Long systemId;

    @Column(name = "action_code", nullable = false, length = 64)
    private String actionCode;

    @Column(name = "action_name", nullable = false, length = 128)
    private String actionName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 1=增加, -1=扣减 */
    @Column(name = "direction", nullable = false)
    private Integer direction;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
