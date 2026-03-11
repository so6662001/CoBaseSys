# CoBaseSys 计费模块 — 系统设计方案

## 一、模块定位与全局架构

### 1.1 模块定位

计费模块是 CoBaseSys 底座系统的核心商业化组件，为公司提供**在线产品销售、套餐管理、订单支付、订阅管理、续费提醒、赠送规则、折扣引擎、积分抵扣**等完整的商业化闭环能力。与现有的积分系统、充值/余额系统、通知服务、Webhook 深度集成。

### 1.2 总体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        计费模块 (Billing Module)                      │
│                                                                     │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌──────────┐ ┌────────┐│
│  │ 产品与套餐  │ │ 定价与折扣  │ │ 订单与支付  │ │ 订阅管理  │ │ 试用管理 ││
│  │ Product   │ │ Pricing   │ │  Order    │ │Subscript │ │ Trial  ││
│  └─────┬─────┘ └─────┬─────┘ └─────┬─────┘ └────┬─────┘ └───┬────┘│
│        └──────────────┴─────────────┴────────────┴───────────┘     │
│                              │                                     │
│  ┌───────────────────────────┴───────────────────────────────────┐ │
│  │ 赠送引擎 │ 折扣引擎 │ 积分抵扣 │ 到期提醒调度 │ 外部查询API   │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                              │                                     │
│  ┌───────────────────────────┴───────────────────────────────────┐ │
│  │          现有底座：积分系统 │ 余额系统 │ 通知服务 │ Webhook      │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                              │                                     │
│  ┌───────────────────────────┴───────────────────────────────────┐ │
│  │           MySQL (InnoDB)   │   Redis (缓存/调度锁)             │ │
│  └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
       │               │               │               │
  ┌────┴─────┐  ┌──────┴────┐   ┌──────┴────┐   ┌──────┴────┐
  │ 客户前台  │  │ 钢贸宝系统 │   │ 其他业务系统│   │ 支付网关   │
  │PC+H5购买 │  │ 查询到期   │   │ 查询授权   │   │微信/支付宝 │
  └──────────┘  └──────────┘   └──────────┘   └──────────┘
```

### 1.3 技术选型

| 组件 | 技术 | 说明 |
|------|------|------|
| 语言 | Java 21 | 虚拟线程支持 10 万+并发 |
| 框架 | Spring Boot 3.2 | 与现有底座统一 |
| 数据库 | MySQL 8.0+ (InnoDB) | 按需求使用 MySQL |
| 缓存 | Redis 7 | 订阅状态缓存、分布式调度锁 |
| 定时任务 | Spring Scheduling + Redis 分布式锁 | 到期提醒调度 |
| 短信 | 腾讯云短信 (Tencent Cloud SMS) | 到期/续费/试用提醒 |
| 管理后台前端 | Vue 3 + Element Plus | PC 管理端 |
| 客户购买前台 | Vue 3 + Vant 4 | H5 移动端自适应 |

### 1.4 终端适配策略

所有面向客户的页面和管理后台均需支持 H5 移动端操作：

```
┌──────────────────────────────────────────────┐
│                终端适配方案                     │
│                                              │
│  管理后台 (Admin Portal)                       │
│  ├─ PC端: Vue 3 + Element Plus               │
│  └─ 移动端: 响应式布局 (Element Plus 内置适配)   │
│                                              │
│  客户购买前台 (Customer Portal)                 │
│  ├─ H5移动端: Vue 3 + Vant 4 (移动优先)       │
│  ├─ PC端: 同一套代码自适应                      │
│  └─ 微信内打开: 支持微信JSAPI支付              │
│                                              │
│  API层: 同一套后端API，前端自行适配             │
└──────────────────────────────────────────────┘
```

---

## 二、核心概念与名词定义

| 概念 | 说明 |
|------|------|
| **产品 (Product)** | 最小可售单元，如"钢贸宝企业版用户"、"100GB 云存储空间" |
| **套餐 (Package)** | 多个产品的组合销售单元，如"企业版套餐 = 10用户 + 100GB空间 + 5000次API调用" |
| **定价方案 (PricingPlan)** | 某个产品或套餐的具体定价配置，支持 5 种收费模式 |
| **订单 (Order)** | 客户购买/续费/升级的交易凭证 |
| **订阅 (Subscription)** | 客户实际拥有的产品/套餐权益，有明确的起止日期和使用量 |
| **赠送规则 (GiftRule)** | 满足条件时自动赠送的产品/套餐/积分规则 |
| **折扣规则 (DiscountRule)** | 产品/套餐维度或订单金额维度的优惠规则 |
| **试用 (Trial)** | 客户在正式购买前免费体验产品/套餐的机制 |

---

## 三、收费模式详解

系统支持以下 5 种收费模式，每种模式的数据结构和计费逻辑各不相同：

### 3.1 一次性软件款 + 年服务费 (`ONE_TIME_ANNUAL`)

```
┌─────────────────┐
│  首次付款         │
│  软件款 (一次性)    │──→ 永久使用权
│  + 年服务费       │──→ 按年续费
└─────────────────┘
```

- **软件款**：首次购买时一次性支付，买断使用权
- **年服务费**：每年到期续费，保障技术支持与升级
- 续费时仅需支付年服务费
- 定价参数：`software_fee`(软件款), `annual_service_fee`(年服务费)

### 3.2 年/季/月订阅费 + 增量付费 (`SUBSCRIPTION`)

```
┌─────────────────┐
│  订阅周期费        │──→ 年/季/月自动续费
│  包含基础用量       │
│  超出部分按量付费   │──→ 增量价格
└─────────────────┘
```

- 按周期（年/季/月）支付订阅费
- 包含一定的基础用量（如 1000 次 API 调用）
- 超出基础用量的部分按增量单价计费
- 定价参数：`period_type`, `period_price`, `included_quantity`, `overage_unit_name`, `overage_unit_price`

### 3.3 按量计费 (`USAGE_BASED`)

```
┌─────────────────┐
│  使用多少付多少    │──→ 按实际用量计费
│  无固定费用        │
└─────────────────┘
```

- 无固定订阅费，完全按实际使用量计费
- 可设置阶梯价（用得越多越便宜）
- 定价参数：`unit_name`, `unit_price`, `tiered_pricing`(可选阶梯)

### 3.4 云端租用费 (`CLOUD_RENTAL`)

```
┌─────────────────┐
│  云端资源租用      │──→ 按周期付费
│  到期停用          │
└─────────────────┘
```

- 云端服务/资源的周期性租用
- 到期不续费则停用
- 定价参数：`period_type`, `rental_price`

### 3.5 空间租用费 (`SPACE_RENTAL`)

```
┌─────────────────┐
│  存储空间租用      │──→ 按容量 × 周期付费
│  可扩容/缩容       │
└─────────────────┘
```

- 按存储空间大小和周期计费
- 支持扩容/缩容
- 定价参数：`space_unit`(GB/TB), `unit_price_per_period`, `period_type`

### 3.6 阶梯累进计费 (`TIERED_PROGRESSIVE`)

```
┌─────────────────┐
│  用量越大越便宜    │──→ 分段累进计算
│  鼓励大客户        │
└─────────────────┘
```

- 与按量计费类似，但按不同用量区间采用不同单价
- 区间累进：前 100 次 1元/次，100-500 次 0.8元/次，500+ 次 0.5元/次
- 适合 API 调用、消息发送等场景
- 定价参数：`tiered_pricing` JSON 配置

```json
{
  "tiers": [
    {"min": 0, "max": 100, "unit_price": 100},
    {"min": 100, "max": 500, "unit_price": 80},
    {"min": 500, "max": null, "unit_price": 50}
  ]
}
```

### 3.7 一次性买断 (`ONE_TIME`)

```
┌─────────────────┐
│  一次付清         │──→ 永久使用权
│  无后续费用        │──→ 无到期概念
└─────────────────┘
```

- 一次性支付即获得永久使用权
- 没有续费和到期的概念
- 适合数据报告、培训课程、模板文件等数字商品
- 定价参数：`one_time_price`

---

## 四、数据模型设计

### 4.1 产品表 (`t_billing_product`)

```
t_billing_product (产品)
├── id                  : bigint, PK
├── tenant_id           : bigint, NOT NULL
├── product_code        : varchar(64), UK(tenant_id, product_code)   -- 产品编码
├── product_name        : varchar(200), NOT NULL                     -- 产品名称
├── category            : varchar(64)                                -- 分类：software/service/resource/storage
├── description         : text                                       -- 产品描述
├── icon_url            : varchar(512)                               -- 产品图标
├── pricing_model       : varchar(30), NOT NULL                      -- ONE_TIME_ANNUAL / SUBSCRIPTION / USAGE_BASED / CLOUD_RENTAL / SPACE_RENTAL / TIERED_PROGRESSIVE / ONE_TIME
├── trial_enabled       : tinyint, default 0                         -- 是否支持试用
├── trial_days          : int, default 0                             -- 试用天数
├── trial_extend_enabled: tinyint, default 0                         -- 是否允许延长试用
├── trial_max_extend_days: int, default 0                            -- 最大可延长天数
├── points_payable      : tinyint, default 0                         -- 是否支持积分支付
├── max_points_ratio    : decimal(5,2), default 0                    -- 最大积分抵扣比例（0.50 = 50%）
├── points_exchange_rate: decimal(12,4), default 0                   -- 积分换算系数（多少积分 = 1元）
├── sort_order          : int, default 0                             -- 排序
├── status              : tinyint, default 1                         -- 1:上架 0:下架
├── created_at          : datetime
└── updated_at          : datetime
```

### 4.2 套餐表 (`t_billing_package`)

```
t_billing_package (套餐)
├── id                  : bigint, PK
├── tenant_id           : bigint, NOT NULL
├── package_code        : varchar(64), UK(tenant_id, package_code)
├── package_name        : varchar(200), NOT NULL
├── description         : text
├── icon_url            : varchar(512)
├── pricing_model       : varchar(30), NOT NULL
├── trial_enabled       : tinyint, default 0
├── trial_days          : int, default 0
├── trial_extend_enabled: tinyint, default 0
├── trial_max_extend_days: int, default 0
├── points_payable      : tinyint, default 0
├── max_points_ratio    : decimal(5,2), default 0
├── points_exchange_rate: decimal(12,4), default 0
├── sort_order          : int, default 0
├── status              : tinyint, default 1
├── created_at          : datetime
└── updated_at          : datetime

t_billing_package_item (套餐明细)
├── id                  : bigint, PK
├── package_id          : bigint, FK -> t_billing_package
├── product_id          : bigint, FK -> t_billing_product
├── quantity            : int, NOT NULL                               -- 包含数量
├── sort_order          : int, default 0
└── created_at          : datetime
```

### 4.3 定价方案表 (`t_billing_pricing_plan`)

一个产品或套餐可以有多个定价方案（不同时期价格不同、新客/老客价格不同等），系统按优先级和生效时间匹配。

```
t_billing_pricing_plan (定价方案)
├── id                  : bigint, PK
├── tenant_id           : bigint, NOT NULL
├── target_type         : varchar(10), NOT NULL                      -- PRODUCT / PACKAGE
├── target_id           : bigint, NOT NULL                           -- 产品ID 或 套餐ID
├── plan_name           : varchar(128), NOT NULL                     -- 方案名称
├── pricing_model       : varchar(30), NOT NULL                      -- 与产品/套餐一致
│
│   ── 一次性+年服务费模式 ──
├── software_fee        : bigint, default 0                          -- 软件款（分）
├── annual_service_fee  : bigint, default 0                          -- 年服务费（分）
│
│   ── 订阅模式 ──
├── period_type         : varchar(10)                                -- YEAR / QUARTER / MONTH
├── period_price        : bigint, default 0                          -- 周期订阅价（分）
├── included_quantity   : int, default 0                             -- 包含的基础用量
├── overage_unit_name   : varchar(32)                                -- 增量计量单位
├── overage_unit_price  : bigint, default 0                          -- 增量单价（分）
│
│   ── 按量计费模式 ──
├── unit_name           : varchar(32)                                -- 计量单位
├── unit_price          : bigint, default 0                          -- 单价（分）
├── tiered_pricing      : json                                       -- 阶梯定价配置
│
│   ── 云端租用 / 空间租用 ──
├── rental_period_type  : varchar(10)                                -- YEAR / QUARTER / MONTH
├── rental_price        : bigint, default 0                          -- 租用价格（分）
├── space_unit          : varchar(10)                                -- GB / TB
├── space_unit_price    : bigint, default 0                          -- 每单位空间每周期价格（分）
│
│   ── 一次性买断 ──
├── one_time_price      : bigint, default 0                          -- 一次性买断价格（分）
│
├── validity_days       : int                                        -- 购买后有效天数（如365天，一次性买断可为null=永久）
├── priority            : int, default 0                             -- 优先级（匹配时用）
├── effective_from      : datetime, NOT NULL                         -- 生效开始
├── effective_to        : datetime                                   -- 生效结束（null=永久）
├── status              : tinyint, default 1
├── created_at          : datetime
└── updated_at          : datetime
  (Index: target_type + target_id + status + effective_from)
```

### 4.4 折扣规则表 (`t_billing_discount_rule`)

```
t_billing_discount_rule (折扣规则)
├── id                  : bigint, PK
├── tenant_id           : bigint, NOT NULL
├── rule_name           : varchar(128), NOT NULL
├── discount_type       : varchar(20), NOT NULL
│   -- PRODUCT_DISCOUNT    : 单一产品折扣
│   -- PACKAGE_DISCOUNT    : 单一套餐折扣
│   -- AMOUNT_DISCOUNT     : 订单满额折扣
│   -- AMOUNT_POINTS_BONUS : 订单满额送积分
│
├── target_type         : varchar(10)                                -- PRODUCT / PACKAGE（当 PRODUCT_DISCOUNT/PACKAGE_DISCOUNT 时）
├── target_id           : bigint                                     -- 产品ID / 套餐ID
├── min_quantity        : int, default 1                             -- 最低购买数量门槛
├── discount_rate       : decimal(5,2)                               -- 折扣率（0.90 = 9折）
├── threshold_amount    : bigint, default 0                          -- 满额门槛（分）
├── bonus_points        : bigint, default 0                          -- 赠送积分数量
├── effective_from      : datetime
├── effective_to        : datetime
├── priority            : int, default 0
├── status              : tinyint, default 1
├── created_at          : datetime
└── updated_at          : datetime
```

### 4.5 赠送规则表 (`t_billing_gift_rule`)

```
t_billing_gift_rule (赠送规则)
├── id                  : bigint, PK
├── tenant_id           : bigint, NOT NULL
├── rule_name           : varchar(128), NOT NULL                     -- 如 "买10个企业版送1个"
│
│   ── 触发条件 ──
├── condition_type      : varchar(20), NOT NULL
│   -- BUY_PRODUCT         : 购买指定产品满N个
│   -- BUY_PACKAGE         : 购买指定套餐满N个
│   -- SPEND_AMOUNT        : 订单金额满N元
│
├── condition_target_type: varchar(10)                               -- PRODUCT / PACKAGE
├── condition_target_id : bigint                                     -- 条件产品/套餐ID
├── condition_quantity  : int, default 1                             -- 条件数量/金额
│
│   ── 赠送内容 ──
├── gift_type           : varchar(10), NOT NULL                      -- PRODUCT / PACKAGE / POINTS
├── gift_target_id      : bigint                                     -- 赠送的产品/套餐ID（POINTS 时为空）
├── gift_quantity       : int, default 1                             -- 赠送数量
├── gift_points         : bigint, default 0                          -- 赠送积分数（gift_type=POINTS 时）
├── gift_validity_days  : int                                        -- 赠品有效天数
│
├── effective_from      : datetime
├── effective_to        : datetime
├── status              : tinyint, default 1
├── created_at          : datetime
└── updated_at          : datetime
```

### 4.6 订单表 (`t_billing_order` / `t_billing_order_item`)

```
t_billing_order (订单主表)
├── id                  : bigint, PK
├── tenant_id           : bigint, NOT NULL
├── order_no            : varchar(64), UK                            -- 订单号
├── customer_id         : varchar(64), NOT NULL                      -- 客户ID
├── customer_name       : varchar(128)                               -- 客户名称
├── order_type          : varchar(20), NOT NULL                      -- NEW_PURCHASE / RENEWAL / UPGRADE / TRIAL
├── order_source        : varchar(20), NOT NULL, default 'CUSTOMER'  -- CUSTOMER:客户自助 / ADMIN:后台代客下单
├── operator_id         : varchar(64)                                -- 操作人ID（后台代客下单时记录操作人）
├── operator_name       : varchar(128)                               -- 操作人姓名
│
│   ── 金额信息 ──
├── total_amount        : bigint, NOT NULL                           -- 商品总金额（分）
├── discount_amount     : bigint, default 0                          -- 折扣减免金额
├── gift_amount         : bigint, default 0                          -- 赠品价值金额
├── points_deduct_amount: bigint, default 0                         -- 积分抵扣金额（分）
├── points_used         : bigint, default 0                          -- 使用的积分数量
├── actual_amount       : bigint, NOT NULL                           -- 实付金额 = 总金额 - 折扣 - 积分抵扣
│
│   ── 支付信息 ──
├── payment_status      : tinyint, NOT NULL                          -- 0:待支付 1:已支付 2:已取消 3:已退款
├── payment_method      : varchar(32)                                -- wechat / alipay / bank / balance / offline
├── payment_no          : varchar(128)                               -- 第三方支付单号
├── paid_at             : datetime
│
├── remark              : varchar(512)
├── status              : tinyint, NOT NULL                          -- 0:待确认 1:已生效 2:已取消 3:已退款
├── created_at          : datetime
└── updated_at          : datetime
  (Index: tenant_id + customer_id + created_at DESC)
  (Index: tenant_id + payment_status)

t_billing_order_item (订单明细)
├── id                  : bigint, PK
├── order_id            : bigint, FK -> t_billing_order
├── item_type           : varchar(10), NOT NULL                      -- PRODUCT / PACKAGE / GIFT
├── item_id             : bigint, NOT NULL                           -- 产品ID 或 套餐ID
├── item_name           : varchar(200)                               -- 冗余名称
├── pricing_plan_id     : bigint                                     -- 关联定价方案ID
├── pricing_model       : varchar(30)                                -- 收费模式
├── quantity            : int, NOT NULL                              -- 购买数量
├── unit_price          : bigint                                     -- 单价（分）
├── original_amount     : bigint                                     -- 原价金额（分）
├── discount_amount     : bigint, default 0                          -- 折扣金额
├── actual_amount       : bigint                                     -- 实际金额
├── period_type         : varchar(10)                                -- 周期类型
├── period_count        : int, default 1                             -- 购买周期数
├── start_date          : date                                       -- 服务开始日期
├── end_date            : date                                       -- 服务结束日期
├── is_gift             : tinyint, default 0                         -- 是否为赠品
├── gift_rule_id        : bigint                                     -- 关联赠送规则ID
├── created_at          : datetime
└── updated_at          : datetime
```

### 4.7 客户订阅表 (`t_billing_subscription`)

这是核心业务表，记录客户当前拥有的每一项产品/套餐权益。

```
t_billing_subscription (客户订阅/已购)
├── id                  : bigint, PK
├── tenant_id           : bigint, NOT NULL
├── customer_id         : varchar(64), NOT NULL
├── subscription_no     : varchar(64), UK                            -- 订阅编号
│
│   ── 关联信息 ──
├── source_type         : varchar(10), NOT NULL                      -- PRODUCT / PACKAGE
├── source_id           : bigint, NOT NULL                           -- 产品ID / 套餐ID
├── source_name         : varchar(200)                               -- 冗余名称
├── order_id            : bigint                                     -- 关联订单ID
├── order_item_id       : bigint                                     -- 关联订单明细ID
├── pricing_model       : varchar(30)                                -- 收费模式
├── pricing_plan_id     : bigint                                     -- 当前定价方案ID
│
│   ── 数量与用量 ──
├── quantity            : int, default 1                             -- 购买数量（如10个用户）
├── usage_quota         : bigint, default 0                          -- 用量配额（按量计费/订阅增量用）
├── usage_used          : bigint, default 0                          -- 已用量
├── usage_unit          : varchar(32)                                -- 用量单位：次/条/GB/TB 等
│
│   ── 空间计量（SPACE_RENTAL 模式）──
├── space_total         : bigint, default 0                          -- 购买总空间（字节）
├── space_used          : bigint, default 0                          -- 已用空间（字节）
│
│   ── 时间计量（冗余便于报表）──
├── total_days          : int, default 0                             -- 购买总天数
├── days_used           : int, default 0                             -- 已使用天数（定时任务每日更新）
│
│   ── 时间与状态 ──
├── status              : varchar(20), NOT NULL
│   -- TRIAL       : 试用中
│   -- ACTIVE      : 正常使用
│   -- EXPIRING    : 即将到期（15天内）
│   -- EXPIRED     : 已过期
│   -- SUSPENDED   : 已暂停
│
├── is_trial            : tinyint, default 0                         -- 是否试用
├── start_date          : date, NOT NULL                             -- 开始日期
├── end_date            : date, NOT NULL                             -- 到期日期
├── auto_renew          : tinyint, default 0                         -- 是否自动续费
│
│   ── 续费定价 ──
├── original_unit_price : bigint                                     -- 购买时的单价（用于判断续费价格）
├── renewal_price       : bigint                                     -- 续费价格（分）
├── price_locked_until  : date                                       -- 价格锁定截止日期
│
├── version             : bigint, default 0                          -- 乐观锁
├── created_at          : datetime
└── updated_at          : datetime
  (Index: tenant_id + customer_id + status)
  (Index: tenant_id + end_date + status)                             -- 到期查询索引
  (Index: source_type + source_id + customer_id)                     -- 外部API查询索引
```

### 4.8 续费提醒记录表 (`t_billing_renewal_reminder`)

```
t_billing_renewal_reminder (续费提醒记录)
├── id                  : bigint, PK
├── tenant_id           : bigint, NOT NULL
├── subscription_id     : bigint, FK -> t_billing_subscription
├── customer_id         : varchar(64), NOT NULL
├── reminder_type       : varchar(20), NOT NULL                      -- PRE_EXPIRY / POST_EXPIRY / TRIAL_EXPIRY / LOGIN
├── channel             : varchar(20), NOT NULL                      -- SMS / EMAIL / IN_APP
├── content             : text                                       -- 提醒内容
├── recipient           : varchar(128)                               -- 接收人手机号/邮箱
├── status              : tinyint                                    -- 0:待发送 1:已发送 2:失败
├── sent_at             : datetime
├── created_at          : datetime
└── updated_at          : datetime
  (Index: subscription_id + reminder_type + DATE(created_at))        -- 防重复发送
```

### 4.9 试用记录表 (`t_billing_trial`)

```
t_billing_trial (试用记录)
├── id                  : bigint, PK
├── tenant_id           : bigint, NOT NULL
├── customer_id         : varchar(64), NOT NULL
├── source_type         : varchar(10), NOT NULL                      -- PRODUCT / PACKAGE
├── source_id           : bigint, NOT NULL
├── source_name         : varchar(200)
├── subscription_id     : bigint                                     -- 关联的订阅ID
├── trial_days          : int, NOT NULL
├── start_date          : date, NOT NULL
├── end_date            : date, NOT NULL
├── status              : varchar(20), NOT NULL                      -- ACTIVE / EXPIRED / CONVERTED
├── converted_order_id  : bigint                                     -- 转正订单ID
├── contact_info        : varchar(512)                               -- 联系方式（到期提醒时附上）
├── extend_count        : int, default 0                             -- 已延长次数
├── total_extend_days   : int, default 0                             -- 累计延长天数
├── created_at          : datetime
└── updated_at          : datetime
  (UK: tenant_id + customer_id + source_type + source_id)            -- 同一客户同一产品只能有一个试用
```

### 4.10 用量流水表 (`t_billing_usage_ledger`)

按量计费、订阅增量、空间租用等模式下，记录每一笔用量变动的明细账。

```
t_billing_usage_ledger (用量流水)
├── id                  : bigint, PK
├── tenant_id           : bigint, NOT NULL
├── subscription_id     : bigint, FK -> t_billing_subscription
├── customer_id         : varchar(64), NOT NULL
│
├── action              : varchar(20), NOT NULL                      -- CONSUME:消耗 / RECHARGE:充值(买量) / ADJUST:调整 / RECLAIM:回收
├── quantity            : bigint, NOT NULL                           -- 本次变动量（正数=增加，负数=消耗）
├── unit                : varchar(32), NOT NULL                      -- 单位：次/条/GB/MB 等
├── balance_before      : bigint, NOT NULL                           -- 变动前剩余量
├── balance_after       : bigint, NOT NULL                           -- 变动后剩余量
│
├── unit_price          : bigint, default 0                          -- 本次单价（分）
├── amount              : bigint, default 0                          -- 本次金额（分）
│
├── biz_system          : varchar(64)                                -- 来源系统编码
├── biz_order_no        : varchar(128)                               -- 外部业务单号
├── biz_description     : varchar(512)                               -- 使用说明/描述
├── idempotent_key      : varchar(128), UK                           -- 幂等键
│
├── created_at          : datetime
└── updated_at          : datetime
  (Index: subscription_id + created_at DESC)
  (Index: tenant_id + customer_id + created_at DESC)
```

### 4.11 试用延长审批表 (`t_billing_trial_extend_approval`)

试用延长需要走审批流程：销售员提交 → 主管审批。

```
t_billing_trial_extend_approval (试用延长审批)
├── id                  : bigint, PK
├── tenant_id           : bigint, NOT NULL
├── trial_id            : bigint, FK -> t_billing_trial
├── subscription_id     : bigint, FK -> t_billing_subscription
├── customer_id         : varchar(64), NOT NULL
├── customer_name       : varchar(128)
├── source_name         : varchar(200)                               -- 产品/套餐名称
│
│   ── 申请信息 ──
├── extend_days         : int, NOT NULL                              -- 申请延长天数
├── apply_reason        : varchar(512)                               -- 申请原因
├── applicant_id        : varchar(64), NOT NULL                      -- 申请人ID（销售员）
├── applicant_name      : varchar(128)                               -- 申请人姓名
├── apply_time          : datetime, NOT NULL                         -- 申请时间
│
│   ── 审批信息 ──
├── status              : varchar(20), NOT NULL                      -- PENDING:待审批 / APPROVED:已通过 / REJECTED:已驳回
├── approver_id         : varchar(64)                                -- 审批人ID（主管）
├── approver_name       : varchar(128)                               -- 审批人姓名
├── approve_time        : datetime                                   -- 审批时间
├── approve_remark      : varchar(512)                               -- 审批意见
│
├── created_at          : datetime
└── updated_at          : datetime
  (Index: tenant_id + status)
  (Index: tenant_id + applicant_id + status)
  (Index: tenant_id + approver_id + status)
```

---

## 五、核心业务流程

### 5.1 在线购买流程（客户自助 + 后台代客下单）

系统支持两种下单方式，共用同一套价格计算和订单处理引擎：

```
┌──────────────────────────────────────────────────────┐
│  方式A: 客户自助下单 (PC/H5)                           │
│  客户浏览商品 → 加入购物车 → 结算支付                    │
│  order_source = CUSTOMER                             │
├──────────────────────────────────────────────────────┤
│  方式B: 后台代客下单 (管理后台)                         │
│  管理员选择客户 → 选择产品/套餐 → 确认价格 → 代客下单   │
│  order_source = ADMIN, operator_id/name 记录操作人    │
│  支持后台直接确认付款 (线下收款场景)                     │
└──────────────────────────────────────────────────────┘

共用流程:
  │
  ├→ 选择产品/套餐 + 数量 + 周期
  │
  ├→ 系统计算价格:
  │   ├→ 1. 查询定价方案 (按优先级+生效时间匹配)
  │   ├→ 2. 应用折扣规则 (产品折扣/套餐折扣/满额折扣)
  │   ├→ 3. 匹配赠送规则 (买N送M)
  │   ├→ 4. 计算积分抵扣 (校验 max_points_ratio)
  │   └→ 5. 计算实付金额
  │
  ├→ 创建订单 (status=待支付)
  │
  ├→ 支付:
  │   ├→ [客户自助] 余额支付 / 微信支付 / 支付宝
  │   ├→ [客户自助] 线下转账后由后台确认
  │   └→ [代客下单] 后台直接确认收款
  │
  ├→ 支付成功回调:
  │   ├→ 1. 更新订单状态 (payment_status=已支付)
  │   ├→ 2. 创建客户订阅 (status=ACTIVE)
  │   ├→ 3. 处理赠送 (创建赠品订阅/发放积分)
  │   ├→ 4. 扣减积分 (如使用了积分)
  │   ├→ 5. 发放满额赠送积分
  │   ├→ 6. 发送 Webhook 通知相关业务系统
  │   └→ 7. 发送购买成功短信/通知
  │
  └→ 完成
```

### 5.2 续费流程

```
到期提醒调度 (每天14:00)
  │
  ├→ 查询 15天内到期的订阅 (status=ACTIVE 且 end_date <= today+15)
  │
  ├→ 对每条即将到期的订阅:
  │   ├→ 检查今日是否已发过提醒 (防重复)
  │   ├→ 确定续费价格:
  │   │   ├→ 若 price_locked_until >= today → 使用 renewal_price (原价)
  │   │   └→ 否则 → 查询当前最新定价方案的价格
  │   ├→ 发送短信提醒
  │   ├→ 发送应用内提醒
  │   └→ 记录提醒记录 (t_billing_renewal_reminder)
  │
  ├→ 客户登录时:
  │   ├→ 查询该客户即将到期/已到期的订阅
  │   └→ 弹窗/消息中心提醒
  │
  ├→ 客户发起续费:
  │   ├→ 系统判断续费价格 (原价 or 新价)
  │   ├→ 创建续费订单 (order_type=RENEWAL)
  │   ├→ 支付成功后延长 end_date
  │   └→ 重新锁定价格 (price_locked_until = new_end_date)
  │
  └→ 到期未续费:
      ├→ 更新订阅状态 (status=EXPIRED)
      ├→ 发送 Webhook 通知业务系统停用功能
      └→ 继续每日提醒 (可配置最长提醒天数)
```

### 5.3 试用流程（含延长试用）

```
客户申请试用
  │
  ├→ 校验: 该客户是否已有此产品/套餐的活跃试用
  ├→ 校验: 产品/套餐是否开启试用
  │
  ├→ 创建试用记录 (t_billing_trial, status=ACTIVE)
  ├→ 创建订阅 (t_billing_subscription, status=TRIAL, is_trial=1)
  ├→ 发送 Webhook 通知业务系统开通试用
  │
  ├→ 试用到期前 3 天:
  │   ├→ 发送提醒 "试用即将到期，请购买正式版"
  │   ├→ 附上联系方式和购买链接
  │   └→ 若支持延长试用，提醒中附上 "申请延长试用" 链接
  │
  ├→ 申请延长试用 (需审批):
  │   ├→ 销售员/客户发起延长申请
  │   ├→ 校验: 产品/套餐是否允许延长试用 (trial_extend_enabled)
  │   ├→ 校验: 累计已延长天数 + 本次申请天数 <= trial_max_extend_days
  │   ├→ 创建审批单 (t_billing_trial_extend_approval, status=PENDING)
  │   ├→ 通知主管审批 (短信 + 应用内)
  │   │
  │   ├→ [主管审批通过]:
  │   │   ├→ 更新审批单 status=APPROVED
  │   │   ├→ 延长 trial.end_date 和 subscription.end_date
  │   │   ├→ 更新 trial.extend_count + 1, trial.total_extend_days += 延长天数
  │   │   ├→ 发送短信通知客户 "延长试用已批准"
  │   │   └→ 发送 Webhook 通知业务系统延长试用
  │   │
  │   └→ [主管审批驳回]:
  │       ├→ 更新审批单 status=REJECTED + 驳回原因
  │       └→ 通知销售员/客户审批结果
  │
  ├→ 试用到期 (包括延长后再次到期):
  │   ├→ 更新试用状态 (status=EXPIRED)
  │   ├→ 更新订阅状态 (status=EXPIRED)
  │   ├→ 发送提醒 "试用已到期，请购买正式版继续使用"
  │   ├→ 附上联系方式 + 购买链接
  │   └→ 发送 Webhook 通知业务系统关闭试用
  │
  └→ 客户购买正式版:
      ├→ 更新试用状态 (status=CONVERTED)
      └→ 更新订阅状态为 ACTIVE + 设置正式起止日期
```

### 5.4 价格计算引擎

```
输入: 购物车项 (产品/套餐 + 数量 + 周期) + 客户积分余额
  │
  ├→ Step 1: 基础价格计算
  │   对每个购物车项:
  │     ├→ 查询匹配的定价方案 (优先级 + 生效时间)
  │     └→ 按收费模式计算基础金额
  │         ├→ ONE_TIME_ANNUAL: software_fee + annual_service_fee × 年数
  │         ├→ SUBSCRIPTION: period_price × 周期数
  │         ├→ USAGE_BASED: unit_price × 用量
  │         ├→ CLOUD_RENTAL: rental_price × 周期数
  │         ├→ SPACE_RENTAL: space_unit_price × 空间量 × 周期数
  │         ├→ TIERED_PROGRESSIVE: 按阶梯区间累进计算
  │         └→ ONE_TIME: one_time_price (一次性)
  │
  ├→ Step 2: 应用折扣
  │   ├→ 产品/套餐维度折扣 (discount_rate × 原价)
  │   └→ 订单满额折扣 (总金额 >= threshold_amount 时)
  │
  ├→ Step 3: 匹配赠送
  │   ├→ 产品购买数量满足 → 赠送产品/套餐/积分
  │   └→ 订单金额满足 → 赠送积分
  │
  ├→ Step 4: 积分抵扣
  │   ├→ 校验产品是否支持积分支付
  │   ├→ 计算最大可抵扣金额 = 实付金额 × max_points_ratio
  │   ├→ 按 points_exchange_rate 换算所需积分
  │   └→ 校验客户积分余额是否足够
  │
  └→ 输出: 价格明细
      ├→ 原价总额
      ├→ 折扣总额
      ├→ 赠品列表
      ├→ 积分可抵扣金额 & 所需积分
      └→ 实付金额
```

### 5.5 续费定价逻辑

```
客户发起续费
  │
  ├→ 查询订阅记录的 price_locked_until
  │
  ├→ IF today <= price_locked_until:
  │   └→ 续费价格 = renewal_price (保持原价)
  │
  ├→ ELSE (价格锁定已过期):
  │   ├→ 查询该产品/套餐当前生效的最新定价方案
  │   └→ 续费价格 = 最新定价方案的价格
  │
  └→ 生成续费订单, 支付后:
      ├→ 延长 end_date
      ├→ 更新 renewal_price = 本次支付价格
      └→ 更新 price_locked_until = new_end_date
```

---

## 六、API 设计

### 6.1 管理后台 API (Admin)

| 方法 | 路径 | 说明 |
|------|------|------|
| **产品管理** | | |
| POST | `/admin/billing/products` | 创建产品 |
| PUT | `/admin/billing/products/{id}` | 更新产品 |
| GET | `/admin/billing/products` | 产品列表 |
| GET | `/admin/billing/products/{id}` | 产品详情 |
| DELETE | `/admin/billing/products/{id}` | 删除产品 |
| **套餐管理** | | |
| POST | `/admin/billing/packages` | 创建套餐 |
| PUT | `/admin/billing/packages/{id}` | 更新套餐 |
| GET | `/admin/billing/packages` | 套餐列表 |
| DELETE | `/admin/billing/packages/{id}` | 删除套餐 |
| POST | `/admin/billing/packages/{id}/items` | 添加套餐项 |
| DELETE | `/admin/billing/packages/{id}/items/{itemId}` | 移除套餐项 |
| **定价方案** | | |
| POST | `/admin/billing/pricing-plans` | 创建定价方案 |
| PUT | `/admin/billing/pricing-plans/{id}` | 更新定价方案 |
| GET | `/admin/billing/pricing-plans` | 定价方案列表 |
| DELETE | `/admin/billing/pricing-plans/{id}` | 删除方案 |
| **折扣规则** | | |
| POST | `/admin/billing/discount-rules` | 创建折扣规则 |
| PUT | `/admin/billing/discount-rules/{id}` | 更新折扣规则 |
| GET | `/admin/billing/discount-rules` | 折扣规则列表 |
| DELETE | `/admin/billing/discount-rules/{id}` | 删除规则 |
| **赠送规则** | | |
| POST | `/admin/billing/gift-rules` | 创建赠送规则 |
| PUT | `/admin/billing/gift-rules/{id}` | 更新赠送规则 |
| GET | `/admin/billing/gift-rules` | 赠送规则列表 |
| DELETE | `/admin/billing/gift-rules/{id}` | 删除规则 |
| **订单管理** | | |
| POST | `/admin/billing/orders/proxy` | 代客下单 (后台为客户创建订单) |
| GET | `/admin/billing/orders` | 订单列表 (支持按客户/状态/时间/来源筛选) |
| GET | `/admin/billing/orders/{id}` | 订单详情 |
| POST | `/admin/billing/orders/{id}/confirm-payment` | 后台确认付款 (线下支付/代客收款) |
| POST | `/admin/billing/orders/{id}/cancel` | 取消订单 |
| **订阅管理** | | |
| GET | `/admin/billing/subscriptions` | 订阅列表 (支持按客户/状态/到期时间筛选) |
| GET | `/admin/billing/subscriptions/{id}` | 订阅详情 |
| POST | `/admin/billing/subscriptions/{id}/extend` | 手动延期 |
| POST | `/admin/billing/subscriptions/{id}/suspend` | 暂停订阅 |
| POST | `/admin/billing/subscriptions/{id}/resume` | 恢复订阅 |
| **试用管理** | | |
| GET | `/admin/billing/trials` | 试用列表 (支持按状态/客户筛选) |
| **试用延长审批** | | |
| POST | `/admin/billing/trial-extend/apply` | 销售员提交延长试用申请 |
| GET | `/admin/billing/trial-extend/my-applies` | 我的申请列表 (销售员查看) |
| GET | `/admin/billing/trial-extend/pending` | 待审批列表 (主管查看) |
| POST | `/admin/billing/trial-extend/{id}/approve` | 审批通过 |
| POST | `/admin/billing/trial-extend/{id}/reject` | 审批驳回 |
| GET | `/admin/billing/trial-extend` | 全部审批记录 |
| **提醒记录** | | |
| GET | `/admin/billing/reminders` | 提醒记录列表 |
| **运营报表** | | |
| GET | `/admin/billing/reports/expiring-subscriptions` | 即将到期的订阅报表 (15天内) |
| GET | `/admin/billing/reports/expired-subscriptions` | 已到期的订阅报表 |
| GET | `/admin/billing/reports/expiring-trials` | 即将到期的试用报表 |
| GET | `/admin/billing/reports/expired-trials` | 已到期的试用报表 |
| GET | `/admin/billing/reports/subscription-detail/{id}` | 订阅详情报表 (含用量/时间/空间多维度) |
| GET | `/admin/billing/reports/usage-ledger` | 用量流水明细 (按客户/订阅筛选) |
| GET | `/admin/billing/reports/revenue-summary` | 收入汇总报表 (按日/周/月) |
| GET | `/admin/billing/reports/customer-assets/{customerId}` | 客户资产全景 (所有产品/套餐/用量/到期) |

### 6.2 客户前台 API (Customer)

| 方法 | 路径 | 说明 |
|------|------|------|
| **商品浏览** | | |
| GET | `/api/v1/billing/products` | 可购买产品列表 (上架的) |
| GET | `/api/v1/billing/products/{id}` | 产品详情 + 定价方案 |
| GET | `/api/v1/billing/packages` | 可购买套餐列表 |
| GET | `/api/v1/billing/packages/{id}` | 套餐详情 + 定价方案 + 明细 |
| **价格计算** | | |
| POST | `/api/v1/billing/calculate-price` | 购物车价格预览 (折扣/赠送/积分抵扣) |
| **下单支付** | | |
| POST | `/api/v1/billing/orders` | 创建订单 |
| POST | `/api/v1/billing/orders/{orderNo}/pay` | 发起支付 |
| POST | `/api/v1/billing/orders/pay-callback` | 支付回调 |
| GET | `/api/v1/billing/orders/{orderNo}` | 查询订单状态 |
| GET | `/api/v1/billing/orders` | 我的订单列表 |
| **我的订阅** | | |
| GET | `/api/v1/billing/my/subscriptions` | 我的订阅列表 (含到期状态/剩余量) |
| GET | `/api/v1/billing/my/subscriptions/{id}` | 订阅详情 |
| POST | `/api/v1/billing/my/subscriptions/{id}/renew` | 发起续费 |
| **试用** | | |
| POST | `/api/v1/billing/trial/apply` | 申请试用 |
| POST | `/api/v1/billing/trial/{id}/extend` | 申请延长试用 (提交审批) |
| GET | `/api/v1/billing/trial/{id}/extend-status` | 查询延长审批状态 |
| GET | `/api/v1/billing/my/trials` | 我的试用列表 |
| **用量明细** | | |
| GET | `/api/v1/billing/my/usage-ledger/{subscriptionId}` | 查询用量流水明细 |
| **登录提醒** | | |
| GET | `/api/v1/billing/my/expiring-alerts` | 获取到期/续费提醒 (登录时调用) |

### 6.3 外部系统查询 API (签名认证)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/billing/check/product` | 查询客户的指定产品是否到期 |
| POST | `/api/v1/billing/check/products` | 批量查询客户多个产品的到期状态 |
| GET | `/api/v1/billing/check/subscription/{subscriptionNo}` | 查询指定订阅的详细状态 |
| POST | `/api/v1/billing/usage/report` | 上报使用量 (按量计费的产品) |

**产品到期查询请求示例：**

```json
POST /api/v1/billing/check/products
{
  "customerId": "C10001",
  "productCodes": ["steel_erp_user", "cloud_storage_100g"]
}
```

**响应示例：**

```json
{
  "code": 0,
  "data": {
    "customerId": "C10001",
    "results": [
      {
        "productCode": "steel_erp_user",
        "productName": "钢贸宝企业版用户",
        "status": "ACTIVE",
        "quantity": 10,
        "startDate": "2026-01-01",
        "endDate": "2026-12-31",
        "daysRemaining": 312,
        "needRenewal": false,
        "renewalPrice": 200000,
        "renewalPriceDisplay": "2000.00元",
        "usageQuota": 0,
        "usageUsed": 0
      },
      {
        "productCode": "cloud_storage_100g",
        "productName": "100GB云存储",
        "status": "EXPIRING",
        "quantity": 1,
        "startDate": "2025-03-01",
        "endDate": "2026-03-10",
        "daysRemaining": 7,
        "needRenewal": true,
        "renewalPrice": 50000,
        "renewalPriceDisplay": "500.00元",
        "usageQuota": 107374182400,
        "usageUsed": 53687091200,
        "usageRemaining": 53687091200
      }
    ]
  }
}
```

---

## 七、到期提醒调度设计

### 7.1 定时任务配置

```
┌─────────────────────────────────────────────────────┐
│              到期提醒定时任务                          │
│                                                     │
│  触发时间: 每天 14:00                                 │
│  分布式锁: Redis SETNX "billing:reminder:lock"       │
│  锁超时: 30分钟                                      │
│                                                     │
│  Step 1: 扫描 15天内到期的 ACTIVE 订阅                │
│  Step 2: 对每条订阅:                                  │
│    ├─ 检查今日是否已发过同类型提醒 (防重复)             │
│    ├─ 生成提醒内容 (含续费价格 + 操作链接)             │
│    ├─ 发送短信 (SMS)                                  │
│    ├─ 发送应用内通知 (调用通知服务)                     │
│    └─ 记录提醒日志                                    │
│  Step 3: 扫描已到期未续费的订阅 (到期后30天内)         │
│    ├─ 更新状态为 EXPIRED                              │
│    ├─ 发送 "已到期请续费" 提醒                         │
│    └─ 发送 Webhook 通知业务系统                        │
│  Step 4: 扫描试用即将到期 (3天内)                      │
│    ├─ 发送试用到期提醒 + 联系方式                      │
│    └─ 试用过期后更新状态                               │
└─────────────────────────────────────────────────────┘
```

### 7.2 登录时提醒

```
客户登录 → 前端调用 GET /api/v1/billing/my/expiring-alerts
  │
  ├→ 查询 Redis 缓存 (key: "billing:alerts:{customerId}")
  │   ├→ 缓存命中 → 直接返回
  │   └→ 缓存未命中 → 查询数据库
  │
  ├→ 查询条件:
  │   ├─ status = ACTIVE 且 end_date <= today + 15天
  │   ├─ status = EXPIRED 且 end_date >= today - 30天
  │   └─ is_trial = 1 且 end_date <= today + 3天
  │
  ├→ 返回提醒列表:
  │   ├─ 产品/套餐名称
  │   ├─ 到期日期
  │   ├─ 剩余天数 (负数表示已过期)
  │   ├─ 续费价格
  │   └─ 续费链接
  │
  └→ 写入 Redis 缓存 (TTL: 1小时)
```

---

## 八、积分抵扣设计

### 8.1 积分抵扣规则

```
┌──────────────────────────────────┐
│         积分抵扣计算               │
│                                  │
│  输入:                            │
│    商品实付金额 (折扣后)            │
│    客户积分余额                    │
│    产品积分配置:                    │
│      points_payable (是否支持)     │
│      max_points_ratio (最大比例)   │
│      points_exchange_rate (汇率)  │
│                                  │
│  计算:                            │
│    最大可抵扣金额                   │
│      = 实付金额 × max_points_ratio│
│    所需积分                        │
│      = 最大可抵扣金额              │
│        × points_exchange_rate    │
│    实际可用积分                     │
│      = min(所需积分, 客户积分余额)  │
│    实际抵扣金额                     │
│      = 实际可用积分                │
│        / points_exchange_rate    │
│                                  │
│  输出:                            │
│    积分抵扣金额                    │
│    使用积分数量                    │
│    还需支付金额                    │
└──────────────────────────────────┘
```

### 8.2 示例

```
场景: 购买钢贸宝企业版 10 用户, 单价 2000 元
  max_points_ratio = 0.50 (最多抵扣50%)
  points_exchange_rate = 100 (100积分 = 1元)
  客户积分余额 = 50000

计算:
  商品金额 = 20000 元
  最大可抵扣金额 = 20000 × 0.50 = 10000 元
  所需积分 = 10000 × 100 = 1000000
  实际可用积分 = min(1000000, 50000) = 50000
  实际抵扣金额 = 50000 / 100 = 500 元
  还需支付 = 20000 - 500 = 19500 元
```

---

## 九、运营报表设计

### 9.1 报表总览

管理后台的报表中心提供多维度的运营数据视图：

```
┌──────────────────────────────────────────────────────────────┐
│                      报表中心                                 │
│                                                              │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐│
│  │ 即将到期    │ │ 已到期     │ │ 即将到期    │ │ 已到期     ││
│  │ 订阅报表   │ │ 订阅报表   │ │ 试用报表   │ │ 试用报表   ││
│  │  32 条     │ │  15 条    │ │  8 条     │ │  5 条     ││
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘│
│                                                              │
│  ┌──────────────────────────────────────────────────────────┐│
│  │                   客户资产全景                             ││
│  │  搜索客户: [__________] [查询]                            ││
│  │                                                          ││
│  │  某某钢铁公司 (C10001)                                    ││
│  │  ┌──────────────────────────────────────────────────────┐││
│  │  │ 产品/套餐       │ 收费模式 │ 状态   │ 关键指标        │││
│  │  ├──────────────────────────────────────────────────────┤││
│  │  │ 钢贸宝企业版×10  │ 订阅     │ ● 正常 │ 剩余312天/365天│││
│  │  │ API调用包        │ 按量     │ ● 正常 │ 剩余3420次/5000│││
│  │  │ 100GB云存储      │ 空间租用 │ ⚠ 即将 │ 已用53GB/100GB ││ │
│  │  │ 数据报告高级版    │ 买断     │ ● 永久 │ —             │││
│  │  │ CRM标准版 (试用)  │ 订阅     │ 试用   │ 剩余5天       │││
│  │  └──────────────────────────────────────────────────────┘││
│  └──────────────────────────────────────────────────────────┘│
│                                                              │
│  ┌──────────────────────────────────────────────────────────┐│
│  │                   用量流水明细                             ││
│  │  订阅: API调用包 (SUB20260101001)                         ││
│  │  购买总量: 5000次  已用: 1580次  剩余: 3420次              ││
│  │  ┌────────┬──────┬──────┬──────┬──────────────────────┐  ││
│  │  │ 时间    │ 动作  │ 数量  │ 剩余  │ 说明               │  ││
│  │  ├────────┼──────┼──────┼──────┼──────────────────────┤  ││
│  │  │ 03-10  │ 消耗  │ -120  │ 3420 │ 钢贸宝批量查询      │  ││
│  │  │ 03-09  │ 消耗  │ -85   │ 3540 │ CRM数据同步         │  ││
│  │  │ 03-01  │ 充值  │ +5000 │ 3625 │ 购买API调用包        │  ││
│  │  └────────┴──────┴──────┴──────┴──────────────────────┘  ││
│  └──────────────────────────────────────────────────────────┘│
│                                                              │
│  ┌──────────────────────────────────────────────────────────┐│
│  │                 收入汇总报表                               ││
│  │  时间范围: [2026-01] ~ [2026-03]  粒度: [月]              ││
│  │  ┌────────┬────────┬────────┬────────┬────────┐          ││
│  │  │ 月份    │ 新购收入 │ 续费收入 │ 增量收入 │ 合计    │          ││
│  │  ├────────┼────────┼────────┼────────┼────────┤          ││
│  │  │ 2026-01│ 12.5万  │ 8.3万   │ 1.2万   │ 22.0万 │          ││
│  │  │ 2026-02│ 15.0万  │ 6.7万   │ 2.1万   │ 23.8万 │          ││
│  │  │ 2026-03│ 10.2万  │ 9.1万   │ 1.8万   │ 21.1万 │          ││
│  │  └────────┴────────┴────────┴────────┴────────┘          ││
│  └──────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────┘
```

### 9.2 订阅详情报表 — 按收费模式展示不同指标

| 收费模式 | 显示的核心指标 |
|---------|--------------|
| **一次性+年服务费** | 购买总天数、已使用天数、剩余天数、年服务费到期日 |
| **年/季/月订阅** | 购买总天数、已使用天数、剩余天数、当前周期起止 |
| **按量计费** | 购买总量、已用量、剩余可用量、单位、费用流水明细 |
| **阶梯累进** | 购买总量、已用量、剩余可用量、当前所在阶梯、费用流水 |
| **云端租用** | 购买总天数、已使用天数、剩余天数、租用周期 |
| **空间租用** | 购买总空间、已用空间、剩余空间（GB/TB）、使用率% |
| **一次性买断** | 购买日期、状态=永久有效 |

### 9.3 订阅详情响应示例

```json
{
  "code": 0,
  "data": {
    "subscriptionNo": "SUB20260101001",
    "customerName": "某某钢铁公司",
    "sourceName": "API调用包",
    "pricingModel": "USAGE_BASED",
    "status": "ACTIVE",

    "timeMetrics": {
      "startDate": "2026-01-01",
      "endDate": "2026-12-31",
      "totalDays": 365,
      "daysUsed": 53,
      "daysRemaining": 312
    },

    "usageMetrics": {
      "unit": "次",
      "totalQuota": 5000,
      "used": 1580,
      "remaining": 3420,
      "usageRate": "31.6%"
    },

    "spaceMetrics": null,

    "recentUsageLedger": [
      {
        "time": "2026-03-10 14:30:00",
        "action": "CONSUME",
        "quantity": -120,
        "balanceAfter": 3420,
        "unitPrice": 10,
        "amount": 1200,
        "bizSystem": "钢贸宝",
        "description": "批量查询接口调用"
      }
    ]
  }
}
```

### 9.4 空间类订阅详情响应示例

```json
{
  "code": 0,
  "data": {
    "subscriptionNo": "SUB20260201002",
    "sourceName": "100GB云存储",
    "pricingModel": "SPACE_RENTAL",
    "status": "ACTIVE",

    "timeMetrics": {
      "startDate": "2026-02-01",
      "endDate": "2027-01-31",
      "totalDays": 365,
      "daysUsed": 20,
      "daysRemaining": 345
    },

    "usageMetrics": null,

    "spaceMetrics": {
      "unit": "GB",
      "totalSpace": "100 GB",
      "totalSpaceBytes": 107374182400,
      "usedSpace": "53.2 GB",
      "usedSpaceBytes": 57124225843,
      "remainingSpace": "46.8 GB",
      "remainingSpaceBytes": 50249956557,
      "usageRate": "53.2%"
    }
  }
}
```

---

## 十、性能设计 (10万+并发)

### 9.1 高并发策略

| 策略 | 实现 | 场景 |
|------|------|------|
| **Java 21 虚拟线程** | `spring.threads.virtual.enabled=true` | 所有请求处理 |
| **Redis 缓存** | 订阅状态、产品定价、到期提醒 | 热点数据查询 |
| **数据库读写分离** | MySQL 主从 + Spring @Transactional(readOnly) | 查询密集场景 |
| **乐观锁** | `version` 字段 + `WHERE version = ?` | 订阅/订单更新 |
| **连接池** | HikariCP max-pool-size=200 | 数据库连接 |
| **索引优化** | 复合索引覆盖高频查询 | 到期查询/客户查询 |
| **批量处理** | 到期提醒分批扫描, batch_size=500 | 定时任务 |
| **分布式锁** | Redis SETNX | 定时任务/订单支付 |
| **异步处理** | 通知/Webhook/赠送 异步执行 | 支付成功后 |

### 9.2 缓存策略

```
缓存键设计:

1. 产品定价缓存
   Key: billing:pricing:{productId}
   TTL: 10分钟
   场景: 商品列表/价格计算

2. 订阅状态缓存
   Key: billing:sub:{customerId}:{productCode}
   TTL: 5分钟
   场景: 外部系统查询到期状态 (最高频)

3. 客户到期提醒缓存
   Key: billing:alerts:{customerId}
   TTL: 1小时
   场景: 登录时查询提醒

4. 折扣规则缓存
   Key: billing:discount:rules:{tenantId}
   TTL: 10分钟
   场景: 价格计算时应用折扣

5. 赠送规则缓存
   Key: billing:gift:rules:{tenantId}
   TTL: 10分钟
   场景: 下单时匹配赠送
```

### 9.3 数据库索引设计要点

```sql
-- 到期扫描核心索引 (每日定时任务)
CREATE INDEX idx_sub_expiry ON t_billing_subscription(tenant_id, status, end_date);

-- 外部系统查询核心索引 (最高频)
CREATE INDEX idx_sub_product_check ON t_billing_subscription(
    tenant_id, customer_id, source_type, source_id, status);

-- 客户订阅列表
CREATE INDEX idx_sub_customer ON t_billing_subscription(tenant_id, customer_id, status);

-- 订单查询
CREATE INDEX idx_order_customer ON t_billing_order(tenant_id, customer_id, created_at DESC);

-- 提醒去重
CREATE INDEX idx_reminder_dedup ON t_billing_renewal_reminder(
    subscription_id, reminder_type, DATE(created_at));

-- 用量流水查询
CREATE INDEX idx_usage_ledger_sub ON t_billing_usage_ledger(subscription_id, created_at DESC);
CREATE INDEX idx_usage_ledger_customer ON t_billing_usage_ledger(tenant_id, customer_id, created_at DESC);

-- 试用延长审批
CREATE INDEX idx_trial_extend_status ON t_billing_trial_extend_approval(tenant_id, status);
CREATE INDEX idx_trial_extend_approver ON t_billing_trial_extend_approval(tenant_id, approver_id, status);
```

---

## 十、与现有底座系统的集成

### 10.1 与积分系统集成

| 场景 | 调用方式 |
|------|---------|
| 积分抵扣货款 | 下单支付时调用积分系统扣减接口 |
| 订单满额送积分 | 支付成功后调用积分系统增加接口 |
| 赠送积分 | 赠送规则触发时调用积分系统增加接口 |
| 查询积分余额 | 价格计算时查询客户积分余额 |

### 10.2 与充值/余额系统集成

| 场景 | 调用方式 |
|------|---------|
| 余额支付 | 支付时调用钱包消费接口 |
| 退款返还 | 退款时调用钱包充值接口 |

### 10.3 与通知服务集成

| 场景 | 调用方式 |
|------|---------|
| 到期提醒 | 调用通知服务发送邮件/短信/应用内通知 |
| 购买成功 | 调用通知服务发送购买确认 |
| 试用到期 | 调用通知服务发送试用到期提醒 |

### 10.4 与 Webhook 集成

| 事件 | 说明 |
|------|------|
| `billing.order.paid` | 订单支付成功 |
| `billing.subscription.activated` | 订阅激活 |
| `billing.subscription.expiring` | 订阅即将到期 |
| `billing.subscription.expired` | 订阅已到期 |
| `billing.trial.started` | 试用开始 |
| `billing.trial.expiring` | 试用即将到期 |
| `billing.trial.expired` | 试用到期 |
| `billing.usage.threshold` | 用量达到阈值 |

---

## 十一、项目目录结构 (计费模块)

```
module/billing/
├── entity/
│   ├── BillingProduct.java
│   ├── BillingPackage.java
│   ├── BillingPackageItem.java
│   ├── BillingPricingPlan.java
│   ├── BillingDiscountRule.java
│   ├── BillingGiftRule.java
│   ├── BillingOrder.java
│   ├── BillingOrderItem.java
│   ├── BillingSubscription.java
│   ├── BillingTrial.java
│   ├── BillingTrialExtendApproval.java
│   ├── BillingUsageLedger.java
│   └── BillingRenewalReminder.java
├── repository/
│   ├── BillingProductRepository.java
│   ├── BillingPackageRepository.java
│   ├── BillingUsageLedgerRepository.java
│   ├── BillingTrialExtendApprovalRepository.java
│   ├── ... (每个实体对应)
│   └── BillingSubscriptionRepository.java
├── service/
│   ├── ProductService.java              // 产品管理
│   ├── PackageService.java              // 套餐管理
│   ├── PricingService.java              // 定价方案管理
│   ├── PriceCalculator.java             // 价格计算引擎 (折扣/赠送/积分)
│   ├── OrderService.java                // 订单管理 (创建/支付/代客下单)
│   ├── SubscriptionService.java         // 订阅管理 (激活/续费/到期)
│   ├── TrialService.java                // 试用管理
│   ├── TrialExtendApprovalService.java  // 试用延长审批
│   ├── UsageLedgerService.java          // 用量流水管理
│   ├── RenewalReminderService.java      // 到期提醒
│   ├── DiscountEngine.java              // 折扣引擎
│   ├── GiftEngine.java                  // 赠送引擎
│   ├── BillingCheckService.java         // 外部系统到期查询
│   ├── BillingReportService.java        // 运营报表
│   └── TencentSmsService.java           // 腾讯云短信
├── controller/
│   ├── admin/
│   │   ├── ProductAdminController.java
│   │   ├── PackageAdminController.java
│   │   ├── PricingAdminController.java
│   │   ├── DiscountAdminController.java
│   │   ├── GiftAdminController.java
│   │   ├── OrderAdminController.java
│   │   ├── SubscriptionAdminController.java
│   │   ├── TrialExtendApprovalController.java  // 试用延长审批
│   │   ├── ReportAdminController.java          // 运营报表
│   │   └── ReminderAdminController.java
│   └── api/
│       ├── BillingShopController.java       // 客户浏览/购买
│       ├── BillingOrderController.java      // 客户订单
│       ├── BillingMyController.java         // 我的订阅/提醒/用量
│       ├── BillingTrialController.java      // 试用申请/延长
│       └── BillingCheckController.java      // 外部系统查询
├── dto/
│   ├── ProductDTO.java
│   ├── PackageDTO.java
│   ├── PricingDTO.java
│   ├── OrderDTO.java
│   ├── SubscriptionDTO.java
│   ├── PriceCalculateDTO.java
│   ├── BillingCheckDTO.java
│   ├── UsageLedgerDTO.java
│   ├── TrialExtendDTO.java
│   └── ReportDTO.java
├── scheduler/
│   ├── RenewalReminderScheduler.java    // 到期提醒定时任务
│   └── SubscriptionDaysUpdateScheduler.java  // 每日更新已用天数
└── event/
    └── BillingEvents.java             // 计费相关事件定义
```

---

## 十二、腾讯云短信 (Tencent Cloud SMS) 集成

### 12.1 对接方案

```
┌──────────────────────────────────────────────────┐
│              腾讯云短信集成                         │
│                                                  │
│  SDK: tencentcloud-sdk-java-sms                  │
│  认证: SecretId + SecretKey                       │
│  短信签名: 需在腾讯云控制台申请审核                  │
│  模板: 在腾讯云控制台配置模板，获取模板ID             │
│                                                  │
│  接入层:                                          │
│  ┌──────────────────────┐                        │
│  │  SmsService          │                        │
│  │  ├ sendSms()         │──→ 腾讯云SMS API       │
│  │  ├ sendBatchSms()    │    (qcloudapi)         │
│  │  └ checkBalance()    │                        │
│  └──────────────────────┘                        │
│                                                  │
│  配置 (application.yml):                          │
│    tencent.sms.secret-id: xxx                    │
│    tencent.sms.secret-key: xxx                   │
│    tencent.sms.sdk-app-id: xxx                   │
│    tencent.sms.sign-name: "CoBaseSys"            │
└──────────────────────────────────────────────────┘
```

### 12.2 短信模板规划

| 场景 | 模板示例 | 模板变量 |
|------|---------|---------|
| 订阅到期提醒 | 您好，您的{1}将于{2}到期，续费价格{3}元，请及时续费。 | 产品名, 到期日期, 价格 |
| 订阅已过期 | 您好，您的{1}已于{2}到期，为避免影响使用请尽快续费。联系电话：{3} | 产品名, 到期日期, 联系方式 |
| 试用到期提醒 | 您好，您试用的{1}将于{2}到期，如需继续使用请购买正式版。{3} | 产品名, 到期日期, 联系方式 |
| 试用延长通知 | 您好，您试用的{1}已延长{2}天，新到期日为{3}。 | 产品名, 延长天数, 新到期日 |
| 购买成功 | 您好，您已成功购买{1}，有效期至{2}，订单号：{3}。 | 产品名, 到期日期, 订单号 |
| 支付提醒 | 您好，您有一笔{1}的订单待支付，金额{2}元，请及时完成支付。 | 产品名, 金额 |

---

## 十三、H5移动端设计

### 13.1 客户购买前台 (H5 移动优先)

采用 **Vue 3 + Vant 4** 构建独立的客户前台，移动端优先设计，PC端自适应。

```
frontend-h5/                            # 客户H5前台
├── package.json
├── vite.config.js
├── src/
│   ├── main.js
│   ├── api/                            # API 请求
│   ├── router/                         # 路由
│   ├── stores/                         # 客户状态 (Pinia)
│   ├── styles/                         # 全局样式
│   └── views/
│       ├── home/                       # 商城首页
│       │   └── Index.vue               # 产品/套餐展示
│       ├── product/
│       │   └── Detail.vue              # 产品详情 + 定价 + 购买按钮
│       ├── package/
│       │   └── Detail.vue              # 套餐详情 + 包含产品 + 购买
│       ├── cart/
│       │   └── Index.vue               # 购物车 + 价格计算 + 积分抵扣
│       ├── order/
│       │   ├── Confirm.vue             # 订单确认 + 折扣/赠送预览
│       │   ├── Pay.vue                 # 支付页 (微信/支付宝/余额)
│       │   ├── Result.vue              # 支付结果
│       │   └── List.vue               # 我的订单列表
│       ├── subscription/
│       │   ├── List.vue               # 我的订阅 (到期状态/剩余量)
│       │   ├── Detail.vue             # 订阅详情
│       │   └── Renew.vue              # 续费页面
│       ├── trial/
│       │   ├── Apply.vue              # 申请试用
│       │   ├── Extend.vue             # 申请延长试用
│       │   └── List.vue               # 我的试用
│       ├── alert/
│       │   └── Index.vue              # 到期提醒中心
│       └── user/
│           └── Login.vue              # 客户登录
```

### 13.2 关键H5页面设计

#### 商城首页 (移动端)

```
┌──────────────────────┐
│  CoBaseSys 商城       │
│  ┌──────────────────┐│
│  │  搜索产品/套餐... ││
│  └──────────────────┘│
│                      │
│  ── 热门套餐 ──       │
│  ┌────┐ ┌────┐       │
│  │套餐A│ │套餐B│       │
│  │¥999│ │¥1999│      │
│  └────┘ └────┘       │
│                      │
│  ── 产品分类 ──       │
│  [软件] [服务] [资源]  │
│                      │
│  ┌──────────────────┐│
│  │ 钢贸宝企业版用户   ││
│  │ ¥2000/年          ││
│  │ [试用] [购买]     ││
│  └──────────────────┘│
│  ┌──────────────────┐│
│  │ 100GB云存储       ││
│  │ ¥500/年           ││
│  │ [购买]            ││
│  └──────────────────┘│
│                      │
│  ┌──┐┌──┐┌──┐┌──┐   │
│  │首页││订阅││订单││我的│  │
│  └──┘└──┘└──┘└──┘   │
└──────────────────────┘
```

#### 我的订阅 (移动端)

```
┌──────────────────────┐
│  我的订阅             │
│                      │
│  ┌──────────────────┐│
│  │ 钢贸宝企业版 ×10  ││
│  │ ● 正常使用        ││
│  │ 到期: 2026-12-31  ││
│  │ 剩余: 312天       ││
│  │ [续费]            ││
│  └──────────────────┘│
│                      │
│  ┌──────────────────┐│
│  │ 100GB云存储       ││
│  │ ⚠ 即将到期        ││
│  │ 到期: 2026-03-10  ││
│  │ 剩余: 7天  已用53%││
│  │ [立即续费]        ││
│  └──────────────────┘│
│                      │
│  ┌──────────────────┐│
│  │ API调用包 (试用中) ││
│  │ 试用到期: 3-15     ││
│  │ [延长试用] [购买]  ││
│  └──────────────────┘│
└──────────────────────┘
```

### 13.3 管理后台移动端适配

管理后台 (Element Plus) 通过以下方式支持 H5 移动端：

- Element Plus 内置响应式断点适配
- 表格在小屏幕自动横向滚动
- 表单在小屏幕自动堆叠排列
- 侧边栏在小屏幕自动收起为汉堡菜单
- 对话框在小屏幕自动全屏展示

---

## 十四、后台代客下单设计

### 14.1 业务场景

- 客户电话/线下沟通后，销售人员在后台为客户创建订单
- 客户线下付款（银行转账/现金），后台直接确认收款
- 为VIP客户提供特殊折扣或定制价格
- 批量为客户开通产品/套餐

### 14.2 代客下单流程

```
管理员操作:
  │
  ├→ 选择/输入客户信息 (customer_id + 客户名称)
  │
  ├→ 选择产品/套餐 + 数量 + 周期
  │
  ├→ 系统计算标准价格 (与客户自助一致)
  │
  ├→ 管理员可调整:
  │   ├→ 手动调整折扣率
  │   ├→ 手动调整实付金额
  │   └→ 添加备注 (说明调价原因)
  │
  ├→ 创建订单 (order_source=ADMIN, operator_id/name 记录)
  │
  ├→ 确认收款方式:
  │   ├→ [直接确认] 线下已收款 → 立即激活订阅
  │   ├→ [发给客户] 生成支付链接发给客户自行支付
  │   └→ [挂账待付] 创建订单但不支付，等待后续确认
  │
  └→ 支付确认后 → 同正常购买流程（激活订阅/赠送/通知）
```

### 14.3 Admin API 补充

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/admin/billing/orders/proxy` | 代客下单 |
| POST | `/admin/billing/orders/{id}/confirm-payment` | 确认收款 |
| POST | `/admin/billing/orders/{id}/generate-pay-link` | 生成支付链接发给客户 |

**代客下单请求示例：**

```json
POST /admin/billing/orders/proxy
{
  "customerId": "C10001",
  "customerName": "某某钢铁公司",
  "items": [
    {
      "itemType": "PRODUCT",
      "itemId": 1,
      "quantity": 10,
      "periodType": "YEAR",
      "periodCount": 1
    }
  ],
  "manualDiscountRate": 0.85,
  "usePoints": false,
  "remark": "老客户优惠",
  "autoConfirmPayment": true
}
```

---

## 十五、数据库选择说明 (MySQL 与 PostgreSQL)

当前底座系统使用 PostgreSQL，计费模块要求使用 MySQL。两种方案：

### 方案 A：双数据源 (推荐)

```
CoBaseSys 应用
├── 数据源1: PostgreSQL  ──→  积分/充值/会员/通知/Webhook 表
└── 数据源2: MySQL       ──→  计费模块所有 t_billing_* 表
```

- 使用 Spring Boot 多数据源配置
- `@Primary` 数据源为 PostgreSQL（现有模块）
- 计费模块 Repository 使用 `@Qualifier` 指定 MySQL 数据源
- 优点：不影响现有系统，计费数据独立

### 方案 B：全部迁移到 MySQL

- 将现有 PostgreSQL 表迁移到 MySQL
- 统一数据源
- 工作量大，有迁移风险

**建议采用方案 A**，双数据源互不干扰。

---

## 十六、确认清单

以下为已确认和待确认事项：

### 已确认事项 ✅

- [x] **试用机制**：支持延长试用，需销售员提交申请→主管审批通过后生效
- [x] **短信通道**：使用腾讯云短信平台 (Tencent Cloud SMS)
- [x] **收费模式**：在原有 5 种基础上新增 2 种（阶梯累进计费 + 一次性买断），共 7 种收费模式
- [x] **下单方式**：客户自助下单 + 后台代客下单，共用同一套价格引擎
- [x] **终端适配**：所有功能支持 H5 移动端操作，客户前台采用 Vant 4 移动优先，管理后台 Element Plus 响应式
- [x] **运营报表**：即将到期/已到期的订阅和试用报表，客户资产全景，用量流水明细，收入汇总
- [x] **用量流水**：按量/空间/时间计费的订阅分别展示剩余量/已用量/购买量，含明细账
- [x] **延长审批**：销售员提交延长试用申请→主管审批→通过后自动延长→短信通知客户

### 待确认事项

- [ ] **数据模型**：14 张核心表的字段设计是否满足需求？是否需要增减？
- [ ] **赠送规则**：买 N 送产品/套餐/积分三种赠送类型是否够用？
- [ ] **折扣规则**：产品折扣、满额折扣、满额送积分是否覆盖所有折扣场景？
- [ ] **积分抵扣**：按比例上限 + 汇率换算的方式是否可接受？
- [ ] **续费定价**：价格锁定到到期日的逻辑是否正确？（到期前续费保原价，过期后用新价）
- [ ] **提醒策略**：到期前 15 天开始、每天 14:00、短信+应用内，是否需要调整？
- [ ] **支付方式**：余额支付/微信/支付宝/线下转账，是否需要其他方式？
- [ ] **数据库**：采用双数据源（PostgreSQL + MySQL）是否可接受？
- [ ] **外部 API**：到期查询和用量上报的接口设计是否满足业务系统需要？
- [ ] **性能**：Redis 缓存 + 虚拟线程 + 读写分离的方案是否可接受？
- [ ] **开发顺序**：建议先后端API → 管理后台前端 → 客户H5前台，是否可接受？
- [ ] **腾讯云SMS配置**：是否已有腾讯云账号和短信签名？需要我在代码中预留哪些模板？
