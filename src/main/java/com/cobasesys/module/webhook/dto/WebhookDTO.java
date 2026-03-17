package com.cobasesys.module.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

public class WebhookDTO {

    @Data
    public static class ConfigCreateRequest {
        private Long systemId;
        @NotBlank(message = "Webhook名称不能为空") private String webhookName;
        @NotBlank(message = "URL不能为空") private String url;
        private String secret;
        @NotBlank(message = "事件类型不能为空") private String events;
    }

    @Data
    public static class ConfigUpdateRequest {
        private String webhookName;
        private String url;
        private String secret;
        private String events;
        private Integer status;
    }

    @Data
    public static class ConfigResponse {
        private Long id;
        private Long systemId;
        private String webhookName;
        private String url;
        private String events;
        private Integer status;
        private LocalDateTime createdAt;
    }

    @Data
    public static class LogResponse {
        private Long id;
        private Long configId;
        private String eventType;
        private String payload;
        private Integer responseStatus;
        private Integer status;
        private String statusText;
        private Integer retryCount;
        private String errorMessage;
        private LocalDateTime createdAt;
    }
}
