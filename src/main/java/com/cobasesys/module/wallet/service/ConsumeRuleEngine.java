package com.cobasesys.module.wallet.service;

import com.cobasesys.module.wallet.entity.ConsumeRule;
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
public class ConsumeRuleEngine {

    private final ObjectMapper objectMapper;

    /**
     * @return 消费金额（单位：分）
     */
    public long calculate(ConsumeRule rule, BigDecimal bizQuantity) {
        long amountInCents = switch (rule.getCalcType()) {
            case "fixed" -> calculateFixed(rule);
            case "unit_price" -> calculateUnitPrice(rule, bizQuantity);
            case "tiered" -> calculateTiered(rule, bizQuantity);
            case "custom" -> calculateCustom(rule, bizQuantity);
            default -> throw new IllegalArgumentException("Unknown calc type: " + rule.getCalcType());
        };

        if (rule.getMinCharge() != null) {
            amountInCents = Math.max(amountInCents, rule.getMinCharge());
        }
        if (rule.getMaxCharge() != null) {
            amountInCents = Math.min(amountInCents, rule.getMaxCharge());
        }

        return Math.max(amountInCents, 0);
    }

    private long calculateFixed(ConsumeRule rule) {
        return rule.getCalcValue()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.CEILING).longValue();
    }

    private long calculateUnitPrice(ConsumeRule rule, BigDecimal bizQuantity) {
        if (bizQuantity == null) return 0;
        BigDecimal effectiveQty = bizQuantity;
        if (rule.getFreeQuota() > 0) {
            effectiveQty = bizQuantity.subtract(BigDecimal.valueOf(rule.getFreeQuota()));
            if (effectiveQty.compareTo(BigDecimal.ZERO) <= 0) return 0;
        }
        return effectiveQty.multiply(rule.getCalcValue())
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.CEILING).longValue();
    }

    private long calculateTiered(ConsumeRule rule, BigDecimal bizQuantity) {
        if (bizQuantity == null || !StringUtils.hasText(rule.getCalcExpression())) return 0;
        try {
            JsonNode tiers = objectMapper.readTree(rule.getCalcExpression()).get("tiers");
            BigDecimal totalCost = BigDecimal.ZERO;
            BigDecimal remaining = bizQuantity;

            if (rule.getFreeQuota() > 0) {
                remaining = remaining.subtract(BigDecimal.valueOf(rule.getFreeQuota()));
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) return 0;
            }

            for (JsonNode tier : tiers) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal min = new BigDecimal(tier.get("min").asText());
                BigDecimal max = tier.has("max") && !tier.get("max").isNull()
                        ? new BigDecimal(tier.get("max").asText()) : null;
                BigDecimal price = new BigDecimal(tier.get("price").asText());

                BigDecimal tierQty;
                if (max != null) {
                    tierQty = remaining.min(max.subtract(min));
                } else {
                    tierQty = remaining;
                }
                totalCost = totalCost.add(tierQty.multiply(price));
                remaining = remaining.subtract(tierQty);
            }
            return totalCost.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.CEILING).longValue();
        } catch (Exception e) {
            log.error("Failed to parse tiered rule: {}", rule.getCalcExpression(), e);
            return 0;
        }
    }

    private long calculateCustom(ConsumeRule rule, BigDecimal bizQuantity) {
        if (!StringUtils.hasText(rule.getCalcExpression())) return 0;
        try {
            JsonNode expr = objectMapper.readTree(rule.getCalcExpression());
            if (expr.has("fixedCents")) {
                return expr.get("fixedCents").asLong();
            }
            if (expr.has("formula") && bizQuantity != null) {
                double multiplier = expr.has("multiplier") ? expr.get("multiplier").asDouble() : 1.0;
                double offset = expr.has("offset") ? expr.get("offset").asDouble() : 0.0;
                return (long) ((bizQuantity.doubleValue() * multiplier + offset) * 100);
            }
            return 0;
        } catch (Exception e) {
            log.error("Failed to parse custom rule: {}", rule.getCalcExpression(), e);
            return 0;
        }
    }
}
