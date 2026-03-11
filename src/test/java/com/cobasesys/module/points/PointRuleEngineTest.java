package com.cobasesys.module.points;

import com.cobasesys.module.points.entity.PointRule;
import com.cobasesys.module.points.service.PointRuleEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PointRuleEngineTest {

    private PointRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PointRuleEngine(new ObjectMapper());
    }

    @Test
    void fixed_shouldReturnFixedValue() {
        PointRule rule = new PointRule();
        rule.setCalcType("fixed");
        rule.setCalcValue(BigDecimal.valueOf(10));
        assertEquals(10, engine.calculate(rule, null));
    }

    @Test
    void rate_shouldMultiplyByAmount() {
        PointRule rule = new PointRule();
        rule.setCalcType("rate");
        rule.setCalcValue(BigDecimal.valueOf(2));
        assertEquals(598, engine.calculate(rule, BigDecimal.valueOf(299)));
    }

    @Test
    void rate_nullAmount_shouldReturnZero() {
        PointRule rule = new PointRule();
        rule.setCalcType("rate");
        rule.setCalcValue(BigDecimal.valueOf(2));
        assertEquals(0, engine.calculate(rule, null));
    }

    @Test
    void fixed_withMinPoints() {
        PointRule rule = new PointRule();
        rule.setCalcType("fixed");
        rule.setCalcValue(BigDecimal.valueOf(3));
        rule.setMinPoints(10);
        assertEquals(10, engine.calculate(rule, null));
    }

    @Test
    void fixed_withMaxPoints() {
        PointRule rule = new PointRule();
        rule.setCalcType("fixed");
        rule.setCalcValue(BigDecimal.valueOf(100));
        rule.setMaxPoints(50);
        assertEquals(50, engine.calculate(rule, null));
    }

    @Test
    void tiered_shouldCalculateByTiers() {
        PointRule rule = new PointRule();
        rule.setCalcType("tiered");
        rule.setCalcExpression("{\"tiers\":[{\"min\":0,\"max\":100,\"rate\":1},{\"min\":100,\"max\":null,\"rate\":2}]}");
        long points = engine.calculate(rule, BigDecimal.valueOf(250));
        assertTrue(points > 0);
    }

    @Test
    void custom_fixedValue() {
        PointRule rule = new PointRule();
        rule.setCalcType("custom");
        rule.setCalcExpression("{\"fixedValue\":88}");
        assertEquals(88, engine.calculate(rule, null));
    }

    @Test
    void custom_formula() {
        PointRule rule = new PointRule();
        rule.setCalcType("custom");
        rule.setCalcExpression("{\"formula\":true,\"multiplier\":1.5,\"offset\":10}");
        long points = engine.calculate(rule, BigDecimal.valueOf(100));
        assertEquals(160, points);
    }

    @Test
    void unknownType_shouldThrow() {
        PointRule rule = new PointRule();
        rule.setCalcType("unknown");
        assertThrows(IllegalArgumentException.class, () -> engine.calculate(rule, null));
    }
}
