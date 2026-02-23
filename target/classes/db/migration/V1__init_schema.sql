-- =============================================
-- CoBaseSys Database Schema
-- V1: Initial schema creation
-- =============================================

-- Tenant
CREATE TABLE t_tenant (
    id              BIGSERIAL PRIMARY KEY,
    tenant_code     VARCHAR(64)  NOT NULL UNIQUE,
    tenant_name     VARCHAR(128) NOT NULL,
    contact_name    VARCHAR(64),
    contact_phone   VARCHAR(32),
    contact_email   VARCHAR(128),
    status          SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- External System
CREATE TABLE t_external_system (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    system_code     VARCHAR(64)  NOT NULL,
    system_name     VARCHAR(128) NOT NULL,
    description     TEXT,
    app_key         VARCHAR(64)  NOT NULL UNIQUE,
    app_secret      VARCHAR(128) NOT NULL,
    callback_url    VARCHAR(512),
    ip_whitelist    TEXT,
    rate_limit      INT          NOT NULL DEFAULT 1000,
    status          SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, system_code)
);
CREATE INDEX idx_external_system_tenant ON t_external_system(tenant_id);

-- ===================== POINTS MODULE =====================

CREATE TABLE t_point_action (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    system_id       BIGINT       NOT NULL,
    action_code     VARCHAR(64)  NOT NULL,
    action_name     VARCHAR(128) NOT NULL,
    description     TEXT,
    direction       SMALLINT     NOT NULL,
    status          SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, system_id, action_code)
);
CREATE INDEX idx_point_action_system ON t_point_action(system_id);

CREATE TABLE t_point_rule (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    action_id       BIGINT       NOT NULL,
    rule_name       VARCHAR(128) NOT NULL,
    calc_type       VARCHAR(20)  NOT NULL,
    calc_value      DECIMAL(12,2),
    calc_expression TEXT,
    min_points      INT,
    max_points      INT,
    daily_limit     INT,
    monthly_limit   INT,
    effective_from  TIMESTAMP    NOT NULL,
    effective_to    TIMESTAMP,
    priority        INT          NOT NULL DEFAULT 0,
    status          SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_point_rule_action ON t_point_rule(action_id);

CREATE TABLE t_point_account (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    user_id         VARCHAR(64)  NOT NULL,
    total_earned    BIGINT       NOT NULL DEFAULT 0,
    total_consumed  BIGINT       NOT NULL DEFAULT 0,
    balance         BIGINT       NOT NULL DEFAULT 0,
    frozen          BIGINT       NOT NULL DEFAULT 0,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, user_id)
);
CREATE INDEX idx_point_account_tenant ON t_point_account(tenant_id);

CREATE TABLE t_point_transaction (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    transaction_no  VARCHAR(64)  NOT NULL UNIQUE,
    account_id      BIGINT       NOT NULL,
    system_id       BIGINT       NOT NULL,
    action_id       BIGINT       NOT NULL,
    rule_id         BIGINT,
    direction       SMALLINT     NOT NULL,
    points          BIGINT       NOT NULL,
    balance_before  BIGINT       NOT NULL,
    balance_after   BIGINT       NOT NULL,
    biz_order_no    VARCHAR(128),
    biz_amount      DECIMAL(12,2),
    remark          VARCHAR(512),
    idempotent_key  VARCHAR(128) NOT NULL UNIQUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_point_tx_account ON t_point_transaction(account_id, created_at DESC);
CREATE INDEX idx_point_tx_tenant ON t_point_transaction(tenant_id, created_at DESC);

-- ===================== WALLET MODULE =====================

CREATE TABLE t_wallet_account (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    user_id         VARCHAR(64)  NOT NULL,
    total_recharged BIGINT       NOT NULL DEFAULT 0,
    total_consumed  BIGINT       NOT NULL DEFAULT 0,
    balance         BIGINT       NOT NULL DEFAULT 0,
    frozen          BIGINT       NOT NULL DEFAULT 0,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, user_id)
);
CREATE INDEX idx_wallet_account_tenant ON t_wallet_account(tenant_id);

CREATE TABLE t_consume_action (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    system_id       BIGINT       NOT NULL,
    action_code     VARCHAR(64)  NOT NULL,
    action_name     VARCHAR(128) NOT NULL,
    description     TEXT,
    status          SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, system_id, action_code)
);

CREATE TABLE t_consume_rule (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    action_id       BIGINT       NOT NULL,
    rule_name       VARCHAR(128) NOT NULL,
    calc_type       VARCHAR(20)  NOT NULL,
    calc_value      DECIMAL(12,4),
    calc_expression TEXT,
    unit_name       VARCHAR(32),
    min_charge      INT,
    max_charge      INT,
    free_quota      INT          NOT NULL DEFAULT 0,
    effective_from  TIMESTAMP    NOT NULL,
    effective_to    TIMESTAMP,
    priority        INT          NOT NULL DEFAULT 0,
    status          SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_consume_rule_action ON t_consume_rule(action_id);

CREATE TABLE t_recharge_order (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    order_no        VARCHAR(64)  NOT NULL UNIQUE,
    account_id      BIGINT       NOT NULL,
    user_id         VARCHAR(64)  NOT NULL,
    amount          BIGINT       NOT NULL,
    actual_amount   BIGINT       NOT NULL,
    gift_amount     BIGINT       NOT NULL DEFAULT 0,
    payment_method  VARCHAR(32),
    payment_no      VARCHAR(128),
    status          SMALLINT     NOT NULL DEFAULT 0,
    paid_at         TIMESTAMP,
    remark          VARCHAR(512),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_recharge_order_account ON t_recharge_order(account_id, created_at DESC);

CREATE TABLE t_wallet_transaction (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    transaction_no  VARCHAR(64)  NOT NULL UNIQUE,
    account_id      BIGINT       NOT NULL,
    type            SMALLINT     NOT NULL,
    system_id       BIGINT,
    action_id       BIGINT,
    rule_id         BIGINT,
    amount          BIGINT       NOT NULL,
    balance_before  BIGINT       NOT NULL,
    balance_after   BIGINT       NOT NULL,
    biz_order_no    VARCHAR(128),
    biz_quantity    DECIMAL(12,4),
    remark          VARCHAR(512),
    idempotent_key  VARCHAR(128) NOT NULL UNIQUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_wallet_tx_account ON t_wallet_transaction(account_id, created_at DESC);
CREATE INDEX idx_wallet_tx_tenant ON t_wallet_transaction(tenant_id, created_at DESC);

CREATE TABLE t_recharge_promotion (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    name            VARCHAR(128) NOT NULL,
    min_amount      BIGINT       NOT NULL,
    gift_type       VARCHAR(20)  NOT NULL,
    gift_value      DECIMAL(12,2) NOT NULL,
    effective_from  TIMESTAMP    NOT NULL,
    effective_to    TIMESTAMP,
    status          SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ===================== MEMBER MODULE =====================

CREATE TABLE t_member_level (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    level_code      VARCHAR(32)  NOT NULL,
    level_name      VARCHAR(64)  NOT NULL,
    level_rank      INT          NOT NULL,
    min_points      BIGINT       NOT NULL DEFAULT 0,
    min_consumption BIGINT       NOT NULL DEFAULT 0,
    point_multiplier DECIMAL(5,2) NOT NULL DEFAULT 1.00,
    discount_rate   DECIMAL(5,2) NOT NULL DEFAULT 1.00,
    icon_url        VARCHAR(512),
    description     TEXT,
    status          SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, level_code)
);

CREATE TABLE t_user_member (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    user_id         VARCHAR(64)  NOT NULL,
    level_id        BIGINT       NOT NULL,
    total_points_earned BIGINT   NOT NULL DEFAULT 0,
    total_consumption   BIGINT   NOT NULL DEFAULT 0,
    level_updated_at TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, user_id)
);

-- ===================== NOTIFICATION MODULE =====================

CREATE TABLE t_notification_template (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    template_code   VARCHAR(64)  NOT NULL,
    template_name   VARCHAR(128) NOT NULL,
    channel         VARCHAR(32)  NOT NULL,
    subject         VARCHAR(256),
    content         TEXT         NOT NULL,
    status          SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE t_notification_rule (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    rule_name       VARCHAR(128) NOT NULL,
    trigger_type    VARCHAR(32)  NOT NULL,
    threshold_value BIGINT,
    channel         VARCHAR(32)  NOT NULL,
    template_id     BIGINT,
    status          SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE t_notification_record (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    user_id         VARCHAR(64)  NOT NULL,
    rule_id         BIGINT,
    channel         VARCHAR(32)  NOT NULL,
    subject         VARCHAR(256),
    content         TEXT,
    recipient       VARCHAR(256),
    status          SMALLINT     NOT NULL DEFAULT 0,
    error_message   VARCHAR(512),
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notification_record_user ON t_notification_record(tenant_id, user_id, created_at DESC);

-- ===================== WEBHOOK MODULE =====================

CREATE TABLE t_webhook_config (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    system_id       BIGINT,
    webhook_name    VARCHAR(128) NOT NULL,
    url             VARCHAR(512) NOT NULL,
    secret          VARCHAR(128),
    events          TEXT         NOT NULL,
    status          SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_webhook_config_tenant ON t_webhook_config(tenant_id);

CREATE TABLE t_webhook_log (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    config_id       BIGINT       NOT NULL,
    event_type      VARCHAR(64)  NOT NULL,
    payload         TEXT,
    response_status INT,
    response_body   TEXT,
    status          SMALLINT     NOT NULL DEFAULT 0,
    retry_count     INT          NOT NULL DEFAULT 0,
    error_message   VARCHAR(1024),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_webhook_log_tenant ON t_webhook_log(tenant_id, created_at DESC);
CREATE INDEX idx_webhook_log_config ON t_webhook_log(config_id, created_at DESC);
CREATE INDEX idx_webhook_log_retry ON t_webhook_log(status, retry_count);

-- Insert default tenant
INSERT INTO t_tenant (tenant_code, tenant_name, status)
VALUES ('default', '默认租户', 1);
