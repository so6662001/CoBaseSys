# CoBaseSys - 公司运营底座系统

CoBaseSys 是一套公司级运营底座系统，为旗下多个业务系统提供统一的 **积分服务**、**充值/余额服务**、**会员等级**、**通知服务** 和 **Webhook** 能力。各外部业务系统通过标准化 API 接入底座，实现用户资产的统一管理。

## 系统架构

```
┌──────────────────────────────────────────────────────────────────┐
│                      CoBaseSys 底座系统                           │
│                                                                  │
│  ┌────────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐ ┌────────┐│
│  │  积分服务   │ │ 钱包服务  │ │ 会员等级  │ │ 通知   │ │Webhook ││
│  │  Points    │ │ Wallet   │ │ Member   │ │ Notify │ │        ││
│  └─────┬──────┘ └────┬─────┘ └────┬─────┘ └───┬────┘ └───┬────┘│
│        └──────────────┴────────────┴───────────┴──────────┘     │
│                    事件驱动 (Spring Events)                       │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │   多租户 │ API签名认证 │ 外部系统注册中心 │ Redis缓存/限流  │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │            PostgreSQL         │        Redis              │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
          │              │              │              │
   ┌──────┴───┐   ┌──────┴───┐   ┌──────┴───┐   ┌──────┴───┐
   │ 业务系统A │   │ 业务系统B │   │ 业务系统C │   │ 业务系统D │
   └──────────┘   └──────────┘   └──────────┘   └──────────┘
```

## 技术栈

| 组件 | 技术 | 说明 |
|------|------|------|
| 语言 | Java 21 | 虚拟线程 (Project Loom) 支持百万级并发 |
| 框架 | Spring Boot 3.2 | 自动配置、虚拟线程原生支持 |
| 数据库 | PostgreSQL 16 | ACID 事务，高可靠性 |
| 缓存 | Redis 7 | 分布式锁、限流、热数据缓存 |
| ORM | Spring Data JPA + Hibernate | 乐观锁、Hibernate Filter 多租户 |
| 迁移 | Flyway | 数据库版本管理 |
| 文档 | SpringDoc OpenAPI | 自动生成 Swagger API 文档 |
| 分布式锁 | Redisson | 高性能 Redis 分布式锁 |
| 容器化 | Docker + Docker Compose | 一键启动开发环境 |

## 核心模块

### 1. 积分服务 (Points Service)
- 外部系统通过 API 触发积分变动
- 支持 4 种计算规则：固定值 / 按金额倍率 / 阶梯式 / 自定义表达式
- 每日/每月积分上限控制
- 完整的积分流水账和余额查询
- 幂等性设计，防止重复发放

### 2. 充值/余额服务 (Wallet Service)
- 用户统一钱包，支持充值、消费、退款、冻结/解冻
- 支持 4 种消费计费规则：固定费用 / 按量计费 / 阶梯定价 / 自定义
- 充值赠送促销活动
- 预授权冻结-消费-解冻流程
- 金额以"分"为单位存储，精确无误差

### 3. 会员等级 (Member Service)
- 根据累计积分和消费金额自动晋升会员等级
- 每个等级可配置积分倍率加成和消费折扣
- 事件驱动，积分/消费变动时自动评估升级

### 4. 通知服务 (Notification Service)
- 余额不足自动提醒
- 充值成功通知
- 会员升级通知
- 支持邮件 / 应用内 / 自定义渠道
- 可配置通知模板和触发规则

### 5. Webhook 支持
- 关键事件实时推送到外部系统
- 支持的事件：积分变动、余额变动、会员升级
- HMAC-SHA256 签名保证安全
- 失败自动重试（可配置最大重试次数）
- 完整的推送日志记录

### 6. 多租户
- 基于 tenant_id 的行级数据隔离
- Hibernate Filter 自动过滤查询
- 每个租户独立管理外部系统、规则、用户数据

## 高并发设计 (支持 100 万用户并发)

| 策略 | 实现 |
|------|------|
| **虚拟线程** | Java 21 虚拟线程，Spring Boot `spring.threads.virtual.enabled=true`，消除线程池瓶颈 |
| **连接池优化** | HikariCP `max-pool-size=100`，Tomcat `max-connections=20000` |
| **乐观锁** | 账户表 `version` 字段，`UPDATE ... WHERE version = ?` 避免行锁 |
| **Redis 缓存** | 热数据缓存、限流计数器、分布式锁、nonce 防重放 |
| **幂等设计** | `idempotent_key` UNIQUE 约束，重复请求直接返回 |
| **异步处理** | Webhook 推送和通知发送使用独立线程池异步执行 |
| **批量写入** | Hibernate batch insert/update，减少数据库往返 |
| **数据库索引** | 针对高频查询的复合索引优化 |
| **ZGC** | Docker 部署使用 ZGC 垃圾回收器，低延迟 |

## 快速启动

### 环境要求
- Java 21+
- Maven 3.8+
- Docker & Docker Compose (可选)

### 方式一：Docker Compose 启动 (推荐)

```bash
docker-compose up -d
```

服务启动后访问：
- 应用：http://localhost:8080
- API 文档：http://localhost:8080/swagger-ui.html

### 方式二：本地开发启动

1. 启动 PostgreSQL 和 Redis：
```bash
docker-compose up -d postgres redis
```

2. 编译运行：
```bash
mvn clean compile
mvn spring-boot:run
```

## 项目结构

```
CoBaseSys/
├── pom.xml                              # Maven 配置
├── docker-compose.yml                   # Docker Compose
├── Dockerfile                           # 容器构建
├── SYSTEM_DESIGN.md                     # 系统设计文档
├── src/main/
│   ├── java/com/cobasesys/
│   │   ├── CoBaseSysApplication.java    # 启动类
│   │   ├── config/                      # 配置类
│   │   │   ├── RedisConfig.java
│   │   │   ├── AsyncConfig.java
│   │   │   └── WebConfig.java
│   │   ├── common/                      # 通用组件
│   │   │   ├── model/                   # BaseEntity, ApiResponse, PageResult
│   │   │   ├── exception/              # BizException, ErrorCode, GlobalExceptionHandler
│   │   │   ├── util/                   # IdGenerator, SignatureUtil
│   │   │   ├── tenant/                 # TenantContext, TenantFilter, TenantAspect
│   │   │   └── auth/                   # ApiAuthFilter (HMAC签名认证)
│   │   └── module/
│   │       ├── system/                 # 租户 + 外部系统注册中心
│   │       ├── points/                 # 积分服务
│   │       ├── wallet/                 # 充值/余额服务
│   │       ├── member/                 # 会员等级
│   │       ├── notification/           # 通知服务
│   │       └── webhook/               # Webhook
│   └── resources/
│       ├── application.yml              # 应用配置
│       └── db/migration/
│           └── V1__init_schema.sql      # 数据库建表脚本
└── src/test/                            # 测试
```

## API 概览

### 开放 API（外部系统调用，需 HMAC 签名认证）

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 积分 | POST | `/api/v1/points/earn` | 增加积分 |
| 积分 | POST | `/api/v1/points/deduct` | 扣减积分 |
| 积分 | POST | `/api/v1/points/freeze` | 冻结积分 |
| 积分 | GET | `/api/v1/points/balance/{userId}` | 查询积分余额 |
| 积分 | GET | `/api/v1/points/transactions/{userId}` | 查询积分流水 |
| 钱包 | POST | `/api/v1/wallet/recharge` | 创建充值订单 |
| 钱包 | POST | `/api/v1/wallet/recharge/callback` | 充值回调 |
| 钱包 | POST | `/api/v1/wallet/consume` | 消费扣费 |
| 钱包 | POST | `/api/v1/wallet/freeze` | 冻结金额 |
| 钱包 | POST | `/api/v1/wallet/refund` | 退款 |
| 钱包 | GET | `/api/v1/wallet/balance/{userId}` | 查询余额 |
| 钱包 | GET | `/api/v1/wallet/transactions/{userId}` | 查询流水 |
| 钱包 | POST | `/api/v1/wallet/check-balance` | 预检查余额 |

### 管理后台 API（需管理员认证）

| 模块 | 路径前缀 | 功能 |
|------|---------|------|
| 租户 | `/admin/tenants` | 租户 CRUD |
| 外部系统 | `/admin/systems` | 系统注册/密钥管理 |
| 积分动作 | `/admin/points/actions` | 积分动作 CRUD |
| 积分规则 | `/admin/points/rules` | 积分规则 CRUD |
| 积分账户 | `/admin/points/accounts` | 查看积分账户 |
| 消费动作 | `/admin/wallet/actions` | 消费动作 CRUD |
| 消费规则 | `/admin/wallet/rules` | 消费规则 CRUD |
| 充值促销 | `/admin/wallet/promotions` | 充值赠送活动 |
| 手动调账 | `/admin/wallet/adjust` | 后台调账 |
| 会员等级 | `/admin/members/levels` | 会员等级 CRUD |
| 会员用户 | `/admin/members/users` | 查看用户会员信息 |
| 通知模板 | `/admin/notifications/templates` | 通知模板管理 |
| 通知规则 | `/admin/notifications/rules` | 通知触发规则 |
| 通知记录 | `/admin/notifications/records` | 发送记录查询 |
| Webhook | `/admin/webhooks` | Webhook CRUD |
| Webhook日志 | `/admin/webhooks/logs` | 推送日志 |

## API 签名认证

外部系统调用开放 API 时需在请求头中携带签名：

```
X-App-Key: {app_key}
X-Timestamp: {unix_timestamp_seconds}
X-Nonce: {random_uuid}
X-Signature: HMAC-SHA256(app_secret, METHOD\nPATH\nTIMESTAMP\nNONCE\nBODY_MD5)
X-Tenant-Id: {tenant_id}
```

### 签名示例 (Java)

```java
String method = "POST";
String path = "/api/v1/points/earn";
String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
String nonce = UUID.randomUUID().toString().replace("-", "");
String bodyMd5 = md5(requestBody);

String signContent = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + bodyMd5;
String signature = hmacSha256(appSecret, signContent);
```

## 配置说明

主要配置项在 `application.yml` 中：

```yaml
cobasesys:
  auth:
    timestamp-tolerance-seconds: 300   # 签名时间戳容差（秒）
    nonce-expire-seconds: 300          # nonce 过期时间
    admin-token: changeme-admin-token  # 管理后台Token
  webhook:
    max-retry: 3                       # Webhook最大重试次数
    retry-interval-seconds: 30         # 重试间隔
    timeout-seconds: 10                # 推送超时
  notification:
    low-balance-threshold: 1000        # 余额不足提醒阈值（分）
```

## License

MIT
