package com.cobasesys.module.security.entity;

import com.cobasesys.common.model.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_admin_user")
public class AdminUser extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 256)
    private String passwordHash;

    @Column(name = "real_name", length = 64)
    private String realName;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "mfa_enabled", nullable = false)
    private Integer mfaEnabled = 0;

    @JsonIgnore
    @Column(name = "mfa_secret", length = 128)
    private String mfaSecret;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "login_fail_count", nullable = false)
    private Integer loginFailCount = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;
}
