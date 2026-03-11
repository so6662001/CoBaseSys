package com.cobasesys.module.points.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_point_account", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "user_id"})
})
public class PointAccount extends TenantEntity {

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "total_earned", nullable = false)
    private Long totalEarned = 0L;

    @Column(name = "total_consumed", nullable = false)
    private Long totalConsumed = 0L;

    @Column(name = "balance", nullable = false)
    private Long balance = 0L;

    @Column(name = "frozen", nullable = false)
    private Long frozen = 0L;

    @Column(name = "balance_digest", length = 64)
    private String balanceDigest = "";

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
}
