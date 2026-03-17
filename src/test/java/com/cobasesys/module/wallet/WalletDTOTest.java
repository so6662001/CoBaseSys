package com.cobasesys.module.wallet;

import com.cobasesys.module.wallet.dto.WalletDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WalletDTOTest {

    @Test
    void formatAmount() {
        assertEquals("0.00元", WalletDTO.formatAmount(0));
        assertEquals("1.00元", WalletDTO.formatAmount(100));
        assertEquals("99.99元", WalletDTO.formatAmount(9999));
    }

    @Test
    void typeText() {
        assertEquals("充值", WalletDTO.typeText(1));
        assertEquals("消费", WalletDTO.typeText(2));
        assertEquals("退款", WalletDTO.typeText(3));
        assertEquals("冻结", WalletDTO.typeText(4));
        assertEquals("解冻", WalletDTO.typeText(5));
        assertEquals("调账", WalletDTO.typeText(6));
        assertEquals("未知", WalletDTO.typeText(99));
    }
}
