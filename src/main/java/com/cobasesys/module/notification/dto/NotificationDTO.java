package com.cobasesys.module.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

public class NotificationDTO {

    @Data
    public static class TemplateCreateRequest {
        @NotBlank private String templateCode;
        @NotBlank private String templateName;
        @NotBlank private String channel;
        private String subject;
        @NotBlank private String content;
    }

    @Data
    public static class TemplateUpdateRequest {
        private String templateName;
        private String subject;
        private String content;
        private Integer status;
    }

    @Data
    public static class TemplateResponse {
        private Long id;
        private String templateCode;
        private String templateName;
        private String channel;
        private String subject;
        private String content;
        private Integer status;
        private LocalDateTime createdAt;
    }

    @Data
    public static class RuleCreateRequest {
        @NotBlank private String ruleName;
        @NotBlank private String triggerType;
        private Long thresholdValue;
        @NotBlank private String channel;
        private Long templateId;
    }

    @Data
    public static class RuleUpdateRequest {
        private String ruleName;
        private String triggerType;
        private Long thresholdValue;
        private String channel;
        private Long templateId;
        private Integer status;
    }

    @Data
    public static class RuleResponse {
        private Long id;
        private String ruleName;
        private String triggerType;
        private Long thresholdValue;
        private String channel;
        private Long templateId;
        private Integer status;
        private LocalDateTime createdAt;
    }

    @Data
    public static class RecordResponse {
        private Long id;
        private String userId;
        private String channel;
        private String subject;
        private String content;
        private String recipient;
        private Integer status;
        private String statusText;
        private String errorMessage;
        private LocalDateTime sentAt;
        private LocalDateTime createdAt;
    }
}
