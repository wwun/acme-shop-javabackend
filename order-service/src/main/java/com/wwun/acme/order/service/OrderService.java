package com.wwun.acme.order.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wwun.acme.order.dto.order.order.OrderCreateRequestDTO;
import com.wwun.acme.order.dto.order.order.OrderUpdateRequestDTO;
import com.wwun.acme.order.entity.Order;

public interface OrderService {

    Order save(OrderCreateRequestDTO orderCreateRequestDTO);
    Optional<Order> findById(UUID id);
    List<Order> findAll();
    List<Order> findAllByUserId(UUID userId);
    Optional<Order> update(UUID id, OrderUpdateRequestDTO orderUpdateRequestDTO);
    void delete(UUID id);

}