package com.cobasesys.module.member.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "t_member_level", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "level_code"})
})
public class MemberLevel extends TenantEntity {

    @Column(name = "level_code", nullable = false, length = 32)
    private String levelCode;

    @Column(name = "level_name", nullable = false, length = 64)
    private String levelName;

    /** 等级排序，数值越大等级越高 */
    @Column(name = "level_rank", nullable = false)
    private Integer levelRank;

    /** 晋升所需最低累计积分 */
    @Column(name = "min_points", nullable = false)
    private Long minPoints = 0L;

    /** 晋升所需最低累计消费（分） */
    @Column(name = "min_consumption", nullable = false)
    private Long minConsumption = 0L;

    /** 积分倍率加成 */
    @Column(name = "point_multiplier", precision = 5, scale = 2, nullable = false)
    private BigDecimal pointMultiplier = BigDecimal.ONE;

    /** 消费折扣（0.95 = 95折） */
    @Column(name = "discount_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal discountRate = BigDecimal.ONE;

    @Column(name = "icon_url", length = 512)
    private String iconUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
