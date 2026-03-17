-- V4: 长周期折扣支持
ALTER TABLE t_billing_discount_rule ADD COLUMN min_period_count INT;
