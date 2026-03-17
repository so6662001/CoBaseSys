package com.cobasesys.module.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletDTO {

    // ===== Action DTOs =====

    @Data
    public static class ActionCreateRequest {
        @Positive private Long systemId;
        @NotBlank(message = "动作编码不能为空") private String actionCode;
        @NotBlank(message = "动作名称不能为空") private String actionName;
        private String description;
    }

    @Data
    public static class ActionUpdateRequest {
        private String actionName;
        private String description;
        private Integer status;
    }

    @Data
    public static class ActionResponse {
        private Long id;
        private Long systemId;
        private String actionCode;
        private String actionName;
        private String description;
        private Integer status;
        private LocalDateTime createdAt;
    }

    // ===== Rule DTOs =====

    @Data
    public static class RuleCreateRequest {
        @Positive private Long actionId;
        @NotBlank(message = "规则名称不能为空") private String ruleName;
        @NotBlank(message = "计算类型不能为空") private String calcType;
        private BigDecimal calcValue;
        private String calcExpression;
        private String unitName;
        private Integer minCharge;
        private Integer maxCharge;
        private Integer freeQuota;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveTo;
        private Integer priority = 0;
    }

    @Data
    public static class RuleUpdateRequest {
        private String ruleName;
        private String calcType;
        private BigDecimal calcValue;
        private String calcExpression;
        private String unitName;
        private Integer minCharge;
        private Integer maxCharge;
        private Integer freeQuota;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveTo;
        private Integer priority;
        private Integer status;
    }

    @Data
    public static class RuleResponse {
        private Long id;
        private Long actionId;
        private String ruleName;
        private String calcType;
        private BigDecimal calcValue;
        private String calcExpression;
        private String unitName;
        private Integer minCharge;
        private Integer maxCharge;
        private Integer freeQuota;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveTo;
        private Integer priority;
        private Integer status;
        private LocalDateTime createdAt;
    }

    // ===== Open API DTOs =====

    @Data
    public static class RechargeRequest {
        @NotBlank(message = "用户ID不能为空") private String userId;
        @Positive(message = "充值金额必须大于0") private Long amount;
        private String paymentMethod;
        private String remark;
    }

    @Data
    public static class RechargeCallbackRequest {
        @NotBlank(message = "订单号不能为空") private String orderNo;
        private String paymentNo;
        private boolean success;
    }

    @Data
    public static class ConsumeRequest {
        @NotBlank(message = "用户ID不能为空") private String userId;
        @NotBlank(message = "动作编码不能为空") private String actionCode;
        private String bizOrderNo;
        private BigDecimal bizQuantity;
        private String remark;
    }

    @Data
    public static class FreezeRequest {
        @NotBlank(message = "用户ID不能为空") private String userId;
        @Positive(message = "冻结金额必须大于0") private Long amount;
        private String bizOrderNo;
        private String remark;
    }

    @Data
    public static class RefundRequest {
        @NotBlank(message = "用户ID不能为空") private String userId;
        @Positive(message = "退款金额必须大于0") private Long amount;
        private String bizOrderNo;
        private String remark;
    }

    @Data
    public static class BalanceCheckRequest {
        @NotBlank(message = "用户ID不能为空") private String userId;
        @NotBlank(message = "动作编码不能为空") private String actionCode;
        private BigDecimal bizQuantity;
    }

    @Data
    public static class TransactionResult {
        private String transactionNo;
        private Long amount;
        private String amountDisplay;
        private Long balance;
        private String balanceDisplay;
        private String ruleName;
    }

    @Data
    public static class RechargeOrderResponse {
        private String orderNo;
        private Long amount;
        private Long actualAmount;
        private Long giftAmount;
        private String paymentMethod;
        private Integer status;
        private String statusText;
        private LocalDateTime paidAt;
        private LocalDateTime createdAt;
    }

    @Data
    public static class BalanceResponse {
        private String userId;
        private Long balance;
        private String balanceDisplay;
        private Long frozen;
        private Long totalRecharged;
        private Long totalConsumed;
    }

    @Data
    public static class TransactionResponse {
        private String transactionNo;
        private Integer type;
        private String typeText;
        private Long amount;
        private String amountDisplay;
        private Long balanceAfter;
        private String actionName;
        private String systemName;
        private String bizOrderNo;
        private String remark;
        private LocalDateTime createdAt;
    }

    @Data
    public static class PromotionCreateRequest {
        @NotBlank(message = "活动名称不能为空") private String name;
        @Positive private Long minAmount;
        @NotBlank private String giftType;
        private BigDecimal giftValue;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveTo;
    }

    @Data
    public static class PromotionResponse {
        private Long id;
        private String name;
        private Long minAmount;
        private String giftType;
        private BigDecimal giftValue;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveTo;
        private Integer status;
        private LocalDateTime createdAt;
    }

    @Data
    public static class BalanceCheckResponse {
        private boolean sufficient;
        private Long estimatedAmount;
        private String estimatedAmountDisplay;
        private Long currentBalance;
    }

    public static String formatAmount(long amountInCents) {
        return String.format("%.2f元", amountInCents / 100.0);
    }

    public static String typeText(int type) {
        return switch (type) {
            case 1 -> "充值";
            case 2 -> "消费";
            case 3 -> "退款";
            case 4 -> "冻结";
            case 5 -> "解冻";
            case 6 -> "调账";
            default -> "未知";
        };
    }
}
