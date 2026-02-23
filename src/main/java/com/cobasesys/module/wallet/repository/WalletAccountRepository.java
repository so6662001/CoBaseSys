package com.cobasesys.module.wallet.repository;

import com.cobasesys.module.wallet.entity.WalletAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, Long> {

    Optional<WalletAccount> findByTenantIdAndUserId(Long tenantId, String userId);

    Page<WalletAccount> findByTenantId(Long tenantId, Pageable pageable);

    @Modifying
    @Query("UPDATE WalletAccount a SET a.balance = a.balance + :amount, a.totalRecharged = a.totalRecharged + :amount, " +
            "a.version = a.version + 1 WHERE a.id = :id AND a.version = :version")
    int recharge(@Param("id") Long id, @Param("amount") long amount, @Param("version") long version);

    @Modifying
    @Query("UPDATE WalletAccount a SET a.balance = a.balance - :amount, a.totalConsumed = a.totalConsumed + :amount, " +
            "a.version = a.version + 1 WHERE a.id = :id AND a.version = :version AND a.balance >= :amount")
    int consume(@Param("id") Long id, @Param("amount") long amount, @Param("version") long version);

    @Modifying
    @Query("UPDATE WalletAccount a SET a.balance = a.balance - :amount, a.frozen = a.frozen + :amount, " +
            "a.version = a.version + 1 WHERE a.id = :id AND a.version = :version AND a.balance >= :amount")
    int freeze(@Param("id") Long id, @Param("amount") long amount, @Param("version") long version);

    @Modifying
    @Query("UPDATE WalletAccount a SET a.frozen = a.frozen - :amount, a.balance = a.balance + :amount, " +
            "a.version = a.version + 1 WHERE a.id = :id AND a.version = :version AND a.frozen >= :amount")
    int unfreeze(@Param("id") Long id, @Param("amount") long amount, @Param("version") long version);
}
