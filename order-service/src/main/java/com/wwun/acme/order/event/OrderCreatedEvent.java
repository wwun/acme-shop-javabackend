package com.wwun.acme.order.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(UUID orderId,
    UUID userId,
    BigDecimal total,
    Instant orderDate,
    List<OrderItemEvent> items){}
