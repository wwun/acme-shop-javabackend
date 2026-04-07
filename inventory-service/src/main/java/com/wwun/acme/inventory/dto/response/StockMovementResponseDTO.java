package com.wwun.acme.inventory.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.wwun.acme.inventory.enums.StockMovementType;


public record StockMovementResponseDTO(
    UUID id,
    UUID productId,
    UUID orderId,
    Instant createdAt,
    Integer quantity,
    StockMovementType type){

}