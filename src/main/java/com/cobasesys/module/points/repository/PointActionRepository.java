package com.cobasesys.module.points.repository;

import com.cobasesys.module.points.entity.PointAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointActionRepository extends JpaRepository<PointAction, Long> {

    Optional<PointAction> findBySystemIdAndActionCode(Long systemId, String actionCode);

    Page<PointAction> findByTenantId(Long tenantId, Pageable pageable);

    Page<PointAction> findByTenantIdAndSystemId(Long tenantId, Long systemId, Pageable pageable);
}
