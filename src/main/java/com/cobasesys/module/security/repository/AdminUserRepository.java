package com.cobasesys.module.security.repository;

import com.cobasesys.module.security.entity.AdminUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUsername(String username);
    boolean existsByUsername(String username);
    Page<AdminUser> findAllByOrderByIdAsc(Pageable pageable);
}
