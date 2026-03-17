package com.cobasesys.module.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_billing_order_item")
public class BillingOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "item_type", nullable = false, length = 10)
    private String itemType;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_name", length = 200)
    private String itemName;

    @Column(name = "pricing_plan_id")
    private Long pricingPlanId;

    @Column(name = "pricing_model", length = 30)
    private String pricingModel;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price")
    private Long unitPrice;

    @Column(name = "original_amount")
    private Long originalAmount;

    @Column(name = "discount_amount")
    private Long discountAmount = 0L;

    @Column(name = "actual_amount")
    private Long actualAmount;

    @Column(name = "period_type", length = 10)
    private String periodType;

    @Column(name = "period_count", nullable = false)
    private Integer periodCount = 1;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_gift", nullable = false)
    private Integer isGift = 0;

    @Column(name = "gift_rule_id")
    private Long giftRuleId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
