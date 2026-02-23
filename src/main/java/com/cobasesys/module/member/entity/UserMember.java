package com.cobasesys.module.member.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_user_member", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "user_id"})
})
public class UserMember extends TenantEntity {

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "level_id", nullable = false)
    private Long levelId;

    @Column(name = "total_points_earned", nullable = false)
    private Long totalPointsEarned = 0L;

    @Column(name = "total_consumption", nullable = false)
    private Long totalConsumption = 0L;

    @Column(name = "level_updated_at")
    private LocalDateTime levelUpdatedAt;
}
