package com.cobasesys.module.wallet.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "t_wallet_transaction", uniqueConstraints = {
        @UniqueConstraint(columnNames = "transaction_no"),
        @UniqueConstraint(columnNames = "idempotent_key")
}, indexes = {
        @Index(columnList = "account_id, created_at")
})
public class WalletTransaction extends TenantEntity {

    @Column(name = "transaction_no", nullable = false, unique = true, length = 64)
    private String transactionNo;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    /** 1:充值 2:消费 3:退款 4:冻结 5:解冻 6:调账 */
    @Column(name = "type", nullable = false)
    private Integer type;

    @Column(name = "system_id")
    private Long systemId;

    @Column(name = "action_id")
    private Long actionId;

    @Column(name = "rule_id")
    private Long ruleId;

    /** 变动金额（分） */
    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "balance_before", nullable = false)
    private Long balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(name = "biz_order_no", length = 128)
    private String bizOrderNo;

    @Column(name = "biz_quantity", precision = 12, scale = 4)
    private BigDecimal bizQuantity;

    @Column(name = "remark", length = 512)
    private String remark;

    @Column(name = "idempotent_key", nullable = false, unique = true, length = 128)
    private String idempotentKey;
}
