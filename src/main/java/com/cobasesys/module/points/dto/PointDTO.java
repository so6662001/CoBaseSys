package com.cobasesys.module.points.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PointDTO {

    // ===== Action DTOs =====

    @Data
    public static class ActionCreateRequest {
        @Positive private Long systemId;
        @NotBlank(message = "动作编码不能为空") private String actionCode;
        @NotBlank(message = "动作名称不能为空") private String actionName;
        private String description;
        private Integer direction = 1;
    }

    @Data
    public static class ActionUpdateRequest {
        private String actionName;
        private String description;
        private Integer direction;
        private Integer status;
    }

    @Data
    public static class ActionResponse {
        private Long id;
        private Long systemId;
        private String actionCode;
        private String actionName;
        private String description;
        private Integer direction;
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
        private Integer minPoints;
        private Integer maxPoints;
        private Integer dailyLimit;
        private Integer monthlyLimit;
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
        private Integer minPoints;
        private Integer maxPoints;
        private Integer dailyLimit;
        private Integer monthlyLimit;
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
        private Integer minPoints;
        private Integer maxPoints;
        private Integer dailyLimit;
        private Integer monthlyLimit;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveTo;
        private Integer priority;
        private Integer status;
        private LocalDateTime createdAt;
    }

    // ===== Open API DTOs =====

    @Data
    public static class EarnRequest {
        @NotBlank(message = "用户ID不能为空") private String userId;
        @NotBlank(message = "动作编码不能为空") private String actionCode;
        private String bizOrderNo;
        private BigDecimal bizAmount;
        private String remark;
    }

    @Data
    public static class DeductRequest {
        @NotBlank(message = "用户ID不能为空") private String userId;
        @NotBlank(message = "动作编码不能为空") private String actionCode;
        @Positive(message = "扣减积分必须大于0") private Long points;
        private String bizOrderNo;
        private String remark;
    }

    @Data
    public static class FreezeRequest {
        @NotBlank(message = "用户ID不能为空") private String userId;
        @Positive(message = "冻结积分必须大于0") private Long points;
        private String bizOrderNo;
        private String remark;
    }

    @Data
    public static class TransactionResult {
        private String transactionNo;
        private Long points;
        private Long balance;
        private String ruleName;
    }

    @Data
    public static class BalanceResponse {
        private String userId;
        private Long balance;
        private Long frozen;
        private Long totalEarned;
        private Long totalConsumed;
    }

    @Data
    public static class TransactionResponse {
        private String transactionNo;
        private Integer direction;
        private String directionText;
        private Long points;
        private Long balanceAfter;
        private String actionName;
        private String systemName;
        private String bizOrderNo;
        private String remark;
        private LocalDateTime createdAt;
    }
}
