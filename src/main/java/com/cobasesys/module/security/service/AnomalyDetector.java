package com.cobasesys.module.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnomalyDetector {

    private final StringRedisTemplate redisTemplate;

    private static final long POINT_HOURLY_LIMIT = 100000;
    private static final long WALLET_HOURLY_LIMIT = 5000000;
    private static final long API_FAIL_MINUTE_LIMIT = 100;

    public void checkPointChange(String userId, long points) {
        String key = "anomaly:points:" + userId + ":" + currentHour();
        Long total = redisTemplate.opsForValue().increment(key, Math.abs(points));
        redisTemplate.expire(key, Duration.ofHours(2));
        if (total != null && total > POINT_HOURLY_LIMIT) {
            log.warn("ANOMALY: User {} point changes reached {} in current hour (limit: {})",
                    userId, total, POINT_HOURLY_LIMIT);
        }
    }

    public void checkWalletChange(String userId, long amount) {
        String key = "anomaly:wallet:" + userId + ":" + currentHour();
        Long total = redisTemplate.opsForValue().increment(key, Math.abs(amount));
        redisTemplate.expire(key, Duration.ofHours(2));
        if (total != null && total > WALLET_HOURLY_LIMIT) {
            log.warn("ANOMALY: User {} wallet changes reached {} cents in current hour (limit: {})",
                    userId, total, WALLET_HOURLY_LIMIT);
        }
    }

    public void checkApiSignatureFailure(String appKey, String ip) {
        String key = "anomaly:api_fail:" + ip + ":" + currentMinute();
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofMinutes(2));
        if (count != null && count > API_FAIL_MINUTE_LIMIT) {
            log.warn("ANOMALY: IP {} has {} API signature failures in current minute (limit: {})",
                    ip, count, API_FAIL_MINUTE_LIMIT);
        }
    }

    public void checkAdminLoginFailure(String username, String ip) {
        String key = "anomaly:login_fail:" + username + ":" + currentHour();
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofHours(2));
        if (count != null && count > 10) {
            log.warn("ANOMALY: Admin user {} has {} login failures from IP {} in current hour",
                    username, count, ip);
        }
    }

    private String currentHour() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
    }

    private String currentMinute() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    }
}
