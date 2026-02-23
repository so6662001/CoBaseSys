package com.cobasesys.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

public class TenantDTO {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "租户编码不能为空")
        private String tenantCode;
        @NotBlank(message = "租户名称不能为空")
        private String tenantName;
        private String contactName;
        private String contactPhone;
        private String contactEmail;
    }

    @Data
    public static class UpdateRequest {
        private String tenantName;
        private String contactName;
        private String contactPhone;
        private String contactEmail;
        private Integer status;
    }

    @Data
    public static class Response {
        private Long id;
        private String tenantCode;
        private String tenantName;
        private String contactName;
        private String contactPhone;
        private String contactEmail;
        private Integer status;
        private LocalDateTime createdAt;
    }
}
