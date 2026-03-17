package com.cobasesys.module.wallet.entity;

import com.cobasesys.common.model.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_wallet_account", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "user_id"})
})
public class WalletAccount extends TenantEntity {

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    /** 累计充值（单位：分） */
    @Column(name = "total_recharged", nullable = false)
    private Long totalRecharged = 0L;

    /** 累计消费（单位：分） */
    @Column(name = "total_consumed", nullable = false)
    private Long totalConsumed = 0L;

    /** 当前可用余额（单位：分） */
    @Column(name = "balance", nullable = false)
    private Long balance = 0L;

    /** 冻结金额（单位：分） */
    @Column(name = "frozen", nullable = false)
    private Long frozen = 0L;

    @Column(name = "balance_digest", length = 64)
    private String balanceDigest = "";

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
}
