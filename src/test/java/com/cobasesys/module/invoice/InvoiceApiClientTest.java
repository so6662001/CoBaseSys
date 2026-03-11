package com.cobasesys.module.invoice;

import com.cobasesys.module.invoice.service.InvoiceApiClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceApiClientTest {

    @Test
    void issueInvoice_shouldReturnSuccess() {
        InvoiceApiClient client = new InvoiceApiClient();

        InvoiceApiClient.IssueRequest req = new InvoiceApiClient.IssueRequest();
        req.setBuyerName("测试公司");
        req.setBuyerTaxNo("91000000000000001X");
        req.setInvoiceType("NORMAL");
        req.setTotalAmount(10000);
        req.setItemName("*信息技术服务*技术服务费");
        req.setTaxRate(0.06);

        InvoiceApiClient.IssueResult result = client.issueInvoice(req);

        assertTrue(result.isSuccess());
        assertNotNull(result.getRequestId());
        assertNotNull(result.getInvoiceCode());
        assertNotNull(result.getInvoiceNumber());
        assertNotNull(result.getInvoiceDate());
        assertNotNull(result.getPdfUrl());
        assertTrue(result.getPdfUrl().startsWith("https://"));
    }

    @Test
    void voidInvoice_shouldReturnTrue() {
        InvoiceApiClient client = new InvoiceApiClient();
        assertTrue(client.voidInvoice("INV001", "12345678", "开票错误"));
    }

    @Test
    void issueResult_fields() {
        InvoiceApiClient.IssueResult result = new InvoiceApiClient.IssueResult();
        result.setSuccess(false);
        result.setErrorMessage("测试错误");
        assertFalse(result.isSuccess());
        assertEquals("测试错误", result.getErrorMessage());
    }
}
