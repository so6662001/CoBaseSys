package com.cobasesys.module.billing;

import com.cobasesys.module.billing.dto.BillingDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BillingDTOTest {

    @Test
    void formatAmount_shouldFormatCentsToYuan() {
        assertEquals("0.00元", BillingDTO.formatAmount(0));
        assertEquals("1.00元", BillingDTO.formatAmount(100));
        assertEquals("99.99元", BillingDTO.formatAmount(9999));
        assertEquals("100.50元", BillingDTO.formatAmount(10050));
        assertEquals("1234.56元", BillingDTO.formatAmount(123456));
    }

    @Test
    void paymentStatusText() {
        assertEquals("待支付", BillingDTO.paymentStatusText(0));
        assertEquals("已支付", BillingDTO.paymentStatusText(1));
        assertEquals("已取消", BillingDTO.paymentStatusText(2));
        assertEquals("已退款", BillingDTO.paymentStatusText(3));
        assertEquals("未知", BillingDTO.paymentStatusText(99));
    }
}
