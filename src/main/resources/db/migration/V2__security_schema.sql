-- =============================================
-- CoBaseSys Security Schema
-- V2: Admin users, RBAC, Audit, Reconciliation
-- =============================================

-- 管理员
CREATE TABLE t_admin_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL UNIQUE,
    password_hash   VARCHAR(256) NOT NULL,
    real_name       VARCHAR(64),
    phone           VARCHAR(32),
    email           VARCHAR(128),
    mfa_enabled     SMALLINT     NOT NULL DEFAULT 0,
    mfa_secret      VARCHAR(128),
    status          SMALLINT     NOT NULL DEFAULT 1,
    login_fail_count INT         NOT NULL DEFAULT 0,
    locked_until    TIMESTAMP,
    last_login_at   TIMESTAMP,
    last_login_ip   VARCHAR(45),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 角色
CREATE TABLE t_admin_role (
    id              BIGSERIAL PRIMARY KEY,
    role_code       VARCHAR(32)  NOT NULL UNIQUE,
    role_name       VARCHAR(64)  NOT NULL,
    description     VARCHAR(256),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 用户-角色
CREATE TABLE t_admin_user_role (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    UNIQUE (user_id, role_id)
);

-- 权限
CREATE TABLE t_admin_permission (
    id              BIGSERIAL PRIMARY KEY,
    permission_code VARCHAR(128) NOT NULL UNIQUE,
    permission_name VARCHAR(128) NOT NULL,
    module          VARCHAR(32),
    action          VARCHAR(64),
    description     VARCHAR(256)
);

-- 角色-权限
CREATE TABLE t_admin_role_permission (
    id              BIGSERIAL PRIMARY KEY,
    role_id         BIGINT NOT NULL,
    permission_id   BIGINT NOT NULL,
    UNIQUE (role_id, permission_id)
);

-- 登录日志
CREATE TABLE t_admin_login_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT,
    username        VARCHAR(64),
    login_ip        VARCHAR(45),
    user_agent      VARCHAR(512),
    status          VARCHAR(10)  NOT NULL,
    fail_reason     VARCHAR(256),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_login_log_user ON t_admin_login_log(user_id, created_at DESC);

-- 审计日志
CREATE TABLE t_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    trace_id        VARCHAR(64),
    operator_type   VARCHAR(20),
    operator_id     VARCHAR(64),
    operator_name   VARCHAR(128),
    operator_ip     VARCHAR(45),
    user_agent      VARCHAR(512),
    module          VARCHAR(32),
    action          VARCHAR(64),
    target_type     VARCHAR(64),
    target_id       VARCHAR(64),
    description     VARCHAR(512),
    before_data     TEXT,
    after_data      TEXT,
    request_body    TEXT,
    status          VARCHAR(10),
    error_message   VARCHAR(512),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_module ON t_audit_log(tenant_id, module, created_at DESC);
CREATE INDEX idx_audit_operator ON t_audit_log(operator_id, created_at DESC);

-- 对账报告
CREATE TABLE t_reconciliation_report (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    report_date     DATE         NOT NULL,
    check_type      VARCHAR(30)  NOT NULL,
    target_table    VARCHAR(64),
    total_records   BIGINT,
    pass_count      BIGINT,
    fail_count      BIGINT,
    status          VARCHAR(20),
    fail_details    TEXT,
    executed_at     TIMESTAMP,
    duration_ms     BIGINT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 流水表增加哈希链字段
ALTER TABLE t_point_transaction ADD COLUMN IF NOT EXISTS data_hash VARCHAR(64) DEFAULT '';
ALTER TABLE t_point_transaction ADD COLUMN IF NOT EXISTS prev_hash VARCHAR(64) DEFAULT '';
ALTER TABLE t_point_transaction ADD COLUMN IF NOT EXISTS chain_hash VARCHAR(64) DEFAULT '';

ALTER TABLE t_wallet_transaction ADD COLUMN IF NOT EXISTS data_hash VARCHAR(64) DEFAULT '';
ALTER TABLE t_wallet_transaction ADD COLUMN IF NOT EXISTS prev_hash VARCHAR(64) DEFAULT '';
ALTER TABLE t_wallet_transaction ADD COLUMN IF NOT EXISTS chain_hash VARCHAR(64) DEFAULT '';

-- 账户表增加余额摘要字段
ALTER TABLE t_point_account ADD COLUMN IF NOT EXISTS balance_digest VARCHAR(64) DEFAULT '';
ALTER TABLE t_wallet_account ADD COLUMN IF NOT EXISTS balance_digest VARCHAR(64) DEFAULT '';

-- 初始化超级管理员 (密码: admin123, BCrypt)
INSERT INTO t_admin_user (username, password_hash, real_name, status)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6CQAkosYIuCMzLP/EiRp2IuHi', '超级管理员', 1);

-- 初始化角色
INSERT INTO t_admin_role (role_code, role_name, description) VALUES
('SUPER_ADMIN', '超级管理员', '全部权限'),
('OPS_MANAGER', '运营主管', '产品/套餐/定价/折扣/赠送管理, 订单查看, 报表查看, 试用延长审批'),
('SALES', '销售员', '代客下单, 订单查看, 客户订阅查看, 试用延长申请'),
('FINANCE', '财务', '订单确认付款, 调账操作, 报表查看, 对账报告'),
('SUPPORT', '客服', '客户订阅查看, 试用管理, 提醒记录查看'),
('AUDITOR', '审计员', '只读全部数据, 审计日志, 对账报告');

-- 关联超级管理员角色
INSERT INTO t_admin_user_role (user_id, role_id) VALUES (1, 1);

-- 初始化核心权限
INSERT INTO t_admin_permission (permission_code, permission_name, module, action) VALUES
('system:tenant:manage', '租户管理', 'system', 'manage'),
('system:external:manage', '外部系统管理', 'system', 'manage'),
('points:action:manage', '积分动作管理', 'points', 'manage'),
('points:rule:manage', '积分规则管理', 'points', 'manage'),
('points:account:view', '积分账户查看', 'points', 'view'),
('points:account:adjust', '积分调账', 'points', 'adjust'),
('wallet:action:manage', '消费动作管理', 'wallet', 'manage'),
('wallet:rule:manage', '消费规则管理', 'wallet', 'manage'),
('wallet:account:view', '钱包账户查看', 'wallet', 'view'),
('wallet:account:adjust', '钱包调账', 'wallet', 'adjust'),
('billing:product:manage', '产品管理', 'billing', 'manage'),
('billing:package:manage', '套餐管理', 'billing', 'manage'),
('billing:pricing:manage', '定价管理', 'billing', 'manage'),
('billing:discount:manage', '折扣管理', 'billing', 'manage'),
('billing:gift:manage', '赠送管理', 'billing', 'manage'),
('billing:order:view', '订单查看', 'billing', 'view'),
('billing:order:proxy', '代客下单', 'billing', 'proxy'),
('billing:order:confirm', '确认付款', 'billing', 'confirm'),
('billing:subscription:view', '订阅查看', 'billing', 'view'),
('billing:subscription:manage', '订阅管理', 'billing', 'manage'),
('billing:trial:manage', '试用管理', 'billing', 'manage'),
('billing:trial:approve', '试用延长审批', 'billing', 'approve'),
('billing:report:view', '报表查看', 'billing', 'view'),
('audit:log:view', '审计日志查看', 'audit', 'view'),
('audit:reconciliation:view', '对账报告查看', 'audit', 'view');

-- 超级管理员拥有全部权限
INSERT INTO t_admin_role_permission (role_id, permission_id)
SELECT 1, id FROM t_admin_permission;

-- 审计日志表保护：禁止修改和删除
CREATE OR REPLACE FUNCTION prevent_audit_modification() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'SECURITY: Audit log records cannot be modified or deleted';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_no_update BEFORE UPDATE ON t_audit_log
FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();

CREATE TRIGGER trg_audit_no_delete BEFORE DELETE ON t_audit_log
FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();

-- 对账报告表保护
CREATE TRIGGER trg_reconciliation_no_update BEFORE UPDATE ON t_reconciliation_report
FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();

CREATE TRIGGER trg_reconciliation_no_delete BEFORE DELETE ON t_reconciliation_report
FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();
