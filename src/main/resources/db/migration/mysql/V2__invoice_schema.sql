-- =============================================
-- V2: 电子发票模块
-- =============================================

-- 订单表增加开票状态
ALTER TABLE t_billing_order ADD COLUMN invoice_status TINYINT NOT NULL DEFAULT 0;

-- 开票申请
CREATE TABLE t_invoice_application (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    application_no       VARCHAR(64)  NOT NULL UNIQUE,
    customer_id          VARCHAR(64)  NOT NULL,
    customer_name        VARCHAR(128),
    invoice_type         VARCHAR(20)  NOT NULL,
    title_name           VARCHAR(200) NOT NULL,
    tax_no               VARCHAR(30)  NOT NULL,
    bank_name            VARCHAR(128),
    bank_account         VARCHAR(64),
    company_address      VARCHAR(256),
    company_phone        VARCHAR(32),
    receiver_email       VARCHAR(128) NOT NULL,
    remark               VARCHAR(512),
    item_mode            VARCHAR(20)  NOT NULL DEFAULT 'DEFAULT',
    total_amount         BIGINT       NOT NULL,
    tax_rate             DECIMAL(5,2) NOT NULL DEFAULT 0.06,
    tax_amount           BIGINT,
    amount_without_tax   BIGINT,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reviewer_id          VARCHAR(64),
    reviewer_name        VARCHAR(128),
    review_time          DATETIME,
    reject_reason        VARCHAR(512),
    invoice_code         VARCHAR(30),
    invoice_number       VARCHAR(20),
    invoice_date         DATE,
    pdf_url              VARCHAR(512),
    pdf_local_path       VARCHAR(512),
    api_request_id       VARCHAR(64),
    api_response         TEXT,
    void_reason          VARCHAR(512),
    void_time            DATETIME,
    void_invoice_number  VARCHAR(20),
    email_sent           TINYINT      NOT NULL DEFAULT 0,
    email_sent_at        DATETIME,
    sms_sent             TINYINT      NOT NULL DEFAULT 0,
    sms_sent_at          DATETIME,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_inv_customer (tenant_id, customer_id, status),
    INDEX idx_inv_status (tenant_id, status, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 开票申请-订单关联
CREATE TABLE t_invoice_application_order (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id       BIGINT       NOT NULL,
    order_id             BIGINT       NOT NULL,
    order_no             VARCHAR(64)  NOT NULL,
    order_amount         BIGINT       NOT NULL,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_app_order (application_id, order_id),
    INDEX idx_inv_order (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
