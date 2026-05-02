package com.LogicProjector.recharge;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RechargeOrderRepository extends JpaRepository<RechargeOrder, Long> {

    @Query("select rechargeOrder from RechargeOrder rechargeOrder where rechargeOrder.user.id = :userId order by rechargeOrder.createdAt desc")
    List<RechargeOrder> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rechargeOrder from RechargeOrder rechargeOrder where rechargeOrder.id = :id and rechargeOrder.user.id = :userId")
    Optional<RechargeOrder> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}
