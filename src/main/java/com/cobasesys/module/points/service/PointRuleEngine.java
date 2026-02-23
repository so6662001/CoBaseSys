package com.cobasesys.module.points.service;

import com.cobasesys.module.points.entity.PointRule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointRuleEngine {

    private final ObjectMapper objectMapper;

    public long calculate(PointRule rule, BigDecimal bizAmount) {
        long points = switch (rule.getCalcType()) {
            case "fixed" -> calculateFixed(rule);
            case "rate" -> calculateRate(rule, bizAmount);
            case "tiered" -> calculateTiered(rule, bizAmount);
            case "custom" -> calculateCustom(rule, bizAmount);
            default -> throw new IllegalArgumentException("Unknown calc type: " + rule.getCalcType());
        };

        if (rule.getMinPoints() != null) {
            points = Math.max(points, rule.getMinPoints());
        }
        if (rule.getMaxPoints() != null) {
            points = Math.min(points, rule.getMaxPoints());
        }

        return Math.max(points, 0);
    }

    private long calculateFixed(PointRule rule) {
        return rule.getCalcValue().longValue();
    }

    private long calculateRate(PointRule rule, BigDecimal bizAmount) {
        if (bizAmount == null) return 0;
        return bizAmount.multiply(rule.getCalcValue())
                .setScale(0, RoundingMode.FLOOR).longValue();
    }

    private long calculateTiered(PointRule rule, BigDecimal bizAmount) {
        if (bizAmount == null || !StringUtils.hasText(rule.getCalcExpression())) return 0;
        try {
            JsonNode tiers = objectMapper.readTree(rule.getCalcExpression()).get("tiers");
            long totalPoints = 0;
            BigDecimal remaining = bizAmount;

            for (JsonNode tier : tiers) {
                BigDecimal min = new BigDecimal(tier.get("min").asText());
                BigDecimal max = tier.has("max") && !tier.get("max").isNull()
                        ? new BigDecimal(tier.get("max").asText()) : null;
                BigDecimal rate = new BigDecimal(tier.get("rate").asText());

                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal tierAmount;
                if (max != null) {
                    BigDecimal tierRange = max.subtract(min);
                    tierAmount = remaining.min(tierRange);
                } else {
                    tierAmount = remaining;
                }

                if (bizAmount.compareTo(min) > 0) {
                    totalPoints += tierAmount.multiply(rate).setScale(0, RoundingMode.FLOOR).longValue();
                    remaining = remaining.subtract(tierAmount);
                }
            }
            return totalPoints;
        } catch (Exception e) {
            log.error("Failed to parse tiered rule expression: {}", rule.getCalcExpression(), e);
            return 0;
        }
    }

    private long calculateCustom(PointRule rule, BigDecimal bizAmount) {
        if (!StringUtils.hasText(rule.getCalcExpression())) return 0;
        try {
            JsonNode expr = objectMapper.readTree(rule.getCalcExpression());
            if (expr.has("fixedValue")) {
                return expr.get("fixedValue").asLong();
            }
            if (expr.has("formula") && bizAmount != null) {
                double multiplier = expr.has("multiplier") ? expr.get("multiplier").asDouble() : 1.0;
                double offset = expr.has("offset") ? expr.get("offset").asDouble() : 0.0;
                return (long) (bizAmount.doubleValue() * multiplier + offset);
            }
            return 0;
        } catch (Exception e) {
            log.error("Failed to parse custom rule expression: {}", rule.getCalcExpression(), e);
            return 0;
        }
    }
}
