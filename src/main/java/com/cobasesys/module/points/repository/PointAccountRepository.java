package com.cobasesys.module.points.repository;

import com.cobasesys.module.points.entity.PointAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {

    Optional<PointAccount> findByTenantIdAndUserId(Long tenantId, String userId);

    Page<PointAccount> findByTenantId(Long tenantId, Pageable pageable);

    @Modifying
    @Query("UPDATE PointAccount a SET a.balance = a.balance + :points, a.totalEarned = a.totalEarned + :points, " +
            "a.version = a.version + 1 WHERE a.id = :id AND a.version = :version")
    int earnPoints(@Param("id") Long id, @Param("points") long points, @Param("version") long version);

    @Modifying
    @Query("UPDATE PointAccount a SET a.balance = a.balance - :points, a.totalConsumed = a.totalConsumed + :points, " +
            "a.version = a.version + 1 WHERE a.id = :id AND a.version = :version AND a.balance >= :points")
    int deductPoints(@Param("id") Long id, @Param("points") long points, @Param("version") long version);

    @Modifying
    @Query("UPDATE PointAccount a SET a.balance = a.balance - :points, a.frozen = a.frozen + :points, " +
            "a.version = a.version + 1 WHERE a.id = :id AND a.version = :version AND a.balance >= :points")
    int freezePoints(@Param("id") Long id, @Param("points") long points, @Param("version") long version);

    @Modifying
    @Query("UPDATE PointAccount a SET a.frozen = a.frozen - :points, a.balance = a.balance + :points, " +
            "a.version = a.version + 1 WHERE a.id = :id AND a.version = :version AND a.frozen >= :points")
    int unfreezePoints(@Param("id") Long id, @Param("points") long points, @Param("version") long version);
}
