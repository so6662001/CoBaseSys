package com.cobasesys.module.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MemberDTO {

    @Data
    public static class LevelCreateRequest {
        @NotBlank(message = "等级编码不能为空") private String levelCode;
        @NotBlank(message = "等级名称不能为空") private String levelName;
        @Positive private Integer levelRank;
        private Long minPoints = 0L;
        private Long minConsumption = 0L;
        private BigDecimal pointMultiplier = BigDecimal.ONE;
        private BigDecimal discountRate = BigDecimal.ONE;
        private String iconUrl;
        private String description;
    }

    @Data
    public static class LevelUpdateRequest {
        private String levelName;
        private Integer levelRank;
        private Long minPoints;
        private Long minConsumption;
        private BigDecimal pointMultiplier;
        private BigDecimal discountRate;
        private String iconUrl;
        private String description;
        private Integer status;
    }

    @Data
    public static class LevelResponse {
        private Long id;
        private String levelCode;
        private String levelName;
        private Integer levelRank;
        private Long minPoints;
        private Long minConsumption;
        private BigDecimal pointMultiplier;
        private BigDecimal discountRate;
        private String iconUrl;
        private String description;
        private Integer status;
        private LocalDateTime createdAt;
    }

    @Data
    public static class UserMemberResponse {
        private String userId;
        private String levelCode;
        private String levelName;
        private Integer levelRank;
        private Long totalPointsEarned;
        private Long totalConsumption;
        private BigDecimal pointMultiplier;
        private BigDecimal discountRate;
        private LocalDateTime levelUpdatedAt;
    }
}
