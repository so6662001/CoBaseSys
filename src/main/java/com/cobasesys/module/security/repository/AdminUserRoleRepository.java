package com.cobasesys.module.security.repository;

import com.cobasesys.module.security.entity.AdminUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdminUserRoleRepository extends JpaRepository<AdminUserRole, Long> {
    List<AdminUserRole> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
