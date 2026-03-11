package com.cobasesys.module.security.repository;

import com.cobasesys.module.security.entity.AdminRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdminRolePermissionRepository extends JpaRepository<AdminRolePermission, Long> {
    List<AdminRolePermission> findByRoleId(Long roleId);
    List<AdminRolePermission> findByRoleIdIn(List<Long> roleIds);
    void deleteByRoleId(Long roleId);
}
