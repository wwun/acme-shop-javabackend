package com.wwun.acme.order.dto.order.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.wwun.acme.order.entity.OrderItem;

public record OrderResponseDTO(UUID id, UUID userId, Instant orderDate, BigDecimal total, List<OrderItem> items){

}
