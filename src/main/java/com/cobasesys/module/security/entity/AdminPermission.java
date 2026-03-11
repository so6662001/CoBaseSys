package com.cobasesys.module.security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_admin_permission")
public class AdminPermission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "permission_code", nullable = false, unique = true, length = 128)
    private String permissionCode;

    @Column(name = "permission_name", nullable = false, length = 128)
    private String permissionName;

    @Column(name = "module", length = 32)
    private String module;

    @Column(name = "action", length = 64)
    private String action;

    @Column(name = "description", length = 256)
    private String description;
}
