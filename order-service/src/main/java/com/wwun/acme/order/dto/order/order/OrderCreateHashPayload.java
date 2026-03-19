package com.wwun.acme.order.dto.order.order;

import java.util.List;
import java.util.UUID;

import com.wwun.acme.order.dto.order.orderItem.OrderItemHashPayload;

public record OrderCreateHashPayload(UUID userId, List<OrderItemHashPayload> items){}
