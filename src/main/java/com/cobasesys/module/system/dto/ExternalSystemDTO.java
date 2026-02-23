package com.cobasesys.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

public class ExternalSystemDTO {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "系统编码不能为空")
        private String systemCode;
        @NotBlank(message = "系统名称不能为空")
        private String systemName;
        private String description;
        private String callbackUrl;
        private String ipWhitelist;
        private Integer rateLimit;
    }

    @Data
    public static class UpdateRequest {
        private String systemName;
        private String description;
        private String callbackUrl;
        private String ipWhitelist;
        private Integer rateLimit;
        private Integer status;
    }

    @Data
    public static class Response {
        private Long id;
        private String systemCode;
        private String systemName;
        private String description;
        private String appKey;
        private String callbackUrl;
        private String ipWhitelist;
        private Integer rateLimit;
        private Integer status;
        private LocalDateTime createdAt;
    }

    @Data
    public static class DetailResponse {
        private Long id;
        private String systemCode;
        private String systemName;
        private String description;
        private String appKey;
        private String appSecret;
        private String callbackUrl;
        private String ipWhitelist;
        private Integer rateLimit;
        private Integer status;
        private LocalDateTime createdAt;
    }
}
