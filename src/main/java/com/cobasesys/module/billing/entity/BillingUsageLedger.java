package com.cobasesys.module.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_billing_usage_ledger")
public class BillingUsageLedger extends BillingBaseEntity {

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Column(name = "unit", nullable = false, length = 32)
    private String unit;

    @Column(name = "balance_before", nullable = false)
    private Long balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(name = "unit_price")
    private Long unitPrice = 0L;

    @Column(name = "amount")
    private Long amount = 0L;

    @Column(name = "biz_system", length = 64)
    private String bizSystem;

    @Column(name = "biz_order_no", length = 128)
    private String bizOrderNo;

    @Column(name = "biz_description", length = 512)
    private String bizDescription;

    @Column(name = "idempotent_key", nullable = false, unique = true, length = 128)
    private String idempotentKey;

    @Column(name = "data_hash", nullable = false, length = 64)
    private String dataHash = "";

    @Column(name = "prev_hash", nullable = false, length = 64)
    private String prevHash = "";

    @Column(name = "chain_hash", nullable = false, length = 64)
    private String chainHash = "";
}
