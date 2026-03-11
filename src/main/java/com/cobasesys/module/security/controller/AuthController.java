package com.cobasesys.module.security.controller;

import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.module.security.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "安全-管理员认证")
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminAuthService authService;

    @PostMapping("/login")
    @Operation(summary = "管理员登录")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For") != null ?
                request.getHeader("X-Forwarded-For").split(",")[0].trim() : request.getRemoteAddr();
        return ApiResponse.ok(authService.login(req.getUsername(), req.getPassword(), ip,
                request.getHeader("User-Agent")));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新Token")
    public ApiResponse<Map<String, Object>> refresh(@RequestParam String refreshToken) {
        return ApiResponse.ok(authService.refreshToken(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public ApiResponse<Void> logout(@RequestParam Long userId) {
        authService.logout(userId);
        return ApiResponse.ok();
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改密码")
    public ApiResponse<Void> changePassword(@RequestParam Long userId,
                                              @RequestParam String oldPassword,
                                              @RequestParam String newPassword) {
        authService.changePassword(userId, oldPassword, newPassword);
        return ApiResponse.ok();
    }

    @PostMapping("/mfa/send")
    @Operation(summary = "发送MFA验证码")
    public ApiResponse<String> sendMfaCode(@RequestParam Long userId) {
        String code = authService.generateMfaCode(userId);
        return ApiResponse.ok("验证码已发送 (开发环境: " + code + ")");
    }

    @PostMapping("/mfa/verify")
    @Operation(summary = "验证MFA验证码")
    public ApiResponse<Boolean> verifyMfaCode(@RequestParam Long userId, @RequestParam String code) {
        return ApiResponse.ok(authService.verifyMfaCode(userId, code));
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
