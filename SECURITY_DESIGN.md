# CoBaseSys 安全防护设计方案

## 一、威胁分析与防护总览

### 1.1 核心威胁清单

| 威胁等级 | 威胁类型 | 攻击场景 | 后果 |
|---------|---------|---------|------|
| **P0 致命** | 账务数据篡改 | 黑客入侵修改积分/余额/订单 | 资金损失、账务混乱 |
| **P0 致命** | 数据库直接入侵 | SQL注入 / DB弱密码 / 未授权访问 | 全部数据泄露和篡改 |
| **P0 致命** | 管理员账号劫持 | 暴力破解 / 钓鱼 / Session劫持 | 以管理员身份执行恶意操作 |
| **P1 严重** | API伪造调用 | 伪造签名 / 重放攻击 | 虚假充值、虚增积分 |
| **P1 严重** | 内部人员篡改 | 运维/DBA直接改数据库 | 账务异常无法追溯 |
| **P2 重要** | 敏感数据泄露 | 日志泄露 / 传输未加密 | 客户信息和交易数据暴露 |
| **P2 重要** | DDoS攻击 | 流量洪泛 | 系统不可用 |
| **P3 一般** | 越权访问 | 普通用户访问他人数据 | 数据隐私泄露 |

### 1.2 六层防护体系

```
┌─────────────────────────────────────────────────────────────┐
│                    第1层：网络与基础设施安全                     │
│  WAF防火墙 │ DDoS防护 │ VPC网络隔离 │ HTTPS强制 │ IP白名单    │
├─────────────────────────────────────────────────────────────┤
│                    第2层：身份认证与访问控制                     │
│  RBAC权限 │ MFA多因素认证 │ JWT+刷新令牌 │ 操作级鉴权         │
├─────────────────────────────────────────────────────────────┤
│                    第3层：API安全                              │
│  HMAC签名 │ 时间戳防重放 │ Nonce防重复 │ 限流 │ 输入校验       │
├─────────────────────────────────────────────────────────────┤
│                    第4层：账务数据防篡改 ★ 核心                  │
│  哈希链 │ 数字摘要 │ 只追加账本 │ 双入口记账 │ 自动对账         │
├─────────────────────────────────────────────────────────────┤
│                    第5层：审计追踪                              │
│  全操作日志 │ 独立审计库 │ 不可篡改 │ 异常告警               │
├─────────────────────────────────────────────────────────────┤
│                    第6层：数据库与存储安全                       │
│  透明加密 │ 最小权限 │ SQL注入防护 │ 备份加密 │ 数据脱敏       │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、账务数据防篡改设计（最核心）

这是整个安全设计的重中之重。即使黑客入侵了数据库，也必须能**发现**数据被篡改，并且让篡改**极其困难**。

### 2.1 哈希链（Hash Chain）—— 账务流水的"区块链"

每一笔账务流水（积分流水、钱包流水、用量流水）都通过哈希链串联起来，任何一条记录被修改都会导致链条断裂，从而被检测到。

```
                    哈希链原理
  ┌──────────┐    ┌──────────┐    ┌──────────┐
  │ 交易 #1   │    │ 交易 #2   │    │ 交易 #3   │
  │          │    │          │    │          │
  │ data_hash│    │ data_hash│    │ data_hash│
  │ prev_hash│──→ │ prev_hash│──→ │ prev_hash│
  │ = "0000" │    │ = #1.hash│    │ = #2.hash│
  │          │    │          │    │          │
  │ hash=SHA │    │ hash=SHA │    │ hash=SHA │
  │(data+prev│    │(data+prev│    │(data+prev│
  │ +secret) │    │ +secret) │    │ +secret) │
  └──────────┘    └──────────┘    └──────────┘

  如果 #2 的数据被篡改 → #2.hash 变化 → #3.prev_hash 不匹配 → 链断裂 → 告警
```

**实现方式：**

每张流水表增加以下字段：

```sql
ALTER TABLE t_point_transaction ADD COLUMN data_hash VARCHAR(64) NOT NULL COMMENT '本记录数据摘要';
ALTER TABLE t_point_transaction ADD COLUMN prev_hash VARCHAR(64) NOT NULL COMMENT '上一条记录的哈希';
ALTER TABLE t_point_transaction ADD COLUMN chain_hash VARCHAR(64) NOT NULL COMMENT '链哈希=SHA256(data_hash+prev_hash+密钥)';

ALTER TABLE t_wallet_transaction ADD COLUMN data_hash VARCHAR(64) NOT NULL;
ALTER TABLE t_wallet_transaction ADD COLUMN prev_hash VARCHAR(64) NOT NULL;
ALTER TABLE t_wallet_transaction ADD COLUMN chain_hash VARCHAR(64) NOT NULL;

ALTER TABLE t_billing_usage_ledger ADD COLUMN data_hash VARCHAR(64) NOT NULL;
ALTER TABLE t_billing_usage_ledger ADD COLUMN prev_hash VARCHAR(64) NOT NULL;
ALTER TABLE t_billing_usage_ledger ADD COLUMN chain_hash VARCHAR(64) NOT NULL;
```

**data_hash 计算方式（Java 伪代码）：**

```java
public String computeDataHash(PointTransaction tx) {
    String raw = tx.getTransactionNo()
        + "|" + tx.getAccountId()
        + "|" + tx.getDirection()
        + "|" + tx.getPoints()
        + "|" + tx.getBalanceBefore()
        + "|" + tx.getBalanceAfter()
        + "|" + tx.getCreatedAt().toString();
    return SHA256(raw);
}

public String computeChainHash(String dataHash, String prevHash) {
    // CHAIN_SECRET 存储在应用配置中，不在数据库里
    return HMAC_SHA256(CHAIN_SECRET, dataHash + "|" + prevHash);
}
```

### 2.2 只追加账本（Append-Only Ledger）

账务流水表在数据库层面禁止 UPDATE 和 DELETE：

```sql
-- MySQL 触发器：禁止修改积分流水
DELIMITER //
CREATE TRIGGER trg_point_tx_no_update
BEFORE UPDATE ON t_point_transaction
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'SECURITY: Point transaction records cannot be modified';
END //

CREATE TRIGGER trg_point_tx_no_delete
BEFORE DELETE ON t_point_transaction
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'SECURITY: Point transaction records cannot be deleted';
END //
DELIMITER ;

-- 同样为 t_wallet_transaction, t_billing_usage_ledger, t_billing_order 创建触发器
```

如果确实需要冲正/调整，必须通过**新增一条反向交易**来实现，而不是修改原记录。

### 2.3 余额校验摘要（Balance Digest）

账户表（积分账户、钱包账户）增加余额校验摘要，每次余额变动时同步更新：

```sql
ALTER TABLE t_point_account ADD COLUMN balance_digest VARCHAR(64) NOT NULL
    COMMENT '余额校验摘要=HMAC(secret, id+balance+frozen+totalEarned+totalConsumed+version)';

ALTER TABLE t_wallet_account ADD COLUMN balance_digest VARCHAR(64) NOT NULL
    COMMENT '余额校验摘要';
```

```java
public String computeBalanceDigest(PointAccount account) {
    String raw = account.getId()
        + "|" + account.getBalance()
        + "|" + account.getFrozen()
        + "|" + account.getTotalEarned()
        + "|" + account.getTotalConsumed()
        + "|" + account.getVersion();
    return HMAC_SHA256(BALANCE_SECRET, raw);
}
```

如果有人直接修改数据库中的余额字段，校验摘要将不匹配，系统在读取时即可检测到。

### 2.4 定时对账任务

```
┌──────────────────────────────────────────────────────┐
│           定时对账调度 (每天凌晨 3:00)                   │
│                                                      │
│  任务1: 哈希链完整性校验                                │
│    ├─ 遍历全部积分流水，逐条校验 chain_hash              │
│    ├─ 遍历全部钱包流水，逐条校验 chain_hash              │
│    └─ 发现断裂 → 立即告警 (短信+邮件+钉钉)              │
│                                                      │
│  任务2: 余额一致性校验                                  │
│    ├─ 积分: SUM(流水) vs 账户余额                       │
│    │   即 SUM(收入流水) - SUM(支出流水) = 当前余额        │
│    ├─ 钱包: SUM(充值) - SUM(消费) = 当前余额             │
│    └─ 不一致 → 立即告警 + 冻结相关账户                   │
│                                                      │
│  任务3: 余额摘要校验                                    │
│    ├─ 重新计算每个账户的 balance_digest                  │
│    ├─ 与数据库中的 digest 比对                           │
│    └─ 不一致 → 说明余额被直接篡改 → 告警                 │
│                                                      │
│  任务4: 订单金额校验                                    │
│    ├─ 订单实付 = 总金额 - 折扣 - 积分抵扣                │
│    └─ 不等式 → 告警                                    │
│                                                      │
│  结果: 写入 t_reconciliation_report                    │
│  告警: 调用通知服务 + 短信 + Webhook                    │
└──────────────────────────────────────────────────────┘
```

### 2.5 对账报告表

```
t_reconciliation_report (对账报告)
├── id                  : bigint, PK
├── tenant_id           : bigint
├── report_date         : date, NOT NULL                 -- 对账日期
├── check_type          : varchar(30), NOT NULL          -- HASH_CHAIN / BALANCE_SUM / BALANCE_DIGEST / ORDER_AMOUNT
├── target_table        : varchar(64)                    -- 校验的表名
├── total_records       : bigint                         -- 校验总记录数
├── pass_count          : bigint                         -- 通过数
├── fail_count          : bigint                         -- 失败数
├── status              : varchar(20)                    -- PASS / FAIL / ERROR
├── fail_details        : json                           -- 失败记录详情 (ID列表+错误信息)
├── executed_at         : datetime
├── duration_ms         : bigint                         -- 执行耗时
├── created_at          : datetime
└── updated_at          : datetime
```

---

## 三、身份认证与访问控制

### 3.1 管理后台认证升级

现有的 `admin-token` 单一 Token 方案安全性不足，需升级为完整的用户认证体系：

```
┌──────────────────────────────────────────────────────┐
│              管理后台认证体系                            │
│                                                      │
│  1. 管理员账号体系                                     │
│     ├─ 用户名 + 密码 (BCrypt加密)                      │
│     ├─ 登录时签发 JWT (短期有效，如30分钟)              │
│     ├─ Refresh Token (长期有效，如7天，存Redis)         │
│     └─ JWT过期后用 Refresh Token 换新JWT               │
│                                                      │
│  2. 多因素认证 (MFA)                                   │
│     ├─ 首次登录: 用户名+密码                            │
│     ├─ 敏感操作: 需二次验证                             │
│     │   ├─ 手机短信验证码 (腾讯云SMS)                   │
│     │   └─ TOTP时间令牌 (Google Authenticator)         │
│     └─ 需要MFA的操作:                                  │
│         ├─ 修改积分/余额 (调账)                        │
│         ├─ 确认大额订单付款                             │
│         ├─ 删除产品/套餐/规则                           │
│         ├─ 修改管理员权限                               │
│         └─ 导出客户数据                                │
│                                                      │
│  3. 登录安全                                           │
│     ├─ 密码错误5次锁定30分钟                           │
│     ├─ 异地登录短信告警                                │
│     ├─ 登录设备指纹记录                                │
│     └─ Session单点登录 (同一账号只能一处在线)            │
└──────────────────────────────────────────────────────┘
```

### 3.2 RBAC 权限模型

```
t_admin_user (管理员)
├── id, username, password_hash, real_name, phone, email
├── mfa_enabled, mfa_secret
├── status, last_login_at, last_login_ip
└── created_at, updated_at

t_admin_role (角色)
├── id, role_code, role_name, description
└── created_at

t_admin_user_role (用户-角色关联)
├── user_id, role_id

t_admin_permission (权限)
├── id, permission_code, permission_name, module, action
│   例: billing:order:create, billing:order:confirm_payment,
│       points:account:adjust, wallet:account:adjust
└── description

t_admin_role_permission (角色-权限关联)
├── role_id, permission_id
```

**预设角色示例：**

| 角色 | 权限范围 |
|------|---------|
| **超级管理员** | 全部权限 |
| **运营主管** | 产品/套餐/定价/折扣/赠送 管理；订单查看；报表查看；试用延长审批 |
| **销售员** | 代客下单；订单查看；客户订阅查看；试用延长申请（不能审批） |
| **财务** | 订单确认付款；调账操作（需MFA）；报表查看；对账报告 |
| **客服** | 客户订阅查看；试用管理；提醒记录查看 |
| **审计员** | 只读全部数据 + 审计日志 + 对账报告（不可执行任何写操作） |

### 3.3 操作级权限注解

```java
@PreAuthorize("hasPermission('billing:order:confirm_payment')")
@MfaRequired  // 自定义注解：需要二次验证
@PostMapping("/{id}/confirm-payment")
public ApiResponse<?> confirmPayment(@PathVariable Long id) { ... }
```

---

## 四、完整审计日志

### 4.1 操作审计表

**每一次写操作**都必须记录审计日志，包括操作前后的数据快照：

```
t_audit_log (审计日志)
├── id                  : bigint, PK
├── tenant_id           : bigint
├── trace_id            : varchar(64)                    -- 请求追踪ID
│
│   ── 操作人 ──
├── operator_type       : varchar(20)                    -- ADMIN / CUSTOMER / SYSTEM / API
├── operator_id         : varchar(64)                    -- 操作人ID
├── operator_name       : varchar(128)                   -- 操作人姓名
├── operator_ip         : varchar(45)                    -- 操作人IP
├── user_agent          : varchar(512)                   -- 浏览器/客户端标识
│
│   ── 操作内容 ──
├── module              : varchar(32)                    -- 模块：points/wallet/billing/system
├── action              : varchar(64)                    -- 动作：earn_points/deduct_balance/confirm_order
├── target_type         : varchar(64)                    -- 目标类型：PointAccount/WalletAccount/Order
├── target_id           : varchar(64)                    -- 目标ID
├── description         : varchar(512)                   -- 操作描述
│
│   ── 数据快照 ──
├── before_data         : json                           -- 操作前数据快照
├── after_data          : json                           -- 操作后数据快照
├── request_body        : text                           -- 请求体（脱敏后）
│
│   ── 结果 ──
├── status              : varchar(10)                    -- SUCCESS / FAIL
├── error_message       : varchar(512)
│
├── created_at          : datetime
└── (无 updated_at，审计日志不可修改)
  (Index: tenant_id + module + created_at DESC)
  (Index: operator_id + created_at DESC)
  (Index: target_type + target_id)
```

### 4.2 审计日志实现 — AOP 切面

```java
@Aspect
@Component
public class AuditAspect {

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) {
        // 1. 记录操作前数据快照 (beforeData)
        // 2. 执行操作
        // 3. 记录操作后数据快照 (afterData)
        // 4. 异步写入 t_audit_log
        // 5. 敏感操作同时写入独立审计数据库 (防止主库被攻破后日志也被删)
    }
}

// 注解使用
@Auditable(module = "wallet", action = "adjust_balance",
           description = "后台手动调账")
@MfaRequired
public WalletDTO.TransactionResult adminAdjust(...) { ... }
```

### 4.3 审计日志安全存储

```
┌─────────────────────────────────────────────┐
│           审计日志双写策略                      │
│                                             │
│  写操作发生                                   │
│    ├→ 写入主数据库 t_audit_log (实时)          │
│    ├→ 异步写入独立审计数据库 (隔离存储)          │
│    │   (独立MySQL实例，只有审计员有读权限)       │
│    └→ 定期归档到对象存储 (OSS/S3，不可修改)     │
│                                             │
│  审计日志表同样禁止 UPDATE/DELETE:              │
│    CREATE TRIGGER trg_audit_no_modify ...     │
│                                             │
│  日志保留策略:                                 │
│    主库: 保留90天                              │
│    审计库: 保留3年                             │
│    归档存储: 永久保留                           │
└─────────────────────────────────────────────┘
```

---

## 五、API 安全加固

### 5.1 现有签名机制加固

当前的 HMAC-SHA256 签名机制基础良好，增加以下加固措施：

```
请求安全校验链:

  ① HTTPS强制 (TLS 1.2+，禁用老版本协议)
       ↓
  ② IP白名单校验 (外部系统必须配置)
       ↓
  ③ 频率限流 (令牌桶算法，Redis实现)
     ├─ 全局: 10000 QPS
     ├─ 单系统: 按 rate_limit 配置
     └─ 单IP: 1000次/分钟
       ↓
  ④ 时间戳校验 (±5分钟窗口)
       ↓
  ⑤ Nonce防重放 (Redis缓存5分钟)
       ↓
  ⑥ HMAC-SHA256 签名校验
       ↓
  ⑦ 请求体 MD5 完整性校验
       ↓
  ⑧ 参数合法性校验 (类型/范围/格式)
       ↓
  ⑨ 业务逻辑校验 (余额充足/幂等键等)
```

### 5.2 关键接口额外防护

对于金额变动类接口，增加额外安全层：

```java
// 大额操作需要二次确认
@PostMapping("/api/v1/wallet/consume")
public ApiResponse<?> consume(HttpServletRequest request, @RequestBody ConsumeRequest body) {

    // 超过阈值的消费需要带安全令牌
    if (body.getEstimatedAmount() > LARGE_AMOUNT_THRESHOLD) {
        String securityToken = request.getHeader("X-Security-Token");
        verifySecurityToken(securityToken, body);
    }

    // 正常处理...
}
```

### 5.3 输入安全校验

```java
// 全局 XSS 过滤
@Component
public class XssFilter implements Filter {
    // 过滤所有请求参数中的 <script>、SQL关键字等
}

// 参数校验注解
public class EarnRequest {
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$")  // 只允许安全字符
    private String userId;

    @Positive
    @Max(value = 10000000)  // 单次最大积分上限
    private Long points;
}
```

---

## 六、数据库安全

### 6.1 最小权限原则

```sql
-- 应用程序使用的数据库用户：只有 SELECT/INSERT/UPDATE 权限
CREATE USER 'cobasesys_app'@'%' IDENTIFIED BY 'xxx';
GRANT SELECT, INSERT, UPDATE ON cobasesys.* TO 'cobasesys_app'@'%';
-- 注意：不授予 DELETE 权限（删除通过状态标记实现）
-- 注意：不授予 DROP/ALTER/TRUNCATE 权限

-- 流水表的用户：只有 SELECT/INSERT 权限（不可改、不可删）
CREATE USER 'cobasesys_ledger'@'%' IDENTIFIED BY 'xxx';
GRANT SELECT, INSERT ON cobasesys.t_point_transaction TO 'cobasesys_ledger'@'%';
GRANT SELECT, INSERT ON cobasesys.t_wallet_transaction TO 'cobasesys_ledger'@'%';
GRANT SELECT, INSERT ON cobasesys.t_billing_usage_ledger TO 'cobasesys_ledger'@'%';
GRANT SELECT, INSERT ON cobasesys.t_audit_log TO 'cobasesys_ledger'@'%';

-- 报表只读用户
CREATE USER 'cobasesys_readonly'@'%' IDENTIFIED BY 'xxx';
GRANT SELECT ON cobasesys.* TO 'cobasesys_readonly'@'%';

-- DBA操作审计：开启 MySQL general_log 或 audit_log 插件
```

### 6.2 敏感数据加密

```
┌──────────────────────────────────────────────┐
│              数据加密策略                       │
│                                              │
│  传输加密:                                    │
│    ├─ 应用↔数据库: 强制 SSL 连接               │
│    ├─ 应用↔Redis: 启用 TLS                   │
│    └─ 前端↔后端: HTTPS (TLS 1.2+)            │
│                                              │
│  存储加密:                                    │
│    ├─ MySQL: 透明数据加密 (TDE)               │
│    ├─ 备份文件: AES-256 加密                  │
│    └─ 密钥管理: 密钥与数据分离存储              │
│                                              │
│  字段级加密 (应用层):                          │
│    ├─ app_secret: AES加密存储                 │
│    ├─ 客户手机号: 脱敏存储 (138****1234)       │
│    ├─ 密码: BCrypt单向哈希                     │
│    └─ 支付单号: 仅在需要时解密                  │
└──────────────────────────────────────────────┘
```

### 6.3 SQL注入防护

```
防护层次:
  1. Spring Data JPA — 参数化查询（天然防SQL注入）
  2. @Query 中使用 :param 命名参数（禁止字符串拼接）
  3. 全局 SQL 关键字过滤（XSS Filter 同时处理）
  4. 数据库用户权限限制（即使注入成功也无法执行DDL）
  5. WAF 层面拦截常见 SQL 注入特征
```

---

## 七、实时监控与异常告警

### 7.1 监控指标

```
┌──────────────────────────────────────────────────────┐
│                 安全监控仪表盘                          │
│                                                      │
│  实时指标:                                            │
│  ├─ API 请求量/错误率/延迟 (Prometheus + Grafana)      │
│  ├─ 签名校验失败次数 (按系统/IP分组)                    │
│  ├─ 管理员登录失败次数                                 │
│  ├─ 积分/余额变动总额 (与历史均值比较)                  │
│  ├─ 大额交易实时告警 (超过阈值)                        │
│  └─ 异常IP访问 (未知IP/高频IP)                        │
│                                                      │
│  告警规则:                                            │
│  ├─ 签名失败 > 100次/分钟 → 短信告警 + 临时封禁IP       │
│  ├─ 管理员登录失败 > 5次 → 锁定账号 + 短信通知          │
│  ├─ 单账户积分变动 > 10万/小时 → 告警审核               │
│  ├─ 单账户余额变动 > 5万元/小时 → 告警审核              │
│  ├─ 对账任务发现异常 → 立即告警 + 冻结相关操作           │
│  ├─ 数据库慢查询 > 5秒 → 运维告警                      │
│  └─ 数据库连接数 > 80% → 运维告警                      │
└──────────────────────────────────────────────────────┘
```

### 7.2 异常交易自动拦截

```java
@Component
public class AnomalyDetector {

    // 单账户单小时积分变动上限
    private static final long POINT_HOURLY_LIMIT = 100000;
    // 单账户单小时余额变动上限（分）
    private static final long WALLET_HOURLY_LIMIT = 5000000; // 5万元

    public void checkBeforePointChange(String userId, long points) {
        String key = "anomaly:points:" + userId + ":" + currentHour();
        Long total = redisTemplate.opsForValue().increment(key, Math.abs(points));
        redisTemplate.expire(key, Duration.ofHours(2));

        if (total > POINT_HOURLY_LIMIT) {
            // 触发告警，但不阻断（避免误判影响业务）
            alertService.sendAlert("POINT_ANOMALY",
                "用户 " + userId + " 小时内积分变动达 " + total);
        }
    }
}
```

---

## 八、数据备份与灾难恢复

```
┌──────────────────────────────────────────────────────┐
│                 备份与恢复策略                          │
│                                                      │
│  MySQL 备份:                                         │
│  ├─ 全量备份: 每天凌晨 2:00 (mysqldump + 压缩加密)    │
│  ├─ 增量备份: Binlog 实时同步到备份服务器               │
│  ├─ 异地备份: 每日全量同步到异地机房/云存储             │
│  └─ 保留策略: 全量30天 + 月度归档12个月               │
│                                                      │
│  Redis 备份:                                         │
│  ├─ RDB快照: 每小时                                  │
│  └─ AOF持久化: 每秒写入                              │
│                                                      │
│  恢复演练:                                            │
│  ├─ 每季度执行一次恢复演练                             │
│  ├─ 验证备份数据完整性                                │
│  └─ 记录恢复 RTO/RPO 指标                            │
│                                                      │
│  目标:                                               │
│  ├─ RPO (最大数据丢失): < 1分钟 (Binlog实时同步)      │
│  └─ RTO (最大恢复时间): < 30分钟                      │
└──────────────────────────────────────────────────────┘
```

---

## 九、安全相关的新增数据表汇总

| 表名 | 用途 |
|------|------|
| `t_admin_user` | 管理员账号 |
| `t_admin_role` | 角色定义 |
| `t_admin_user_role` | 用户-角色关联 |
| `t_admin_permission` | 权限定义 |
| `t_admin_role_permission` | 角色-权限关联 |
| `t_admin_login_log` | 登录日志（IP/设备/结果） |
| `t_audit_log` | 全操作审计日志 |
| `t_reconciliation_report` | 对账报告 |
| 流水表增加字段 | `data_hash`, `prev_hash`, `chain_hash` |
| 账户表增加字段 | `balance_digest` |

---

## 十、安全实施优先级

| 优先级 | 措施 | 实施阶段 | 防护的威胁 |
|--------|------|---------|-----------|
| **P0** | 哈希链 + 余额摘要 | 一期 | 账务数据篡改 |
| **P0** | 流水表禁止 UPDATE/DELETE 触发器 | 一期 | 账务数据篡改 |
| **P0** | 定时对账任务 | 一期 | 账务数据篡改 |
| **P0** | 管理员用户体系 + RBAC | 一期 | 管理员账号劫持 |
| **P0** | 完整审计日志 | 一期 | 内部人员篡改 |
| **P1** | MFA多因素认证 | 一期 | 管理员账号劫持 |
| **P1** | 数据库最小权限 | 一期 | 数据库入侵 |
| **P1** | 异常交易监控告警 | 一期 | API伪造调用 |
| **P1** | 数据库传输加密 (SSL) | 一期 | 数据泄露 |
| **P2** | 审计日志独立存储 | 二期 | 日志被篡改 |
| **P2** | 字段级加密 | 二期 | 数据泄露 |
| **P2** | 异地灾备 | 二期 | 灾难恢复 |
| **P3** | WAF/DDoS防护 | 部署时 | DDoS攻击 |

---

## 十一、确认清单

- [ ] **哈希链方案**：流水表通过 SHA256 哈希链串联，是否可接受？是否需要更强的校验（如独立校验库双写）？
- [ ] **只追加账本**：通过数据库触发器禁止 UPDATE/DELETE 流水记录，冲正通过反向交易实现，是否可接受？
- [ ] **对账频率**：每天凌晨3:00全量对账，是否需要更高频率（如每小时增量对账）？
- [ ] **管理员认证**：升级为用户名+密码+JWT+MFA，是否可接受？还是有现有的统一认证系统要对接？
- [ ] **RBAC角色**：超级管理员/运营主管/销售员/财务/客服/审计员，角色划分是否合理？
- [ ] **MFA方式**：短信验证码 + TOTP (Google Authenticator)，是否需要其他方式（如企业微信/钉钉审批）？
- [ ] **审计日志保留**：主库90天、审计库3年、归档永久，是否符合贵司合规要求？
- [ ] **备份策略**：每日全量+实时增量+异地备份，RPO<1分钟 / RTO<30分钟，是否可接受？
- [ ] **告警渠道**：安全告警通过短信+邮件+钉钉发送，是否需要其他渠道？
- [ ] **独立审计库**：是否需要部署独立的审计数据库实例（增加成本但提高安全性）？
