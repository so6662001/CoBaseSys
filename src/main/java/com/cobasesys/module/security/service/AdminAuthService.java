package com.cobasesys.module.security.service;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.module.security.entity.*;
import com.cobasesys.module.security.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminUserRepository userRepository;
    private final AdminUserRoleRepository userRoleRepository;
    private final AdminRoleRepository roleRepository;
    private final AdminRolePermissionRepository rolePermissionRepository;
    private final AdminPermissionRepository permissionRepository;
    private final AdminLoginLogRepository loginLogRepository;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final int MAX_FAIL_COUNT = 5;
    private static final int LOCK_MINUTES = 30;

    @Transactional
    public Map<String, Object> login(String username, String password, String ip, String userAgent) {
        AdminUser user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            saveLoginLog(null, username, ip, userAgent, "FAIL", "用户不存在");
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            saveLoginLog(user.getId(), username, ip, userAgent, "FAIL", "账号已锁定");
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号已锁定，请" + LOCK_MINUTES + "分钟后重试");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.setLoginFailCount(user.getLoginFailCount() + 1);
            if (user.getLoginFailCount() >= MAX_FAIL_COUNT) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                user.setLoginFailCount(0);
                log.warn("Admin user {} locked after {} failed attempts", username, MAX_FAIL_COUNT);
            }
            userRepository.save(user);
            saveLoginLog(user.getId(), username, ip, userAgent, "FAIL", "密码错误");
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            saveLoginLog(user.getId(), username, ip, userAgent, "FAIL", "账号已禁用");
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号已禁用");
        }

        user.setLoginFailCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ip);
        userRepository.save(user);

        List<String> permissions = getUserPermissions(user.getId());
        String accessToken = jwtService.generateToken(user.getId(), user.getUsername(), permissions);
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername());

        redisTemplate.opsForValue().set("admin:refresh:" + user.getId(), refreshToken, Duration.ofDays(7));

        saveLoginLog(user.getId(), username, ip, userAgent, "SUCCESS", null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("mfaEnabled", user.getMfaEnabled());
        result.put("permissions", permissions);
        return result;
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        if (!jwtService.isValid(refreshToken)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Refresh token 无效");
        }
        Long userId = jwtService.getUserId(refreshToken);
        String stored = redisTemplate.opsForValue().get("admin:refresh:" + userId);
        if (!refreshToken.equals(stored)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Refresh token 已失效");
        }

        AdminUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        List<String> permissions = getUserPermissions(userId);
        String newAccessToken = jwtService.generateToken(userId, user.getUsername(), permissions);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("userId", userId);
        result.put("username", user.getUsername());
        return result;
    }

    public void logout(Long userId) {
        redisTemplate.delete("admin:refresh:" + userId);
    }

    public boolean verifyMfaCode(Long userId, String code) {
        String key = "admin:mfa:" + userId;
        String stored = redisTemplate.opsForValue().get(key);
        if (code.equals(stored)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    public String generateMfaCode(Long userId) {
        String code = String.format("%06d", new Random().nextInt(1000000));
        redisTemplate.opsForValue().set("admin:mfa:" + userId, code, Duration.ofMinutes(5));
        return code;
    }

    public List<String> getUserPermissions(Long userId) {
        List<AdminUserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) return List.of();

        List<Long> roleIds = userRoles.stream().map(AdminUserRole::getRoleId).toList();

        boolean isSuperAdmin = roleIds.stream().anyMatch(rid -> {
            var role = roleRepository.findById(rid).orElse(null);
            return role != null && "SUPER_ADMIN".equals(role.getRoleCode());
        });
        if (isSuperAdmin) {
            return permissionRepository.findAll().stream()
                    .map(AdminPermission::getPermissionCode).toList();
        }

        List<AdminRolePermission> rps = rolePermissionRepository.findByRoleIdIn(roleIds);
        Set<Long> permIds = rps.stream().map(AdminRolePermission::getPermissionId).collect(Collectors.toSet());
        return permissionRepository.findAllById(permIds).stream()
                .map(AdminPermission::getPermissionCode).toList();
    }

    private void saveLoginLog(Long userId, String username, String ip, String userAgent,
                                String status, String failReason) {
        AdminLoginLog log = new AdminLoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setLoginIp(ip);
        log.setUserAgent(userAgent);
        log.setStatus(status);
        log.setFailReason(failReason);
        loginLogRepository.save(log);
    }
}
