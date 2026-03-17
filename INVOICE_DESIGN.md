# CoBaseSys 电子发票模块 — 系统设计方案

## 一、模块定位

电子发票模块为 CoBaseSys 提供**在线开票申请、财务审核、数电发票开具、发票下载、邮件通知**的完整开票闭环。与计费模块的订单系统深度集成，对接新时代数电开票 API 完成发票开具。

## 二、业务流程

```
客户付款完成
  │
  ├→ 客户进入"我的发票"页面
  │   ├→ 查看已付款且未申请开票的订单列表
  │   ├→ 选择一个或多个订单
  │   ├→ 填写开票信息:
  │   │   ├─ 发票类型: 增值税普通发票 / 增值税专用发票
  │   │   ├─ 抬头名称 (公司名称)
  │   │   ├─ 纳税人识别号
  │   │   ├─ 开户银行 + 账号 (专票必填)
  │   │   ├─ 公司地址 + 电话 (专票必填)
  │   │   ├─ 接收邮箱
  │   │   └─ 备注
  │   └→ 提交开票申请 (status=PENDING)
  │
  ├→ 财务后台审核:
  │   ├→ 查看待审核开票申请列表
  │   ├→ 审核开票信息是否正确
  │   ├→ [审核通过]:
  │   │   ├→ 调用新时代数电开票 API
  │   │   ├→ 获取发票代码/号码/PDF下载地址
  │   │   ├→ 更新开票申请状态 (status=ISSUED)
  │   │   ├→ 保存发票文件信息
  │   │   └→ 发送邮件通知客户 (附PDF下载链接)
  │   └→ [审核驳回]:
  │       ├→ 填写驳回原因
  │       ├→ 更新状态 (status=REJECTED)
  │       └→ 通知客户修改后重新申请
  │
  ├→ 客户查看/下载发票:
  │   ├→ 在"我的发票"页面查看开票状态
  │   ├→ 已开具的发票显示发票号码/金额/PDF链接
  │   └→ 点击下载发票PDF
  │
  └→ 红冲/作废 (可选):
      ├→ 财务发起红冲
      ├→ 调用数电 API 红冲
      └→ 更新状态 (status=VOIDED)
```

## 三、数据模型

### 3.1 开票申请表 (`t_invoice_application`)

```
t_invoice_application (开票申请)
├── id                  : bigint, PK
├── tenant_id           : bigint, NOT NULL
├── application_no      : varchar(64), UK                -- 申请编号
├── customer_id         : varchar(64), NOT NULL
├── customer_name       : varchar(128)
│
│   ── 开票信息 ──
├── invoice_type        : varchar(20), NOT NULL           -- NORMAL(普通发票) / SPECIAL(专用发票)
├── title_name          : varchar(200), NOT NULL          -- 发票抬头(公司名称)
├── tax_no              : varchar(30), NOT NULL           -- 纳税人识别号
├── bank_name           : varchar(128)                    -- 开户银行 (专票必填)
├── bank_account        : varchar(64)                     -- 银行账号 (专票必填)
├── company_address     : varchar(256)                    -- 公司地址 (专票必填)
├── company_phone       : varchar(32)                     -- 公司电话 (专票必填)
├── receiver_email      : varchar(128), NOT NULL          -- 接收邮箱
├── remark              : varchar(512)                    -- 备注
│
│   ── 开票明细方式 ──
├── item_mode           : varchar(20), default 'DEFAULT'  -- DEFAULT(技术服务费) / FROM_ORDER(从订单带入)
│
│   ── 金额 ──
├── total_amount        : bigint, NOT NULL                -- 开票总金额(分)
├── tax_rate            : decimal(5,2), default 0.06      -- 税率 (如 0.06 = 6%)
├── tax_amount          : bigint                          -- 税额(分)
├── amount_without_tax  : bigint                          -- 不含税金额(分)
│
│   ── 状态 ──
├── status              : varchar(20), NOT NULL
│   -- PENDING    : 待审核
│   -- APPROVED   : 审核通过，开票中
│   -- ISSUED     : 已开具
│   -- REJECTED   : 已驳回
│   -- VOIDED     : 已红冲/作废
│
│   ── 审核信息 ──
├── reviewer_id         : varchar(64)
├── reviewer_name       : varchar(128)
├── review_time         : datetime
├── reject_reason       : varchar(512)
│
│   ── 发票信息 (开具后填充) ──
├── invoice_code        : varchar(30)                     -- 发票代码
├── invoice_number      : varchar(20)                     -- 发票号码
├── invoice_date        : date                            -- 开票日期
├── pdf_url             : varchar(512)                    -- 发票PDF下载地址(数电平台)
├── pdf_local_path      : varchar(512)                    -- 发票PDF本地/OSS路径(备份)
├── api_request_id      : varchar(64)                     -- 数电API请求ID
├── api_response        : text                            -- 数电API返回原文
│
│   ── 红冲/作废 ──
├── void_reason         : varchar(512)                    -- 红冲/作废原因
├── void_time           : datetime                        -- 红冲时间
├── void_invoice_number : varchar(20)                     -- 红字发票号码
│
│   ── 通知 ──
├── email_sent          : tinyint, default 0              -- 是否已发送邮件
├── email_sent_at       : datetime
├── sms_sent            : tinyint, default 0              -- 是否已发送短信
├── sms_sent_at         : datetime
│
├── created_at          : datetime
└── updated_at          : datetime
  (Index: tenant_id + customer_id + status)
  (Index: tenant_id + status + created_at DESC)
```

### 3.2 开票申请-订单关联表 (`t_invoice_application_order`)

一次开票申请可以关联多个已付款订单。

```
t_invoice_application_order (开票申请-订单关联)
├── id                  : bigint, PK
├── application_id      : bigint, FK -> t_invoice_application
├── order_id            : bigint, NOT NULL                -- 关联订单ID
├── order_no            : varchar(64), NOT NULL           -- 订单号(冗余)
├── order_amount        : bigint, NOT NULL                -- 该订单金额(分)
├── created_at          : datetime
  (UK: application_id + order_id)
```

### 3.3 订单表扩展

在 `t_billing_order` 增加开票状态字段：

```sql
ALTER TABLE t_billing_order ADD COLUMN invoice_status TINYINT NOT NULL DEFAULT 0;
-- 0: 未申请  1: 已申请  2: 已开票
```

## 四、新时代数电开票 API 对接

### 4.1 对接架构

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  CoBaseSys   │────→│ InvoiceService│────→│ 新时代数电API │
│  财务审核通过  │     │  封装调用层    │     │  开具发票     │
└──────────────┘     └──────────────┘     └──────────────┘
                           │                      │
                           │←─────────────────────│
                           │  返回: 发票代码/号码    │
                           │        PDF下载地址     │
```

### 4.2 配置

```yaml
cobasesys:
  invoice:
    api-url: https://api.xsdapi.com/v1/invoice
    app-key: xxx
    app-secret: xxx
    seller-name: "您的公司名称"
    seller-tax-no: "91xxxxxxxxxxxxx"
    seller-address: "公司地址"
    seller-phone: "公司电话"
    seller-bank-name: "开户银行"
    seller-bank-account: "银行账号"
    default-tax-rate: 0.06
    callback-url: https://your-domain.com/api/v1/invoice/callback
```

### 4.3 核心接口调用

| 接口 | 说明 |
|------|------|
| 开具发票 | 传入买方信息+商品明细+金额，返回发票代码/号码/PDF |
| 查询发票 | 根据请求ID查询开票结果 |
| 红冲发票 | 传入原发票信息，开具红字发票 |

## 五、API 设计

### 5.1 客户端 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/invoice/available-orders` | 可开票的订单列表（已付款+未申请开票） |
| POST | `/api/v1/invoice/apply` | 提交开票申请 |
| GET | `/api/v1/invoice/my` | 我的开票申请列表 |
| GET | `/api/v1/invoice/my/{id}` | 开票申请详情（含发票信息） |
| GET | `/api/v1/invoice/download/{id}` | 下载发票PDF |

### 5.2 管理后台 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/invoice/applications` | 开票申请列表（按状态筛选） |
| GET | `/admin/invoice/applications/{id}` | 申请详情 |
| POST | `/admin/invoice/applications/{id}/approve` | 审核通过（触发数电API开票） |
| POST | `/admin/invoice/applications/{id}/reject` | 审核驳回 |
| POST | `/admin/invoice/applications/{id}/void` | 红冲/作废 |
| POST | `/admin/invoice/applications/{id}/resend-email` | 重发邮件通知 |
| GET | `/admin/invoice/statistics` | 开票统计（总金额/数量/按月汇总） |

### 5.3 数电API回调

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/invoice/callback` | 数电API异步回调（开票结果通知） |

## 六、邮件通知

### 6.1 开票成功邮件模板

```
主题: 您的电子发票已开具 - {invoiceNumber}

尊敬的 {customerName}:

您申请的电子发票已成功开具，详情如下：

  发票类型：{invoiceType}
  发票号码：{invoiceNumber}
  开票日期：{invoiceDate}
  发票金额：{totalAmount} 元

请点击以下链接下载发票PDF文件：
  {pdfDownloadUrl}

如有任何问题，请联系我们的客服。

CoBaseSys 运营团队
```

### 6.2 审核驳回邮件模板

```
主题: 您的开票申请未通过 - 请修改后重新提交

尊敬的 {customerName}:

您提交的开票申请（编号: {applicationNo}）未通过审核。

驳回原因：{rejectReason}

请修改开票信息后重新提交申请。

CoBaseSys 运营团队
```

## 七、项目结构

```
module/invoice/
├── entity/
│   ├── InvoiceApplication.java
│   └── InvoiceApplicationOrder.java
├── repository/
│   ├── InvoiceApplicationRepository.java
│   └── InvoiceApplicationOrderRepository.java
├── service/
│   ├── InvoiceService.java              // 开票申请/审核/开具
│   ├── InvoiceApiClient.java            // 新时代数电API封装
│   └── InvoiceEmailService.java         // 发票邮件通知
├── controller/
│   ├── admin/InvoiceAdminController.java
│   └── api/InvoiceCustomerController.java
├── dto/
│   └── InvoiceDTO.java
└── config/
    └── InvoiceConfig.java               // 数电API配置
```

## 八、前端页面

### 8.1 管理后台（Element Plus）

| 页面 | 功能 |
|------|------|
| 开票审核 | 待审核/已开票/已驳回 Tab + 审核通过(触发开票)/驳回 + 发票信息展示 |
| 开票统计 | 总开票金额/数量/按月统计图表 |

### 8.2 客户前台（管理后台暂代）

| 页面 | 功能 |
|------|------|
| 申请开票 | 选择可开票订单 + 填写开票信息表单 |
| 我的发票 | 申请列表 + 状态跟踪 + PDF下载 |

## 九、开票频率控制

每个客户限制开票频率，防止高频恶意申请：

```yaml
cobasesys:
  invoice:
    rate-limit:
      max-per-day: 5          # 每天最多申请5次
      min-interval-minutes: 30 # 两次申请间隔至少30分钟
```

- 提交申请时校验：今日已申请次数 < max-per-day
- 校验：距上次申请时间 >= min-interval-minutes
- 不限制最低开票金额

---

## 十、确认清单（全部已确认 ✅）

- [x] **数电平台**：使用新时代数电开票API，扫码登录获取凭证
- [x] **发票类型**：增值税普通发票 + 增值税专用发票
- [x] **税率**：默认6%
- [x] **开票明细**：默认"技术服务费"，财务审核时可选择是否从订单明细带入
- [x] **合并开票**：支持多个订单合并开一张票
- [x] **红冲/作废**：一期实现
- [x] **通知方式**：邮件通知（附PDF链接）+ 腾讯云短信通知
- [x] **PDF存储**：双存储 — 数电平台原始链接 + 下载到我方服务器/OSS备份
- [x] **开票频率**：限制每日申请次数和申请间隔，不限最低金额

**所有确认项已通过，可以开始编码。**
