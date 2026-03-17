-- =============================================
-- CoBaseSys Billing Module - MySQL Schema
-- =============================================

-- 产品
CREATE TABLE t_billing_product (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    product_code         VARCHAR(64)  NOT NULL,
    product_name         VARCHAR(200) NOT NULL,
    category             VARCHAR(64),
    description          TEXT,
    icon_url             VARCHAR(512),
    pricing_model        VARCHAR(30)  NOT NULL,
    trial_enabled        TINYINT      NOT NULL DEFAULT 0,
    trial_days           INT          NOT NULL DEFAULT 0,
    trial_extend_enabled TINYINT      NOT NULL DEFAULT 0,
    trial_max_extend_days INT         NOT NULL DEFAULT 0,
    points_payable       TINYINT      NOT NULL DEFAULT 0,
    max_points_ratio     DECIMAL(5,2) NOT NULL DEFAULT 0,
    points_exchange_rate DECIMAL(12,4) NOT NULL DEFAULT 0,
    sort_order           INT          NOT NULL DEFAULT 0,
    status               TINYINT      NOT NULL DEFAULT 1,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_product_code (tenant_id, product_code),
    INDEX idx_product_tenant (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 套餐
CREATE TABLE t_billing_package (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    package_code         VARCHAR(64)  NOT NULL,
    package_name         VARCHAR(200) NOT NULL,
    description          TEXT,
    icon_url             VARCHAR(512),
    pricing_model        VARCHAR(30)  NOT NULL,
    trial_enabled        TINYINT      NOT NULL DEFAULT 0,
    trial_days           INT          NOT NULL DEFAULT 0,
    trial_extend_enabled TINYINT      NOT NULL DEFAULT 0,
    trial_max_extend_days INT         NOT NULL DEFAULT 0,
    points_payable       TINYINT      NOT NULL DEFAULT 0,
    max_points_ratio     DECIMAL(5,2) NOT NULL DEFAULT 0,
    points_exchange_rate DECIMAL(12,4) NOT NULL DEFAULT 0,
    sort_order           INT          NOT NULL DEFAULT 0,
    status               TINYINT      NOT NULL DEFAULT 1,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_package_code (tenant_id, package_code),
    INDEX idx_package_tenant (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 套餐明细
CREATE TABLE t_billing_package_item (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id           BIGINT       NOT NULL,
    product_id           BIGINT       NOT NULL,
    quantity             INT          NOT NULL,
    sort_order           INT          NOT NULL DEFAULT 0,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pkg_item (package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 定价方案
CREATE TABLE t_billing_pricing_plan (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    target_type          VARCHAR(10)  NOT NULL,
    target_id            BIGINT       NOT NULL,
    plan_name            VARCHAR(128) NOT NULL,
    pricing_model        VARCHAR(30)  NOT NULL,
    software_fee         BIGINT       NOT NULL DEFAULT 0,
    annual_service_fee   BIGINT       NOT NULL DEFAULT 0,
    period_type          VARCHAR(10),
    period_price         BIGINT       NOT NULL DEFAULT 0,
    included_quantity    INT          NOT NULL DEFAULT 0,
    overage_unit_name    VARCHAR(32),
    overage_unit_price   BIGINT       NOT NULL DEFAULT 0,
    unit_name            VARCHAR(32),
    unit_price           BIGINT       NOT NULL DEFAULT 0,
    tiered_pricing       JSON,
    rental_period_type   VARCHAR(10),
    rental_price         BIGINT       NOT NULL DEFAULT 0,
    space_unit           VARCHAR(10),
    space_unit_price     BIGINT       NOT NULL DEFAULT 0,
    one_time_price       BIGINT       NOT NULL DEFAULT 0,
    validity_days        INT,
    priority             INT          NOT NULL DEFAULT 0,
    effective_from       DATETIME     NOT NULL,
    effective_to         DATETIME,
    status               TINYINT      NOT NULL DEFAULT 1,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pricing_target (target_type, target_id, status, effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 折扣规则
CREATE TABLE t_billing_discount_rule (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    rule_name            VARCHAR(128) NOT NULL,
    discount_type        VARCHAR(20)  NOT NULL,
    target_type          VARCHAR(10),
    target_id            BIGINT,
    min_quantity         INT          NOT NULL DEFAULT 1,
    discount_rate        DECIMAL(5,2),
    threshold_amount     BIGINT       NOT NULL DEFAULT 0,
    bonus_points         BIGINT       NOT NULL DEFAULT 0,
    effective_from       DATETIME,
    effective_to         DATETIME,
    priority             INT          NOT NULL DEFAULT 0,
    status               TINYINT      NOT NULL DEFAULT 1,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_discount_tenant (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 赠送规则
CREATE TABLE t_billing_gift_rule (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    rule_name            VARCHAR(128) NOT NULL,
    condition_type       VARCHAR(20)  NOT NULL,
    condition_target_type VARCHAR(10),
    condition_target_id  BIGINT,
    condition_quantity   INT          NOT NULL DEFAULT 1,
    gift_type            VARCHAR(10)  NOT NULL,
    gift_target_id       BIGINT,
    gift_quantity        INT          NOT NULL DEFAULT 1,
    gift_points          BIGINT       NOT NULL DEFAULT 0,
    gift_validity_days   INT,
    effective_from       DATETIME,
    effective_to         DATETIME,
    status               TINYINT      NOT NULL DEFAULT 1,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_gift_tenant (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单主表
CREATE TABLE t_billing_order (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    order_no             VARCHAR(64)  NOT NULL UNIQUE,
    customer_id          VARCHAR(64)  NOT NULL,
    customer_name        VARCHAR(128),
    order_type           VARCHAR(20)  NOT NULL,
    order_source         VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    operator_id          VARCHAR(64),
    operator_name        VARCHAR(128),
    total_amount         BIGINT       NOT NULL,
    discount_amount      BIGINT       NOT NULL DEFAULT 0,
    gift_amount          BIGINT       NOT NULL DEFAULT 0,
    points_deduct_amount BIGINT       NOT NULL DEFAULT 0,
    points_used          BIGINT       NOT NULL DEFAULT 0,
    actual_amount        BIGINT       NOT NULL,
    payment_status       TINYINT      NOT NULL DEFAULT 0,
    payment_method       VARCHAR(32),
    payment_no           VARCHAR(128),
    paid_at              DATETIME,
    remark               VARCHAR(512),
    status               TINYINT      NOT NULL DEFAULT 0,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_customer (tenant_id, customer_id, created_at DESC),
    INDEX idx_order_status (tenant_id, payment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单明细
CREATE TABLE t_billing_order_item (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id             BIGINT       NOT NULL,
    item_type            VARCHAR(10)  NOT NULL,
    item_id              BIGINT       NOT NULL,
    item_name            VARCHAR(200),
    pricing_plan_id      BIGINT,
    pricing_model        VARCHAR(30),
    quantity             INT          NOT NULL,
    unit_price           BIGINT,
    original_amount      BIGINT,
    discount_amount      BIGINT       NOT NULL DEFAULT 0,
    actual_amount        BIGINT,
    period_type          VARCHAR(10),
    period_count         INT          NOT NULL DEFAULT 1,
    start_date           DATE,
    end_date             DATE,
    is_gift              TINYINT      NOT NULL DEFAULT 0,
    gift_rule_id         BIGINT,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_item (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 客户订阅
CREATE TABLE t_billing_subscription (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    customer_id          VARCHAR(64)  NOT NULL,
    subscription_no      VARCHAR(64)  NOT NULL UNIQUE,
    source_type          VARCHAR(10)  NOT NULL,
    source_id            BIGINT       NOT NULL,
    source_name          VARCHAR(200),
    order_id             BIGINT,
    order_item_id        BIGINT,
    pricing_model        VARCHAR(30),
    pricing_plan_id      BIGINT,
    quantity             INT          NOT NULL DEFAULT 1,
    usage_quota          BIGINT       NOT NULL DEFAULT 0,
    usage_used           BIGINT       NOT NULL DEFAULT 0,
    usage_unit           VARCHAR(32),
    space_total          BIGINT       NOT NULL DEFAULT 0,
    space_used           BIGINT       NOT NULL DEFAULT 0,
    total_days           INT          NOT NULL DEFAULT 0,
    days_used            INT          NOT NULL DEFAULT 0,
    status               VARCHAR(20)  NOT NULL,
    is_trial             TINYINT      NOT NULL DEFAULT 0,
    start_date           DATE         NOT NULL,
    end_date             DATE         NOT NULL,
    auto_renew           TINYINT      NOT NULL DEFAULT 0,
    original_unit_price  BIGINT,
    renewal_price        BIGINT,
    price_locked_until   DATE,
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sub_customer (tenant_id, customer_id, status),
    INDEX idx_sub_expiry (tenant_id, status, end_date),
    INDEX idx_sub_source (source_type, source_id, customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 续费提醒
CREATE TABLE t_billing_renewal_reminder (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    subscription_id      BIGINT       NOT NULL,
    customer_id          VARCHAR(64)  NOT NULL,
    reminder_type        VARCHAR(20)  NOT NULL,
    channel              VARCHAR(20)  NOT NULL,
    content              TEXT,
    recipient            VARCHAR(128),
    status               TINYINT      NOT NULL DEFAULT 0,
    sent_at              DATETIME,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_reminder_dedup (subscription_id, reminder_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 试用记录
CREATE TABLE t_billing_trial (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    customer_id          VARCHAR(64)  NOT NULL,
    source_type          VARCHAR(10)  NOT NULL,
    source_id            BIGINT       NOT NULL,
    source_name          VARCHAR(200),
    subscription_id      BIGINT,
    trial_days           INT          NOT NULL,
    start_date           DATE         NOT NULL,
    end_date             DATE         NOT NULL,
    status               VARCHAR(20)  NOT NULL,
    converted_order_id   BIGINT,
    contact_info         VARCHAR(512),
    extend_count         INT          NOT NULL DEFAULT 0,
    total_extend_days    INT          NOT NULL DEFAULT 0,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_trial (tenant_id, customer_id, source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用量流水
CREATE TABLE t_billing_usage_ledger (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    subscription_id      BIGINT       NOT NULL,
    customer_id          VARCHAR(64)  NOT NULL,
    action               VARCHAR(20)  NOT NULL,
    quantity             BIGINT       NOT NULL,
    unit                 VARCHAR(32)  NOT NULL,
    balance_before       BIGINT       NOT NULL,
    balance_after        BIGINT       NOT NULL,
    unit_price           BIGINT       NOT NULL DEFAULT 0,
    amount               BIGINT       NOT NULL DEFAULT 0,
    biz_system           VARCHAR(64),
    biz_order_no         VARCHAR(128),
    biz_description      VARCHAR(512),
    idempotent_key       VARCHAR(128) NOT NULL UNIQUE,
    data_hash            VARCHAR(64)  NOT NULL DEFAULT '',
    prev_hash            VARCHAR(64)  NOT NULL DEFAULT '',
    chain_hash           VARCHAR(64)  NOT NULL DEFAULT '',
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_usage_sub (subscription_id, created_at DESC),
    INDEX idx_usage_customer (tenant_id, customer_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 试用延长审批
CREATE TABLE t_billing_trial_extend_approval (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    trial_id             BIGINT       NOT NULL,
    subscription_id      BIGINT       NOT NULL,
    customer_id          VARCHAR(64)  NOT NULL,
    customer_name        VARCHAR(128),
    source_name          VARCHAR(200),
    extend_days          INT          NOT NULL,
    apply_reason         VARCHAR(512),
    applicant_id         VARCHAR(64)  NOT NULL,
    applicant_name       VARCHAR(128),
    apply_time           DATETIME     NOT NULL,
    status               VARCHAR(20)  NOT NULL,
    approver_id          VARCHAR(64),
    approver_name        VARCHAR(128),
    approve_time         DATETIME,
    approve_remark       VARCHAR(512),
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_extend_status (tenant_id, status),
    INDEX idx_extend_approver (tenant_id, approver_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 流水表禁止UPDATE/DELETE触发器
DELIMITER //
CREATE TRIGGER trg_usage_ledger_no_update BEFORE UPDATE ON t_billing_usage_ledger
FOR EACH ROW BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SECURITY: Usage ledger cannot be modified';
END //
CREATE TRIGGER trg_usage_ledger_no_delete BEFORE DELETE ON t_billing_usage_ledger
FOR EACH ROW BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SECURITY: Usage ledger cannot be deleted';
END //
DELIMITER ;
