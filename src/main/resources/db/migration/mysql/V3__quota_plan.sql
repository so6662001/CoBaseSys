-- =============================================
-- V3: 月费配额套餐 (QUOTA_PLAN)
-- =============================================

-- 定价方案表增加配额配置字段
ALTER TABLE t_billing_pricing_plan ADD COLUMN quota_config JSON;

-- 订阅表增加配额配置快照
ALTER TABLE t_billing_subscription ADD COLUMN quota_config JSON;

-- 订阅配额用量跟踪表 (每个维度独立跟踪)
CREATE TABLE t_billing_subscription_quota (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    subscription_id      BIGINT       NOT NULL,
    customer_id          VARCHAR(64)  NOT NULL,
    dimension            VARCHAR(64)  NOT NULL,
    dimension_label      VARCHAR(128),
    quota_limit          BIGINT       NOT NULL DEFAULT 0,
    quota_used           BIGINT       NOT NULL DEFAULT 0,
    period_start         DATE         NOT NULL,
    period_end           DATE         NOT NULL,
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sub_dim_period (subscription_id, dimension, period_start),
    INDEX idx_quota_customer (tenant_id, customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
