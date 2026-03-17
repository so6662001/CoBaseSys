package com.cobasesys.module.security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_admin_login_log")
public class AdminLoginLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "login_ip", length = 45)
    private String loginIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "fail_reason", length = 256)
    private String failReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
