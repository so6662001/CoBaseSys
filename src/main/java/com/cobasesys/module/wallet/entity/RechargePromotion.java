package com.cobasesys.module.wallet.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_recharge_promotion")
public class RechargePromotion extends TenantEntity {

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /** 最低充值金额（分） */
    @Column(name = "min_amount", nullable = false)
    private Long minAmount;

    /** fixed=固定赠送金额 / rate=按比例赠送 */
    @Column(name = "gift_type", nullable = false, length = 20)
    private String giftType;

    /** 赠送金额（分）或比例 */
    @Column(name = "gift_value", precision = 12, scale = 2, nullable = false)
    private BigDecimal giftValue;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
