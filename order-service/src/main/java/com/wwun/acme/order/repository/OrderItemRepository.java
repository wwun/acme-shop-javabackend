package com.wwun.acme.order.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wwun.acme.order.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID>{

    List<OrderItem> findAllByOrderId(UUID orderId);

}
