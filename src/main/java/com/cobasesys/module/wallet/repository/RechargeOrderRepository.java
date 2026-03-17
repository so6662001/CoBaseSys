package com.cobasesys.module.wallet.repository;

import com.cobasesys.module.wallet.entity.RechargeOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RechargeOrderRepository extends JpaRepository<RechargeOrder, Long> {

    Optional<RechargeOrder> findByOrderNo(String orderNo);

    Page<RechargeOrder> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Page<RechargeOrder> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);
}
