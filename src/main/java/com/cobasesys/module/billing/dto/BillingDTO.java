package com.cobasesys.module.billing.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class BillingDTO {

    // ===== Product =====
    @Data
    public static class ProductCreateReq {
        @NotBlank private String productCode;
        @NotBlank private String productName;
        private String category;
        private String description;
        private String iconUrl;
        @NotBlank private String pricingModel;
        private Integer trialEnabled = 0;
        private Integer trialDays = 0;
        private Integer trialExtendEnabled = 0;
        private Integer trialMaxExtendDays = 0;
        private Integer pointsPayable = 0;
        private BigDecimal maxPointsRatio = BigDecimal.ZERO;
        private BigDecimal pointsExchangeRate = BigDecimal.ZERO;
        private Integer sortOrder = 0;
    }

    @Data
    public static class ProductResp {
        private Long id;
        private String productCode;
        private String productName;
        private String category;
        private String description;
        private String iconUrl;
        private String pricingModel;
        private Integer trialEnabled;
        private Integer trialDays;
        private Integer trialExtendEnabled;
        private Integer trialMaxExtendDays;
        private Integer pointsPayable;
        private BigDecimal maxPointsRatio;
        private BigDecimal pointsExchangeRate;
        private Integer sortOrder;
        private Integer status;
        private LocalDateTime createdAt;
    }

    // ===== Package =====
    @Data
    public static class PackageCreateReq {
        @NotBlank private String packageCode;
        @NotBlank private String packageName;
        private String description;
        private String iconUrl;
        @NotBlank private String pricingModel;
        private Integer trialEnabled = 0;
        private Integer trialDays = 0;
        private Integer trialExtendEnabled = 0;
        private Integer trialMaxExtendDays = 0;
        private Integer pointsPayable = 0;
        private BigDecimal maxPointsRatio = BigDecimal.ZERO;
        private BigDecimal pointsExchangeRate = BigDecimal.ZERO;
        private Integer sortOrder = 0;
        private List<PackageItemReq> items;
    }

    @Data
    public static class PackageItemReq {
        @Positive private Long productId;
        @Positive private Integer quantity;
        private Integer sortOrder = 0;
    }

    @Data
    public static class PackageResp {
        private Long id;
        private String packageCode;
        private String packageName;
        private String description;
        private String pricingModel;
        private Integer trialEnabled;
        private Integer pointsPayable;
        private Integer status;
        private LocalDateTime createdAt;
        private List<PackageItemResp> items;
    }

    @Data
    public static class PackageItemResp {
        private Long id;
        private Long productId;
        private String productName;
        private Integer quantity;
    }

    // ===== PricingPlan =====
    @Data
    public static class PricingPlanCreateReq {
        @NotBlank private String targetType;
        @Positive private Long targetId;
        @NotBlank private String planName;
        @NotBlank private String pricingModel;
        private Long softwareFee = 0L;
        private Long annualServiceFee = 0L;
        private String periodType;
        private Long periodPrice = 0L;
        private Integer includedQuantity = 0;
        private String overageUnitName;
        private Long overageUnitPrice = 0L;
        private String unitName;
        private Long unitPrice = 0L;
        private String tieredPricing;
        private String rentalPeriodType;
        private Long rentalPrice = 0L;
        private String spaceUnit;
        private Long spaceUnitPrice = 0L;
        private Long oneTimePrice = 0L;
        private Integer validityDays;
        private Integer priority = 0;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveTo;
    }

    @Data
    public static class PricingPlanResp {
        private Long id;
        private String targetType;
        private Long targetId;
        private String planName;
        private String pricingModel;
        private Long softwareFee;
        private Long annualServiceFee;
        private String periodType;
        private Long periodPrice;
        private Integer includedQuantity;
        private String overageUnitName;
        private Long overageUnitPrice;
        private String unitName;
        private Long unitPrice;
        private String tieredPricing;
        private String rentalPeriodType;
        private Long rentalPrice;
        private String spaceUnit;
        private Long spaceUnitPrice;
        private Long oneTimePrice;
        private Integer validityDays;
        private Integer priority;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveTo;
        private Integer status;
        private LocalDateTime createdAt;
    }

    // ===== Discount / Gift =====
    @Data
    public static class DiscountRuleReq {
        @NotBlank private String ruleName;
        @NotBlank private String discountType;
        private String targetType;
        private Long targetId;
        private Integer minQuantity = 1;
        private BigDecimal discountRate;
        private Long thresholdAmount = 0L;
        private Long bonusPoints = 0L;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveTo;
        private Integer priority = 0;
    }

    @Data
    public static class GiftRuleReq {
        @NotBlank private String ruleName;
        @NotBlank private String conditionType;
        private String conditionTargetType;
        private Long conditionTargetId;
        private Integer conditionQuantity = 1;
        @NotBlank private String giftType;
        private Long giftTargetId;
        private Integer giftQuantity = 1;
        private Long giftPoints = 0L;
        private Integer giftValidityDays;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveTo;
    }

    // ===== Order =====
    @Data
    public static class CreateOrderReq {
        @NotBlank private String customerId;
        private String customerName;
        @NotEmpty private List<OrderItemReq> items;
        private Boolean usePoints = false;
        private String remark;
    }

    @Data
    public static class ProxyOrderReq {
        @NotBlank private String customerId;
        @NotBlank private String customerName;
        @NotEmpty private List<OrderItemReq> items;
        private BigDecimal manualDiscountRate;
        private Boolean usePoints = false;
        private String remark;
        private Boolean autoConfirmPayment = false;
    }

    @Data
    public static class OrderItemReq {
        @NotBlank private String itemType;
        @Positive private Long itemId;
        @Positive private Integer quantity;
        private String periodType;
        private Integer periodCount = 1;
    }

    @Data
    public static class OrderResp {
        private Long id;
        private String orderNo;
        private String customerId;
        private String customerName;
        private String orderType;
        private String orderSource;
        private String operatorName;
        private Long totalAmount;
        private Long discountAmount;
        private Long giftAmount;
        private Long pointsDeductAmount;
        private Long pointsUsed;
        private Long actualAmount;
        private String actualAmountDisplay;
        private Integer paymentStatus;
        private String paymentStatusText;
        private String paymentMethod;
        private LocalDateTime paidAt;
        private String remark;
        private Integer status;
        private Integer invoiceStatus;
        private LocalDateTime createdAt;
        private List<OrderItemResp> items;
    }

    @Data
    public static class OrderItemResp {
        private Long id;
        private String itemType;
        private Long itemId;
        private String itemName;
        private String pricingModel;
        private Integer quantity;
        private Long unitPrice;
        private Long originalAmount;
        private Long discountAmount;
        private Long actualAmount;
        private String periodType;
        private Integer periodCount;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer isGift;
    }

    // ===== Subscription =====
    @Data
    public static class SubscriptionResp {
        private Long id;
        private String subscriptionNo;
        private String customerId;
        private String sourceType;
        private Long sourceId;
        private String sourceName;
        private String pricingModel;
        private Integer quantity;
        private String status;
        private Integer isTrial;
        private LocalDate startDate;
        private LocalDate endDate;

        private Integer totalDays;
        private Integer daysUsed;
        private Integer daysRemaining;

        private Long usageQuota;
        private Long usageUsed;
        private Long usageRemaining;
        private String usageUnit;

        private Long spaceTotal;
        private Long spaceUsed;
        private Long spaceRemaining;
        private String spaceUsageRate;

        private Long renewalPrice;
        private String renewalPriceDisplay;
        private LocalDateTime createdAt;
    }

    // ===== Trial =====
    @Data
    public static class TrialApplyReq {
        @NotBlank private String customerId;
        @NotBlank private String sourceType;
        @Positive private Long sourceId;
    }

    @Data
    public static class TrialExtendReq {
        @Positive private Integer extendDays;
        private String applyReason;
        @NotBlank private String applicantId;
        private String applicantName;
    }

    @Data
    public static class TrialResp {
        private Long id;
        private String customerId;
        private String sourceType;
        private Long sourceId;
        private String sourceName;
        private Integer trialDays;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private Integer extendCount;
        private Integer totalExtendDays;
        private LocalDateTime createdAt;
    }

    @Data
    public static class TrialExtendApprovalResp {
        private Long id;
        private Long trialId;
        private String customerName;
        private String sourceName;
        private Integer extendDays;
        private String applyReason;
        private String applicantName;
        private LocalDateTime applyTime;
        private String status;
        private String approverName;
        private LocalDateTime approveTime;
        private String approveRemark;
    }

    // ===== Usage Ledger =====
    @Data
    public static class UsageReportReq {
        @NotBlank private String customerId;
        @NotBlank private String subscriptionNo;
        @NotNull private Long quantity;
        @NotBlank private String unit;
        private String bizSystem;
        private String bizOrderNo;
        private String bizDescription;
    }

    @Data
    public static class UsageLedgerResp {
        private Long id;
        private String action;
        private Long quantity;
        private String unit;
        private Long balanceBefore;
        private Long balanceAfter;
        private Long unitPrice;
        private Long amount;
        private String bizSystem;
        private String bizOrderNo;
        private String bizDescription;
        private LocalDateTime createdAt;
    }

    // ===== Price Calculate =====
    @Data
    public static class PriceCalculateReq {
        @NotBlank private String customerId;
        @NotEmpty private List<OrderItemReq> items;
        private Boolean usePoints = false;
    }

    @Data
    public static class PriceCalculateResp {
        private Long totalAmount;
        private Long discountAmount;
        private Long pointsDeductAmount;
        private Long pointsNeeded;
        private Long actualAmount;
        private String actualAmountDisplay;
        private List<GiftPreview> gifts;
        private List<ItemPriceDetail> itemDetails;
    }

    @Data
    public static class ItemPriceDetail {
        private String itemType;
        private Long itemId;
        private String itemName;
        private Integer quantity;
        private Long unitPrice;
        private Long originalAmount;
        private Long discountAmount;
        private Long actualAmount;
    }

    @Data
    public static class GiftPreview {
        private String giftType;
        private String giftName;
        private Integer giftQuantity;
        private Long giftPoints;
        private String ruleName;
    }

    // ===== Check API =====
    @Data
    public static class ProductCheckReq {
        @NotBlank private String customerId;
        @NotEmpty private List<String> productCodes;
    }

    @Data
    public static class ProductCheckResp {
        private String productCode;
        private String productName;
        private String status;
        private Integer quantity;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer daysRemaining;
        private Boolean needRenewal;
        private Long renewalPrice;
        private String renewalPriceDisplay;
        private Long usageQuota;
        private Long usageUsed;
        private Long usageRemaining;
    }

    // ===== Helpers =====
    public static String formatAmount(long cents) {
        return String.format("%.2f元", cents / 100.0);
    }

    public static String paymentStatusText(int status) {
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已取消";
            case 3 -> "已退款";
            default -> "未知";
        };
    }
}
