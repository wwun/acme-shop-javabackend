package com.wwun.acme.order.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wwun.acme.order.entity.Order;
import java.util.List;
import java.util.Optional;


public interface OrderRepository extends JpaRepository<Order, UUID>{
    List<Order> findAllByUserId(UUID userId);
    Optional<Order> findByUserIdAndIdempotencyKey(UUID userId, UUID idempotencyKey);
}
