package com.wwun.acme.order.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wwun.acme.order.dto.order.orderItem.OrderItemCreateRequestDTO;
import com.wwun.acme.order.dto.order.orderItem.OrderItemResponseDTO;
import com.wwun.acme.order.dto.order.orderItem.OrderItemUpdateRequestDTO;
import com.wwun.acme.order.entity.OrderItem;

public interface OrderItemService {

    OrderItem save(UUID orderId, OrderItemCreateRequestDTO orderItemCreateRequestDTO);
    Optional<OrderItem> findById(UUID id);
    List<OrderItem> findAll();
    //List<OrderItem> findAllByOrderId(UUID orderId);
    List<OrderItemResponseDTO> findAllByOrderId(UUID orderId);
    Optional<OrderItem> update(UUID id, OrderItemUpdateRequestDTO orderItemUpdateRequestDTO);
    void delete(UUID id);

}
