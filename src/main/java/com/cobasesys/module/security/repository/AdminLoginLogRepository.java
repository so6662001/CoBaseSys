package com.cobasesys.module.security.repository;

import com.cobasesys.module.security.entity.AdminLoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminLoginLogRepository extends JpaRepository<AdminLoginLog, Long> {
    Page<AdminLoginLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<AdminLoginLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
