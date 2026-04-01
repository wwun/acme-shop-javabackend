package com.wwun.acme.order.dto.order.orderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponseDTO(UUID id, UUID orderId, UUID productId, String productName, Integer quantity, BigDecimal priceAtPurchase){

}
