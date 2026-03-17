package com.cobasesys.module.wallet;

import com.cobasesys.module.wallet.entity.ConsumeRule;
import com.cobasesys.module.wallet.service.ConsumeRuleEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ConsumeRuleEngineTest {

    private ConsumeRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ConsumeRuleEngine(new ObjectMapper());
    }

    @Test
    void fixed_shouldReturnFixedAmountInCents() {
        ConsumeRule rule = new ConsumeRule();
        rule.setCalcType("fixed");
        rule.setCalcValue(BigDecimal.valueOf(0.50));
        rule.setFreeQuota(0);
        long cents = engine.calculate(rule, null);
        assertEquals(50, cents);
    }

    @Test
    void unitPrice_shouldMultiplyByQuantity() {
        ConsumeRule rule = new ConsumeRule();
        rule.setCalcType("unit_price");
        rule.setCalcValue(BigDecimal.valueOf(0.02));
        rule.setFreeQuota(0);
        long cents = engine.calculate(rule, BigDecimal.valueOf(1000));
        assertEquals(2000, cents);
    }

    @Test
    void unitPrice_withFreeQuota_shouldSubtract() {
        ConsumeRule rule = new ConsumeRule();
        rule.setCalcType("unit_price");
        rule.setCalcValue(BigDecimal.valueOf(0.10));
        rule.setFreeQuota(100);
        long cents = engine.calculate(rule, BigDecimal.valueOf(150));
        assertEquals(500, cents);
    }

    @Test
    void unitPrice_withinFreeQuota_shouldBeZero() {
        ConsumeRule rule = new ConsumeRule();
        rule.setCalcType("unit_price");
        rule.setCalcValue(BigDecimal.valueOf(0.10));
        rule.setFreeQuota(200);
        long cents = engine.calculate(rule, BigDecimal.valueOf(150));
        assertEquals(0, cents);
    }

    @Test
    void fixed_withMinCharge() {
        ConsumeRule rule = new ConsumeRule();
        rule.setCalcType("fixed");
        rule.setCalcValue(BigDecimal.valueOf(0.01));
        rule.setMinCharge(100);
        rule.setFreeQuota(0);
        long cents = engine.calculate(rule, null);
        assertEquals(100, cents);
    }

    @Test
    void fixed_withMaxCharge() {
        ConsumeRule rule = new ConsumeRule();
        rule.setCalcType("fixed");
        rule.setCalcValue(BigDecimal.valueOf(100.00));
        rule.setMaxCharge(5000);
        rule.setFreeQuota(0);
        long cents = engine.calculate(rule, null);
        assertEquals(5000, cents);
    }

    @Test
    void unknownType_shouldThrow() {
        ConsumeRule rule = new ConsumeRule();
        rule.setCalcType("unknown");
        rule.setFreeQuota(0);
        assertThrows(IllegalArgumentException.class, () -> engine.calculate(rule, null));
    }
}
