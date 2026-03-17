package com.cobasesys.module.wallet.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_recharge_order", uniqueConstraints = {
        @UniqueConstraint(columnNames = "order_no")
})
public class RechargeOrder extends TenantEntity {

    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    private String orderNo;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    /** 充值金额（分） */
    @Column(name = "amount", nullable = false)
    private Long amount;

    /** 实际到账金额（分） */
    @Column(name = "actual_amount", nullable = false)
    private Long actualAmount;

    /** 赠送金额（分） */
    @Column(name = "gift_amount", nullable = false)
    private Long giftAmount = 0L;

    @Column(name = "payment_method", length = 32)
    private String paymentMethod;

    @Column(name = "payment_no", length = 128)
    private String paymentNo;

    /** 0:待支付 1:已支付 2:已取消 3:已退款 */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "remark", length = 512)
    private String remark;
}
