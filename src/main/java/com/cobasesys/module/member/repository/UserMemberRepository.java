package com.cobasesys.module.member.repository;

import com.cobasesys.module.member.entity.UserMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserMemberRepository extends JpaRepository<UserMember, Long> {

    Optional<UserMember> findByTenantIdAndUserId(Long tenantId, String userId);

    Page<UserMember> findByTenantId(Long tenantId, Pageable pageable);
}
