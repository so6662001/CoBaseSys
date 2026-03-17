package com.cobasesys.module.wallet.repository;

import com.cobasesys.module.wallet.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Optional<WalletTransaction> findByIdempotentKey(String idempotentKey);

    Page<WalletTransaction> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    Page<WalletTransaction> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);
}
