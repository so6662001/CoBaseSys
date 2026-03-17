package com.cobasesys.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Common
    SYSTEM_ERROR(10000, "系统内部错误"),
    PARAM_INVALID(10001, "参数校验失败"),
    UNAUTHORIZED(10002, "认证失败"),
    FORBIDDEN(10003, "权限不足"),
    NOT_FOUND(10004, "资源不存在"),
    DUPLICATE_REQUEST(10005, "重复请求"),
    RATE_LIMITED(10006, "请求过于频繁"),
    TENANT_NOT_FOUND(10007, "租户不存在"),

    // External System
    SYSTEM_DISABLED(20001, "外部系统已禁用"),
    SYSTEM_NOT_FOUND(20002, "外部系统不存在"),
    SIGNATURE_INVALID(20003, "签名校验失败"),
    TIMESTAMP_EXPIRED(20004, "请求时间戳已过期"),
    IP_NOT_ALLOWED(20005, "IP地址不在白名单中"),

    // Points
    POINT_ACTION_NOT_FOUND(30001, "积分动作不存在"),
    POINT_RULE_NOT_FOUND(30002, "积分规则不存在"),
    POINT_ACCOUNT_NOT_FOUND(30003, "积分账户不存在"),
    POINT_BALANCE_INSUFFICIENT(30004, "积分余额不足"),
    POINT_DAILY_LIMIT_EXCEEDED(30005, "已达每日积分上限"),
    POINT_MONTHLY_LIMIT_EXCEEDED(30006, "已达每月积分上限"),
    POINT_ACTION_DISABLED(30007, "积分动作已禁用"),

    // Wallet
    WALLET_ACCOUNT_NOT_FOUND(40001, "钱包账户不存在"),
    WALLET_BALANCE_INSUFFICIENT(40002, "余额不足"),
    CONSUME_ACTION_NOT_FOUND(40003, "消费动作不存在"),
    CONSUME_RULE_NOT_FOUND(40004, "消费规则不存在"),
    RECHARGE_ORDER_NOT_FOUND(40005, "充值订单不存在"),
    RECHARGE_ORDER_PAID(40006, "充值订单已支付"),
    CONSUME_ACTION_DISABLED(40007, "消费动作已禁用"),

    // Member
    MEMBER_LEVEL_NOT_FOUND(50001, "会员等级不存在"),

    // Webhook
    WEBHOOK_CONFIG_NOT_FOUND(60001, "Webhook配置不存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
