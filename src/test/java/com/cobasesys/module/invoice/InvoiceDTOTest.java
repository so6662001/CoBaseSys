package com.cobasesys.module.invoice;

import com.cobasesys.module.invoice.dto.InvoiceDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceDTOTest {

    @Test
    void statusText_allStatuses() {
        assertEquals("待审核", InvoiceDTO.statusText("PENDING"));
        assertEquals("开票中", InvoiceDTO.statusText("APPROVED"));
        assertEquals("已开具", InvoiceDTO.statusText("ISSUED"));
        assertEquals("已驳回", InvoiceDTO.statusText("REJECTED"));
        assertEquals("已红冲", InvoiceDTO.statusText("VOIDED"));
        assertEquals("未知", InvoiceDTO.statusText(null));
    }

    @Test
    void invoiceTypeText() {
        assertEquals("增值税普通发票", InvoiceDTO.invoiceTypeText("NORMAL"));
        assertEquals("增值税专用发票", InvoiceDTO.invoiceTypeText("SPECIAL"));
        assertEquals("", InvoiceDTO.invoiceTypeText(null));
    }

    @Test
    void formatAmount() {
        assertEquals("0.00元", InvoiceDTO.formatAmount(0));
        assertEquals("100.00元", InvoiceDTO.formatAmount(10000));
        assertEquals("1234.56元", InvoiceDTO.formatAmount(123456));
    }
}
