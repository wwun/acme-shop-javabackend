package com.wwun.acme.order.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wwun.acme.order.entity.Order;
import java.util.List;


public interface OrderRepository extends JpaRepository<Order, UUID>{
    List<Order> findAllByUserId(UUID userId);
}
