package com.cobasesys.module.wallet.repository;

import com.cobasesys.module.wallet.entity.ConsumeAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsumeActionRepository extends JpaRepository<ConsumeAction, Long> {

    Optional<ConsumeAction> findBySystemIdAndActionCode(Long systemId, String actionCode);

    Page<ConsumeAction> findByTenantId(Long tenantId, Pageable pageable);

    Page<ConsumeAction> findByTenantIdAndSystemId(Long tenantId, Long systemId, Pageable pageable);
}
