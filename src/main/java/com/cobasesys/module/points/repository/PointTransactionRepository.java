package com.cobasesys.module.points.repository;

import com.cobasesys.module.points.entity.PointTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    Optional<PointTransaction> findByIdempotentKey(String idempotentKey);

    Page<PointTransaction> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    Page<PointTransaction> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    long countByActionIdAndAccountId(Long actionId, Long accountId);
}
