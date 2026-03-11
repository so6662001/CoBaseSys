package com.cobasesys.module.security.controller;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.common.exception.ErrorCode;
import com.cobasesys.common.model.ApiResponse;
import com.cobasesys.common.model.PageResult;
import com.cobasesys.module.security.entity.*;
import com.cobasesys.module.security.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "安全-管理员用户管理")
@RestController
@RequestMapping("/admin/security")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserRepository userRepository;
    private final AdminRoleRepository roleRepository;
    private final AdminUserRoleRepository userRoleRepository;
    private final AdminPermissionRepository permissionRepository;
    private final AdminRolePermissionRepository rolePermissionRepository;
    private final AdminLoginLogRepository loginLogRepository;
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping("/users")
    @Operation(summary = "管理员列表")
    public ApiResponse<PageResult<?>> listUsers(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(PageResult.from(userRepository.findAllByOrderByIdAsc(PageRequest.of(page - 1, pageSize))));
    }

    @PostMapping("/users")
    @Operation(summary = "创建管理员")
    @Transactional
    public ApiResponse<?> createUser(@RequestBody CreateUserReq req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "用户名已存在");
        }
        AdminUser user = new AdminUser();
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        userRepository.save(user);

        if (req.getRoleIds() != null) {
            for (Long roleId : req.getRoleIds()) {
                AdminUserRole ur = new AdminUserRole();
                ur.setUserId(user.getId()); ur.setRoleId(roleId);
                userRoleRepository.save(ur);
            }
        }
        return ApiResponse.ok(user);
    }

    @PostMapping("/users/{id}/reset-password")
    @Operation(summary = "重置密码")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        AdminUser user = userRepository.findById(id).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setLoginFailCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        return ApiResponse.ok();
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "删除管理员")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id); return ApiResponse.ok();
    }

    @GetMapping("/roles")
    @Operation(summary = "角色列表")
    public ApiResponse<List<AdminRole>> listRoles() {
        return ApiResponse.ok(roleRepository.findAll());
    }

    @GetMapping("/permissions")
    @Operation(summary = "权限列表")
    public ApiResponse<List<AdminPermission>> listPermissions() {
        return ApiResponse.ok(permissionRepository.findAll());
    }

    @GetMapping("/login-logs")
    @Operation(summary = "登录日志")
    public ApiResponse<PageResult<AdminLoginLog>> loginLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        var p = userId != null
                ? loginLogRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page - 1, pageSize))
                : loginLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page - 1, pageSize));
        return ApiResponse.ok(PageResult.from(p));
    }

    @Data
    public static class CreateUserReq {
        private String username;
        private String password;
        private String realName;
        private String phone;
        private String email;
        private List<Long> roleIds;
    }
}
