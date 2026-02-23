package com.cobasesys.module.member.repository;

import com.cobasesys.module.member.entity.MemberLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberLevelRepository extends JpaRepository<MemberLevel, Long> {

    List<MemberLevel> findByTenantIdAndStatusOrderByLevelRankAsc(Long tenantId, Integer status);

    Page<MemberLevel> findByTenantIdOrderByLevelRankAsc(Long tenantId, Pageable pageable);
}
