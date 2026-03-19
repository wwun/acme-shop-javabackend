package com.wwun.acme.order.dto.order.orderItem;

import java.util.UUID;

public record OrderItemHashPayload(UUID productId, Integer quantity){}
