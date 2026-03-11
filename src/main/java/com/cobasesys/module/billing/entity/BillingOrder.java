package com.cobasesys.module.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_billing_order")
public class BillingOrder extends BillingBaseEntity {

    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    private String orderNo;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "customer_name", length = 128)
    private String customerName;

    @Column(name = "order_type", nullable = false, length = 20)
    private String orderType;

    @Column(name = "order_source", nullable = false, length = 20)
    private String orderSource = "CUSTOMER";

    @Column(name = "operator_id", length = 64)
    private String operatorId;

    @Column(name = "operator_name", length = 128)
    private String operatorName;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount = 0L;

    @Column(name = "gift_amount", nullable = false)
    private Long giftAmount = 0L;

    @Column(name = "points_deduct_amount", nullable = false)
    private Long pointsDeductAmount = 0L;

    @Column(name = "points_used", nullable = false)
    private Long pointsUsed = 0L;

    @Column(name = "actual_amount", nullable = false)
    private Long actualAmount;

    @Column(name = "payment_status", nullable = false)
    private Integer paymentStatus = 0;

    @Column(name = "payment_method", length = 32)
    private String paymentMethod;

    @Column(name = "payment_no", length = 128)
    private String paymentNo;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "remark", length = 512)
    private String remark;

    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "invoice_status")
    private Integer invoiceStatus = 0;
}
