package com.wwun.acme.inventory.dto.response;

import java.time.Instant;
import java.util.UUID;

public record InventoryResponseDTO(
    UUID id,
    UUID productId,
    Integer quantityAvailable,
    Integer quantityReserved,
    Instant updatedAt){

}