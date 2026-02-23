package com.cobasesys.module.system.repository;

import com.cobasesys.module.system.entity.ExternalSystem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExternalSystemRepository extends JpaRepository<ExternalSystem, Long> {

    Optional<ExternalSystem> findByAppKey(String appKey);

    Optional<ExternalSystem> findByTenantIdAndSystemCode(Long tenantId, String systemCode);

    Page<ExternalSystem> findByTenantId(Long tenantId, Pageable pageable);

    List<ExternalSystem> findByTenantIdAndStatus(Long tenantId, Integer status);

    boolean existsByTenantIdAndSystemCode(Long tenantId, String systemCode);
}
