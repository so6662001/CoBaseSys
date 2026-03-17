package com.cobasesys.module.invoice.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
public class InvoiceApiClient {

    @Value("${cobasesys.invoice.api-url:https://api.xsdapi.com/v1/invoice}")
    private String apiUrl;

    @Value("${cobasesys.invoice.seller-name:CoBaseSys}")
    private String sellerName;

    @Value("${cobasesys.invoice.seller-tax-no:91000000000000000X}")
    private String sellerTaxNo;

    public IssueResult issueInvoice(IssueRequest request) {
        log.info("Calling XSD invoice API: buyer={}, amount={}", request.getBuyerName(), request.getTotalAmount());

        // TODO: 对接新时代数电开票API的实际HTTP调用
        // 当前为模拟实现，生产环境需替换为真实API调用

        IssueResult result = new IssueResult();
        result.setSuccess(true);
        result.setRequestId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        result.setInvoiceCode("0" + System.currentTimeMillis() % 1000000000L);
        result.setInvoiceNumber(String.valueOf(System.currentTimeMillis() % 100000000L));
        result.setInvoiceDate(LocalDate.now());
        result.setPdfUrl("https://invoice-cdn.example.com/pdf/" + result.getRequestId() + ".pdf");
        result.setRawResponse("{\"code\":0,\"message\":\"success\",\"invoiceCode\":\"" + result.getInvoiceCode() + "\"}");
        return result;
    }

    public boolean voidInvoice(String invoiceCode, String invoiceNumber, String reason) {
        log.info("Calling XSD void API: code={}, number={}, reason={}", invoiceCode, invoiceNumber, reason);
        return true;
    }

    @Data
    public static class IssueRequest {
        private String buyerName;
        private String buyerTaxNo;
        private String buyerAddress;
        private String buyerPhone;
        private String buyerBankName;
        private String buyerBankAccount;
        private String invoiceType;
        private long totalAmount;
        private String itemName;
        private double taxRate;
    }

    @Data
    public static class IssueResult {
        private boolean success;
        private String requestId;
        private String invoiceCode;
        private String invoiceNumber;
        private LocalDate invoiceDate;
        private String pdfUrl;
        private String rawResponse;
        private String errorMessage;
    }
}
