package com.anuhcollections.backend.orders;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerOrderRepository
        extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<CustomerOrder> findByIdAndUser_Id(Long orderId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o
            from CustomerOrder o
            where o.id = :orderId
              and o.user.id = :userId
            """)
    Optional<CustomerOrder> findOwnedOrderForUpdate(
            @Param("orderId") Long orderId,
            @Param("userId") Long userId);
}