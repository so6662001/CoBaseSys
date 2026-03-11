package com.cobasesys.module.invoice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class InvoiceDTO {

    @Data
    public static class ApplyRequest {
        @NotBlank private String customerId;
        private String customerName;
        @NotBlank private String invoiceType;
        @NotBlank private String titleName;
        @NotBlank private String taxNo;
        private String bankName;
        private String bankAccount;
        private String companyAddress;
        private String companyPhone;
        @NotBlank @Email private String receiverEmail;
        private String remark;
        @NotEmpty private List<Long> orderIds;
    }

    @Data
    public static class ApproveRequest {
        private String itemMode = "DEFAULT";
        private String reviewerId;
        private String reviewerName;
    }

    @Data
    public static class RejectRequest {
        @NotBlank private String rejectReason;
        private String reviewerId;
        private String reviewerName;
    }

    @Data
    public static class VoidRequest {
        @NotBlank private String voidReason;
    }

    @Data
    public static class ApplicationResponse {
        private Long id;
        private String applicationNo;
        private String customerId;
        private String customerName;
        private String invoiceType;
        private String invoiceTypeText;
        private String titleName;
        private String taxNo;
        private String bankName;
        private String bankAccount;
        private String companyAddress;
        private String companyPhone;
        private String receiverEmail;
        private String remark;
        private String itemMode;
        private Long totalAmount;
        private String totalAmountDisplay;
        private BigDecimal taxRate;
        private Long taxAmount;
        private Long amountWithoutTax;
        private String status;
        private String statusText;
        private String reviewerName;
        private LocalDateTime reviewTime;
        private String rejectReason;
        private String invoiceCode;
        private String invoiceNumber;
        private LocalDate invoiceDate;
        private String pdfUrl;
        private String pdfLocalPath;
        private String voidReason;
        private LocalDateTime voidTime;
        private Integer emailSent;
        private Integer smsSent;
        private LocalDateTime createdAt;
        private List<OrderItem> orders;
    }

    @Data
    public static class OrderItem {
        private Long orderId;
        private String orderNo;
        private Long orderAmount;
        private String orderAmountDisplay;
    }

    @Data
    public static class Statistics {
        private long totalIssued;
        private long totalPending;
        private long totalRejected;
        private long totalVoided;
        private Long totalIssuedAmount;
        private String totalIssuedAmountDisplay;
    }

    public static String statusText(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "PENDING" -> "待审核";
            case "APPROVED" -> "开票中";
            case "ISSUED" -> "已开具";
            case "REJECTED" -> "已驳回";
            case "VOIDED" -> "已红冲";
            default -> status;
        };
    }

    public static String invoiceTypeText(String type) {
        if (type == null) return "";
        return switch (type) {
            case "NORMAL" -> "增值税普通发票";
            case "SPECIAL" -> "增值税专用发票";
            default -> type;
        };
    }

    public static String formatAmount(long cents) {
        return String.format("%.2f元", cents / 100.0);
    }
}
