-- =============================================
-- V3: 积分赠送功能 - 流水来源分类 + 赠送审批
-- =============================================

-- 积分流水增加来源类型字段
ALTER TABLE t_point_transaction ADD COLUMN IF NOT EXISTS source_type VARCHAR(20) DEFAULT 'EARNED'
    NOT NULL;
-- source_type: EARNED(规则赚取) / GIFT_MANUAL(人工赠送) / GIFT_ORDER(订单赠送) / GIFT_ACTIVITY(活动赠送) / SYSTEM(系统调整)

COMMENT ON COLUMN t_point_transaction.source_type IS '来源类型: EARNED/GIFT_MANUAL/GIFT_ORDER/GIFT_ACTIVITY/SYSTEM';

CREATE INDEX IF NOT EXISTS idx_point_tx_source ON t_point_transaction(tenant_id, source_type, created_at DESC);

-- 积分赠送审批表
CREATE TABLE t_point_gift_approval (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    customer_id      VARCHAR(64)  NOT NULL,
    customer_name    VARCHAR(128),
    points           BIGINT       NOT NULL,
    gift_reason      VARCHAR(512) NOT NULL,
    source_type      VARCHAR(20)  NOT NULL DEFAULT 'GIFT_MANUAL',

    applicant_id     VARCHAR(64)  NOT NULL,
    applicant_name   VARCHAR(128),
    apply_time       TIMESTAMP    NOT NULL,

    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    approver_id      VARCHAR(64),
    approver_name    VARCHAR(128),
    approve_time     TIMESTAMP,
    approve_remark   VARCHAR(512),

    transaction_no   VARCHAR(64),

    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_gift_approval_tenant ON t_point_gift_approval(tenant_id, status);
CREATE INDEX idx_gift_approval_applicant ON t_point_gift_approval(tenant_id, applicant_id);
