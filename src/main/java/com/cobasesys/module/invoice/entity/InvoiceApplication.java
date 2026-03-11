package com.cobasesys.module.invoice.entity;

import com.cobasesys.module.billing.entity.BillingBaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_invoice_application")
public class InvoiceApplication extends BillingBaseEntity {

    @Column(name = "application_no", nullable = false, unique = true, length = 64)
    private String applicationNo;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "customer_name", length = 128)
    private String customerName;

    @Column(name = "invoice_type", nullable = false, length = 20)
    private String invoiceType;

    @Column(name = "title_name", nullable = false, length = 200)
    private String titleName;

    @Column(name = "tax_no", nullable = false, length = 30)
    private String taxNo;

    @Column(name = "bank_name", length = 128)
    private String bankName;

    @Column(name = "bank_account", length = 64)
    private String bankAccount;

    @Column(name = "company_address", length = 256)
    private String companyAddress;

    @Column(name = "company_phone", length = 32)
    private String companyPhone;

    @Column(name = "receiver_email", nullable = false, length = 128)
    private String receiverEmail;

    @Column(name = "remark", length = 512)
    private String remark;

    @Column(name = "item_mode", nullable = false, length = 20)
    private String itemMode = "DEFAULT";

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate = new BigDecimal("0.06");

    @Column(name = "tax_amount")
    private Long taxAmount;

    @Column(name = "amount_without_tax")
    private Long amountWithoutTax;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "reviewer_id", length = 64)
    private String reviewerId;

    @Column(name = "reviewer_name", length = 128)
    private String reviewerName;

    @Column(name = "review_time")
    private LocalDateTime reviewTime;

    @Column(name = "reject_reason", length = 512)
    private String rejectReason;

    @Column(name = "invoice_code", length = 30)
    private String invoiceCode;

    @Column(name = "invoice_number", length = 20)
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "pdf_url", length = 512)
    private String pdfUrl;

    @Column(name = "pdf_local_path", length = 512)
    private String pdfLocalPath;

    @Column(name = "api_request_id", length = 64)
    private String apiRequestId;

    @Column(name = "api_response", columnDefinition = "TEXT")
    private String apiResponse;

    @Column(name = "void_reason", length = 512)
    private String voidReason;

    @Column(name = "void_time")
    private LocalDateTime voidTime;

    @Column(name = "void_invoice_number", length = 20)
    private String voidInvoiceNumber;

    @Column(name = "email_sent", nullable = false)
    private Integer emailSent = 0;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Column(name = "sms_sent", nullable = false)
    private Integer smsSent = 0;

    @Column(name = "sms_sent_at")
    private LocalDateTime smsSentAt;
}
