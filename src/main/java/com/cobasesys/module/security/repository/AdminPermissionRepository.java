package com.cobasesys.module.security.repository;

import com.cobasesys.module.security.entity.AdminPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdminPermissionRepository extends JpaRepository<AdminPermission, Long> {
    List<AdminPermission> findByModule(String module);
}
