# CoBaseSys 公司运营底座系统 — 系统设计方案

## 一、系统总览

### 1.1 系统定位

CoBaseSys 是一套公司级运营底座系统，为公司旗下多个业务系统提供统一的 **积分服务** 和 **充值/余额服务**。各外部业务系统通过 API 接入底座，实现用户资产的统一管理，降低各系统重复建设成本，同时为客户提供一致的积分和余额体验。

### 1.2 总体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                      CoBaseSys 底座系统                          │
│  ┌─────────────┐  ┌─────────────────┐  ┌─────────────────────┐ │
│  │  统一认证网关  │  │   管理后台(Web)  │  │   统一用户中心      │ │
│  │  API Gateway │  │   Admin Portal  │  │   User Identity    │ │
│  └──────┬───────┘  └────────┬────────┘  └─────────┬──────────┘ │
│         │                   │                     │            │
│  ┌──────┴───────────────────┴─────────────────────┴──────────┐ │
│  │                     核心服务层                              │ │
│  │  ┌──────────────────┐       ┌──────────────────────────┐  │ │
│  │  │    积分服务        │       │     充值/余额服务         │  │ │
│  │  │  Points Service  │       │   Wallet/Balance Service │  │ │
│  │  └──────────────────┘       └──────────────────────────┘  │ │
│  │                                                           │ │
│  │  ┌──────────────────────────────────────────────────────┐ │ │
│  │  │              外部系统注册中心                          │ │ │
│  │  │           External System Registry                   │ │ │
│  │  └──────────────────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────────────────┘ │
│         │                                       │              │
│  ┌──────┴───────────────────────────────────────┴──────────┐   │
│  │                    数据存储层                             │   │
│  │   PostgreSQL / MySQL        Redis (缓存/限流)            │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
          │              │              │              │
   ┌──────┴───┐   ┌──────┴───┐   ┌──────┴───┐   ┌──────┴───┐
   │ 业务系统A │   │ 业务系统B │   │ 业务系统C │   │ 业务系统D │
   │  (电商)   │   │  (社区)   │   │  (工具)   │   │  (CRM)   │
   └──────────┘   └──────────┘   └──────────┘   └──────────┘
```

### 1.3 技术选型建议

| 层次 | 技术选型 | 说明 |
|------|---------|------|
| 后端框架 | Python + FastAPI | 高性能异步框架，自动生成 OpenAPI 文档 |
| 数据库 | PostgreSQL | 支持事务、JSONB、高可靠性 |
| 缓存 | Redis | 余额/积分缓存、分布式锁、API 限流 |
| ORM | SQLAlchemy 2.0 | 成熟的 Python ORM |
| 数据迁移 | Alembic | 数据库版本管理 |
| 认证 | API Key + HMAC 签名 | 外部系统接入认证 |
| 管理后台 | Vue 3 + Element Plus | 管理端 Web 界面 |
| 容器化 | Docker + Docker Compose | 开发和部署 |

---

## 二、外部系统注册中心

这是积分服务和充值服务共享的基础模块，用来管理所有接入底座的外部业务系统。

### 2.1 数据模型

```
ExternalSystem (外部系统)
├── id              : bigint, PK
├── system_code     : varchar(64), UK      -- 系统编码，如 "mall", "community"
├── system_name     : varchar(128)          -- 系统名称，如 "电商系统"
├── description     : text                  -- 系统描述
├── app_key         : varchar(64), UK       -- API 接入凭证 (公钥)
├── app_secret      : varchar(128)          -- API 接入密钥 (私钥)
├── callback_url    : varchar(512)          -- 回调通知地址 (可选)
├── ip_whitelist    : text                  -- IP 白名单 (JSON 数组)
├── rate_limit      : int, default 1000     -- 每分钟请求限额
├── status          : smallint, default 1   -- 1:启用 0:禁用
├── created_at      : timestamp
└── updated_at      : timestamp
```

### 2.2 认证机制

外部系统调用 API 时需要在请求头中携带签名信息：

```
Header:
  X-App-Key: {app_key}
  X-Timestamp: {unix_timestamp}
  X-Nonce: {random_string}
  X-Signature: {HMAC-SHA256(app_secret, method + path + timestamp + nonce + body_md5)}
```

服务端验证流程：
1. 根据 `app_key` 查找系统，校验状态
2. 检查时间戳是否在 5 分钟窗口内（防重放）
3. 检查 nonce 是否已使用过（Redis 缓存，5 分钟过期）
4. 使用 `app_secret` 重新计算签名并比对
5. 检查来源 IP 是否在白名单内
6. 检查限流是否超额

---

## 三、积分系统（Points Service）

### 3.1 核心概念

| 概念 | 说明 |
|------|------|
| **积分动作** | 外部系统中会触发积分变动的业务行为，如"每日签到"、"下单购买"、"邀请好友" |
| **积分规则** | 每个动作对应的积分计算方式，如"签到+10分"、"购买金额×2倍积分" |
| **积分账户** | 每个用户在底座中拥有一个统一积分账户 |
| **积分流水** | 每一笔积分变动的详细记录 |

### 3.2 数据模型

```
PointAction (积分动作)
├── id              : bigint, PK
├── system_id       : bigint, FK -> ExternalSystem  -- 所属外部系统
├── action_code     : varchar(64)                    -- 动作编码，如 "daily_checkin"
├── action_name     : varchar(128)                   -- 动作名称，如 "每日签到"
├── description     : text                           -- 动作描述
├── direction       : smallint                       -- 1:增加 -1:扣减
├── status          : smallint, default 1            -- 1:启用 0:禁用
├── created_at      : timestamp
└── updated_at      : timestamp
  (UK: system_id + action_code)

PointRule (积分规则)
├── id              : bigint, PK
├── action_id       : bigint, FK -> PointAction      -- 所属动作
├── rule_name       : varchar(128)                   -- 规则名称
├── calc_type       : varchar(20)                    -- 计算类型: "fixed" / "rate" / "tiered" / "custom"
├── calc_value      : decimal(12,2)                  -- 固定值 或 倍率
├── calc_expression : text                           -- 高级计算表达式（JSON规则引擎）
├── min_points      : int, nullable                  -- 单次最小积分
├── max_points      : int, nullable                  -- 单次最大积分
├── daily_limit     : int, nullable                  -- 每日上限次数
├── monthly_limit   : int, nullable                  -- 每月上限次数
├── effective_from  : timestamp                      -- 生效时间
├── effective_to    : timestamp, nullable            -- 失效时间（null=永久）
├── priority        : int, default 0                 -- 优先级（多规则时生效）
├── status          : smallint, default 1
├── created_at      : timestamp
└── updated_at      : timestamp

PointAccount (积分账户)
├── id              : bigint, PK
├── user_id         : varchar(64), UK                -- 用户唯一标识
├── total_earned    : bigint, default 0              -- 累计获得积分
├── total_consumed  : bigint, default 0              -- 累计消耗积分
├── balance         : bigint, default 0              -- 当前可用余额
├── frozen          : bigint, default 0              -- 冻结积分
├── version         : bigint, default 0              -- 乐观锁版本号
├── created_at      : timestamp
└── updated_at      : timestamp

PointTransaction (积分流水)
├── id              : bigint, PK
├── transaction_no  : varchar(64), UK                -- 流水号（全局唯一）
├── account_id      : bigint, FK -> PointAccount     -- 积分账户
├── system_id       : bigint, FK -> ExternalSystem   -- 来源系统
├── action_id       : bigint, FK -> PointAction      -- 触发动作
├── rule_id         : bigint, FK -> PointRule         -- 命中规则
├── direction       : smallint                       -- 1:收入 -1:支出
├── points          : bigint                         -- 变动积分数
├── balance_before  : bigint                         -- 变动前余额
├── balance_after   : bigint                         -- 变动后余额
├── biz_order_no    : varchar(128), nullable         -- 外部业务单号
├── biz_amount      : decimal(12,2), nullable        -- 外部业务金额（用于按比例算积分）
├── remark          : varchar(512)                   -- 备注
├── idempotent_key  : varchar(128), UK               -- 幂等键 (system_id + biz_order_no + action_code)
├── created_at      : timestamp
└── updated_at      : timestamp
  (Index: account_id + created_at DESC)
```

### 3.3 积分规则计算类型

| calc_type | 说明 | 示例 |
|-----------|------|------|
| `fixed` | 固定值 | 签到 +10 积分，calc_value = 10 |
| `rate` | 按业务金额的倍率 | 消费1元=2积分，calc_value = 2，积分 = biz_amount × calc_value |
| `tiered` | 阶梯式 | 消费0-100元1倍，100-500元1.5倍，500+元2倍（通过calc_expression定义） |
| `custom` | 自定义表达式 | 通过 calc_expression JSON 定义复杂规则 |

**阶梯规则示例 (calc_expression JSON)：**

```json
{
  "tiers": [
    { "min": 0, "max": 100, "rate": 1.0 },
    { "min": 100, "max": 500, "rate": 1.5 },
    { "min": 500, "max": null, "rate": 2.0 }
  ]
}
```

### 3.4 API 设计

#### 3.4.1 管理端 API（后台管理用）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/systems` | 获取外部系统列表 |
| POST | `/admin/systems` | 注册外部系统 |
| PUT | `/admin/systems/{id}` | 更新外部系统 |
| DELETE | `/admin/systems/{id}` | 删除外部系统 |
| GET | `/admin/points/actions` | 获取积分动作列表 |
| POST | `/admin/points/actions` | 创建积分动作 |
| PUT | `/admin/points/actions/{id}` | 更新积分动作 |
| DELETE | `/admin/points/actions/{id}` | 删除积分动作 |
| GET | `/admin/points/rules` | 获取积分规则列表 |
| POST | `/admin/points/rules` | 创建积分规则 |
| PUT | `/admin/points/rules/{id}` | 更新积分规则 |
| DELETE | `/admin/points/rules/{id}` | 删除积分规则 |
| GET | `/admin/points/accounts` | 查看所有积分账户 |
| GET | `/admin/points/transactions` | 查看所有积分流水 |

#### 3.4.2 开放 API（外部系统调用）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/points/earn` | 增加积分 |
| POST | `/api/v1/points/deduct` | 扣减积分 |
| POST | `/api/v1/points/freeze` | 冻结积分 |
| POST | `/api/v1/points/unfreeze` | 解冻积分 |
| GET | `/api/v1/points/balance/{user_id}` | 查询积分余额 |
| GET | `/api/v1/points/transactions/{user_id}` | 查询积分流水明细 |
| POST | `/api/v1/points/check-rule` | 预校验规则（不实际变动，返回将产生的积分） |

**增加积分请求示例：**

```json
POST /api/v1/points/earn
{
  "user_id": "U10001",
  "action_code": "purchase",
  "biz_order_no": "ORD20260223001",
  "biz_amount": 299.00,
  "remark": "购买商品获得积分"
}
```

**响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "transaction_no": "PT20260223000001",
    "points": 598,
    "balance": 1598,
    "rule_name": "购物积分-双倍"
  }
}
```

**查询余额响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "user_id": "U10001",
    "balance": 1598,
    "frozen": 0,
    "total_earned": 2000,
    "total_consumed": 402
  }
}
```

**查询流水响应示例（分页）：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 128,
    "page": 1,
    "page_size": 20,
    "items": [
      {
        "transaction_no": "PT20260223000001",
        "direction": 1,
        "direction_text": "收入",
        "points": 598,
        "balance_after": 1598,
        "action_name": "购物积分",
        "system_name": "电商系统",
        "biz_order_no": "ORD20260223001",
        "remark": "购买商品获得积分",
        "created_at": "2026-02-23T10:30:00Z"
      }
    ]
  }
}
```

### 3.5 关键业务流程

#### 积分增加流程

```
外部系统 -> API网关(认证/签名校验/限流)
  -> 查找 action_code 对应的积分动作
  -> 幂等校验 (idempotent_key = system_id + biz_order_no + action_code)
  -> 匹配生效的积分规则 (按优先级，校验时间窗口)
  -> 校验每日/每月上限
  -> 计算积分 (根据 calc_type)
  -> 开启数据库事务:
      1. 更新积分账户余额 (乐观锁: WHERE version = ?)
      2. 插入积分流水记录
  -> 返回结果
```

#### 积分扣减流程

```
外部系统 -> API网关(认证)
  -> 幂等校验
  -> 查询账户可用余额
  -> 校验余额是否充足
  -> 开启事务:
      1. 扣减账户余额 (乐观锁 + 余额 >= 扣减值)
      2. 插入扣减流水
  -> 返回结果
```

---

## 四、充值/余额系统（Wallet Service）

### 4.1 核心概念

| 概念 | 说明 |
|------|------|
| **钱包账户** | 用户在底座系统中的统一余额账户（真实金额，单位：分） |
| **充值** | 用户通过支付渠道向钱包充值 |
| **消费动作** | 外部系统中会触发余额扣减的业务行为 |
| **消费规则** | 每个动作对应的扣费计算方式，如"固定费用"、"按量计费"、"阶梯定价" |
| **账单流水** | 每一笔充值和消费的详细记录 |

### 4.2 数据模型

```
WalletAccount (钱包账户)
├── id              : bigint, PK
├── user_id         : varchar(64), UK                -- 用户唯一标识
├── total_recharged : bigint, default 0              -- 累计充值（单位：分）
├── total_consumed  : bigint, default 0              -- 累计消费
├── balance         : bigint, default 0              -- 当前可用余额
├── frozen          : bigint, default 0              -- 冻结金额
├── version         : bigint, default 0              -- 乐观锁版本号
├── created_at      : timestamp
└── updated_at      : timestamp

ConsumeAction (消费动作)
├── id              : bigint, PK
├── system_id       : bigint, FK -> ExternalSystem   -- 所属外部系统
├── action_code     : varchar(64)                    -- 动作编码，如 "ai_call"
├── action_name     : varchar(128)                   -- 动作名称，如 "AI 调用"
├── description     : text                           -- 动作描述
├── status          : smallint, default 1
├── created_at      : timestamp
└── updated_at      : timestamp
  (UK: system_id + action_code)

ConsumeRule (消费规则)
├── id              : bigint, PK
├── action_id       : bigint, FK -> ConsumeAction
├── rule_name       : varchar(128)                   -- 规则名称
├── calc_type       : varchar(20)                    -- "fixed" / "unit_price" / "tiered" / "custom"
├── calc_value      : decimal(12,4)                  -- 单价或固定金额（单位：元）
├── calc_expression : text                           -- 高级规则表达式 (JSON)
├── unit_name       : varchar(32), nullable          -- 计量单位，如 "次"、"GB"、"Token"
├── min_charge      : int, nullable                  -- 最低收费（分）
├── max_charge      : int, nullable                  -- 最高收费（分）
├── free_quota      : int, default 0                 -- 免费额度
├── effective_from  : timestamp
├── effective_to    : timestamp, nullable
├── priority        : int, default 0
├── status          : smallint, default 1
├── created_at      : timestamp
└── updated_at      : timestamp

RechargeOrder (充值订单)
├── id              : bigint, PK
├── order_no        : varchar(64), UK                -- 充值订单号
├── account_id      : bigint, FK -> WalletAccount
├── user_id         : varchar(64)                    -- 冗余用户ID
├── amount          : bigint                         -- 充值金额（分）
├── actual_amount   : bigint                         -- 实际到账金额（含赠送）
├── gift_amount     : bigint, default 0              -- 赠送金额（分）
├── payment_method  : varchar(32)                    -- 支付方式：wechat/alipay/bank/manual
├── payment_no      : varchar(128), nullable         -- 第三方支付单号
├── status          : smallint                       -- 0:待支付 1:已支付 2:已取消 3:已退款
├── paid_at         : timestamp, nullable
├── remark          : varchar(512)
├── created_at      : timestamp
└── updated_at      : timestamp

WalletTransaction (钱包流水)
├── id              : bigint, PK
├── transaction_no  : varchar(64), UK                -- 流水号
├── account_id      : bigint, FK -> WalletAccount
├── type            : smallint                       -- 1:充值 2:消费 3:退款 4:冻结 5:解冻 6:后台调账
├── system_id       : bigint, nullable               -- 来源系统（消费时）
├── action_id       : bigint, nullable               -- 触发动作（消费时）
├── rule_id         : bigint, nullable               -- 命中规则（消费时）
├── amount          : bigint                         -- 变动金额（分）
├── balance_before  : bigint                         -- 变动前余额
├── balance_after   : bigint                         -- 变动后余额
├── biz_order_no    : varchar(128), nullable         -- 外部业务单号
├── biz_quantity    : decimal(12,4), nullable        -- 业务计量数（如调用次数、使用量）
├── remark          : varchar(512)
├── idempotent_key  : varchar(128), UK               -- 幂等键
├── created_at      : timestamp
└── updated_at      : timestamp
  (Index: account_id + created_at DESC)
```

### 4.3 消费规则计算类型

| calc_type | 说明 | 示例 |
|-----------|------|------|
| `fixed` | 每次固定金额 | 每次调用收费 0.5 元，calc_value = 0.50 |
| `unit_price` | 按量×单价 | 每 1000 Token 收费 0.02 元，calc_value = 0.02，unit_name = "千Token" |
| `tiered` | 阶梯定价 | 0-100次免费，100-1000次每次0.1元，1000+每次0.05元 |
| `custom` | 自定义表达式 | 通过 calc_expression 定义复杂规则 |

**充值赠送规则（可选扩展）：**

```
RechargePromotion (充值促销)
├── id              : bigint, PK
├── name            : varchar(128)                   -- 活动名称
├── min_amount      : bigint                         -- 最低充值金额（分）
├── gift_type       : varchar(20)                    -- "fixed" / "rate"
├── gift_value      : decimal(12,2)                  -- 赠送金额或比例
├── effective_from  : timestamp
├── effective_to    : timestamp, nullable
├── status          : smallint, default 1
├── created_at      : timestamp
└── updated_at      : timestamp
```

**赠送示例：** 充 100 送 10，充 500 送 80，充 1000 送 200。

### 4.4 API 设计

#### 4.4.1 管理端 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/wallet/actions` | 获取消费动作列表 |
| POST | `/admin/wallet/actions` | 创建消费动作 |
| PUT | `/admin/wallet/actions/{id}` | 更新消费动作 |
| DELETE | `/admin/wallet/actions/{id}` | 删除消费动作 |
| GET | `/admin/wallet/rules` | 获取消费规则列表 |
| POST | `/admin/wallet/rules` | 创建消费规则 |
| PUT | `/admin/wallet/rules/{id}` | 更新消费规则 |
| DELETE | `/admin/wallet/rules/{id}` | 删除消费规则 |
| GET | `/admin/wallet/accounts` | 查看所有钱包账户 |
| GET | `/admin/wallet/transactions` | 查看所有流水 |
| POST | `/admin/wallet/adjust` | 后台手动调账 |
| GET | `/admin/wallet/promotions` | 获取充值促销列表 |
| POST | `/admin/wallet/promotions` | 创建充值促销 |

#### 4.4.2 开放 API（外部系统调用）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/wallet/recharge` | 创建充值订单 |
| POST | `/api/v1/wallet/recharge/callback` | 充值支付回调 |
| GET | `/api/v1/wallet/recharge/{order_no}` | 查询充值订单状态 |
| POST | `/api/v1/wallet/consume` | 消费扣费 |
| POST | `/api/v1/wallet/freeze` | 冻结金额 |
| POST | `/api/v1/wallet/unfreeze` | 解冻金额 |
| POST | `/api/v1/wallet/refund` | 退款（返还余额） |
| GET | `/api/v1/wallet/balance/{user_id}` | 查询余额 |
| GET | `/api/v1/wallet/transactions/{user_id}` | 查询流水明细 |
| POST | `/api/v1/wallet/check-balance` | 预检查余额是否足够 |

**消费请求示例：**

```json
POST /api/v1/wallet/consume
{
  "user_id": "U10001",
  "action_code": "ai_call",
  "biz_order_no": "AI20260223001",
  "biz_quantity": 5.0,
  "remark": "AI 文档生成 5次调用"
}
```

**响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "transaction_no": "WT20260223000001",
    "amount": 250,
    "amount_display": "2.50元",
    "balance": 9750,
    "balance_display": "97.50元",
    "rule_name": "AI调用-按次计费"
  }
}
```

### 4.5 关键业务流程

#### 充值流程

```
用户发起充值 -> 创建充值订单 (status=0)
  -> 匹配充值促销规则，计算赠送金额
  -> 调用第三方支付
  -> 支付回调/主动查询:
      若支付成功:
        开启事务:
          1. 更新订单状态 (status=1)
          2. 增加钱包余额 (充值金额 + 赠送金额)
          3. 插入充值流水
      若支付失败:
        更新订单状态 (status=2)
```

#### 消费流程

```
外部系统 -> API网关(认证/签名/限流)
  -> 查找 action_code 对应的消费动作
  -> 幂等校验 (idempotent_key)
  -> 匹配生效的消费规则 (按优先级)
  -> 计算扣费金额 (根据 calc_type 和 biz_quantity)
  -> 校验免费额度
  -> 校验余额是否充足
  -> 开启事务:
      1. 扣减钱包余额 (乐观锁)
      2. 插入消费流水
  -> 返回结果
  -> (可选) 余额不足时通知用户充值
```

#### 冻结-消费-解冻流程（适用于预授权场景）

```
Step 1: 冻结 - 预留金额
  -> 从可用余额扣减到冻结金额
  -> 返回冻结单号

Step 2a: 确认消费 - 实际扣费
  -> 从冻结金额中扣减实际消费额
  -> 多余冻结金额释放回可用余额

Step 2b: 取消 - 释放冻结
  -> 全额释放冻结金额回可用余额
```

---

## 五、积分与余额的联动设计（可选扩展）

底座系统可支持积分和余额的联动场景：

### 5.1 积分抵扣

- 用户消费时可以使用积分抵扣部分金额
- 管理后台可设置积分兑换比例（如 100积分 = 1元）
- 管理后台可设置最大抵扣比例（如最多抵扣 50%）

### 5.2 积分兑换余额

- 用户可将积分兑换为钱包余额
- 按配置的兑换比例进行转换

### 5.3 消费赠送积分

- 用户通过余额消费时，自动赠送积分
- 通过积分规则中配置"充值消费"动作实现

---

## 六、系统安全与可靠性设计

### 6.1 幂等性

所有资金和积分变动接口均支持幂等：
- 由 `system_id + biz_order_no + action_code` 组成幂等键
- 数据库层面 UNIQUE 约束
- 重复请求直接返回之前的结果

### 6.2 并发安全

- 账户余额更新使用 **乐观锁**（version 字段）
- 关键操作使用 **Redis 分布式锁**（user_id 粒度）
- 数据库事务确保原子性

### 6.3 数据一致性

- 所有余额/积分变动在数据库事务中完成（账户更新 + 流水记录）
- 流水记录包含 `balance_before` 和 `balance_after`，支持对账
- 定时任务核对账户余额与流水汇总是否一致

### 6.4 审计追踪

- 所有管理端操作记录操作日志
- 流水记录不可修改、不可删除
- 关键操作支持回调通知外部系统

### 6.5 限流与防护

- API 网关层面：按 app_key 限流
- 业务层面：积分按日/月限额，消费按余额限制
- 签名防篡改 + 时间戳防重放

---

## 七、项目目录结构（建议）

```
CoBaseSys/
├── README.md
├── SYSTEM_DESIGN.md                    # 本设计文档
├── docker-compose.yml                  # 本地开发环境
├── backend/
│   ├── pyproject.toml                  # Python 项目配置
│   ├── alembic/                        # 数据库迁移
│   │   ├── alembic.ini
│   │   └── versions/
│   ├── app/
│   │   ├── __init__.py
│   │   ├── main.py                     # FastAPI 应用入口
│   │   ├── config.py                   # 配置管理
│   │   ├── database.py                 # 数据库连接
│   │   ├── models/                     # SQLAlchemy 模型
│   │   │   ├── __init__.py
│   │   │   ├── system.py              # ExternalSystem
│   │   │   ├── points.py             # 积分相关模型
│   │   │   └── wallet.py             # 钱包相关模型
│   │   ├── schemas/                    # Pydantic 请求/响应模型
│   │   │   ├── __init__.py
│   │   │   ├── system.py
│   │   │   ├── points.py
│   │   │   └── wallet.py
│   │   ├── api/                        # API 路由
│   │   │   ├── __init__.py
│   │   │   ├── deps.py                # 依赖注入（认证等）
│   │   │   ├── admin/                 # 管理端路由
│   │   │   │   ├── systems.py
│   │   │   │   ├── points.py
│   │   │   │   └── wallet.py
│   │   │   └── v1/                    # 开放API v1
│   │   │       ├── points.py
│   │   │       └── wallet.py
│   │   ├── services/                   # 业务逻辑层
│   │   │   ├── __init__.py
│   │   │   ├── auth.py                # 签名认证
│   │   │   ├── points.py             # 积分服务
│   │   │   ├── wallet.py             # 钱包服务
│   │   │   └── rule_engine.py         # 规则计算引擎
│   │   └── utils/                      # 工具类
│   │       ├── __init__.py
│   │       ├── id_generator.py        # 流水号生成
│   │       └── exceptions.py          # 自定义异常
│   └── tests/                          # 测试
│       ├── test_points.py
│       └── test_wallet.py
└── frontend/                           # 管理后台前端
    ├── package.json
    ├── src/
    │   ├── views/
    │   │   ├── systems/               # 外部系统管理
    │   │   ├── points/                # 积分管理
    │   │   └── wallet/                # 钱包管理
    │   └── ...
    └── ...
```

---

## 八、管理后台功能规划

### 8.1 外部系统管理

- 系统列表（编码、名称、状态、创建时间）
- 注册新系统（自动生成 app_key 和 app_secret）
- 编辑系统信息（名称、回调地址、IP白名单、限流配额）
- 启用/禁用系统
- 重置 app_secret
- 查看系统调用统计

### 8.2 积分管理

- **动作管理：** 按系统查看/创建/编辑积分动作
- **规则管理：** 为每个动作配置积分规则（支持多规则、优先级、时间窗口）
- **账户查询：** 搜索用户积分账户，查看余额
- **流水查询：** 按用户/系统/时间范围查询积分明细
- **数据统计：** 积分发放趋势、消耗趋势、各系统占比

### 8.3 钱包管理

- **动作管理：** 按系统查看/创建/编辑消费动作
- **规则管理：** 配置消费计费规则
- **充值管理：** 查看充值订单列表、手动充值（后台调账）
- **促销管理：** 配置充值赠送活动
- **账户查询：** 搜索用户钱包账户
- **流水查询：** 充值/消费明细
- **数据统计：** 充值趋势、消费趋势、各系统消费占比

---

## 九、后续扩展方向

| 方向 | 说明 |
|------|------|
| 积分商城 | 积分可兑换商品/优惠券 |
| 会员等级 | 根据积分/消费金额自动升级会员等级，不同等级享受不同权益 |
| 优惠券系统 | 统一优惠券发放和核销 |
| 通知服务 | 余额不足提醒、积分到期提醒 |
| 数据报表 | 更丰富的运营数据分析 |
| 多租户 | 支持多公司/品牌独立运营 |
| Webhook | 关键事件实时通知外部系统 |

---

## 十、确认清单

请您确认以下设计要点，确认后即可开始开发：

- [ ] **技术选型**：Python + FastAPI + PostgreSQL + Redis 是否可接受？或有其他偏好？
- [ ] **数据模型**：积分和钱包的表结构设计是否满足需求？是否需要增减字段？
- [ ] **积分规则类型**：fixed / rate / tiered / custom 四种是否够用？
- [ ] **消费规则类型**：fixed / unit_price / tiered / custom 四种是否够用？
- [ ] **API 设计**：开放 API 和管理 API 的接口是否覆盖了所需场景？
- [ ] **认证方式**：HMAC 签名认证是否合适？还是更倾向 OAuth2 / JWT？
- [ ] **充值支付**：是否需要对接具体的支付渠道（微信/支付宝）？还是先只支持后台手动充值？
- [ ] **积分余额联动**：积分抵扣、积分兑换余额等功能是否需要在一期实现？
- [ ] **管理后台**：是否需要在一期同步开发前端管理界面？还是先只做后端 API？
- [ ] **金额单位**：余额系统中金额存储使用"分"（整数），展示时转换为"元"，是否可接受？
- [ ] **项目结构**：目录结构是否需要调整？
- [ ] **其他需求**：是否有其他未覆盖的业务场景或特殊要求？
