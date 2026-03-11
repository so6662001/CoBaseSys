package com.cobasesys.module.points.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "t_point_transaction", uniqueConstraints = {
        @UniqueConstraint(columnNames = "transaction_no"),
        @UniqueConstraint(columnNames = "idempotent_key")
}, indexes = {
        @Index(columnList = "account_id, created_at")
})
public class PointTransaction extends TenantEntity {

    @Column(name = "transaction_no", nullable = false, unique = true, length = 64)
    private String transactionNo;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "system_id", nullable = false)
    private Long systemId;

    @Column(name = "action_id", nullable = false)
    private Long actionId;

    @Column(name = "rule_id")
    private Long ruleId;

    /** 1=收入, -1=支出 */
    @Column(name = "direction", nullable = false)
    private Integer direction;

    @Column(name = "points", nullable = false)
    private Long points;

    @Column(name = "balance_before", nullable = false)
    private Long balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(name = "biz_order_no", length = 128)
    private String bizOrderNo;

    @Column(name = "biz_amount", precision = 12, scale = 2)
    private BigDecimal bizAmount;

    @Column(name = "remark", length = 512)
    private String remark;

    @Column(name = "idempotent_key", nullable = false, unique = true, length = 128)
    private String idempotentKey;

    @Column(name = "data_hash", length = 64)
    private String dataHash = "";

    @Column(name = "prev_hash", length = 64)
    private String prevHash = "";

    @Column(name = "chain_hash", length = 64)
    private String chainHash = "";
}
