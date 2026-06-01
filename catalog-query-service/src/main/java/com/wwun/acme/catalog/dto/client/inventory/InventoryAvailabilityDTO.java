package com.wwun.acme.catalog.dto.client.inventory;

import java.util.UUID;

public record InventoryAvailabilityDTO(
    UUID productId,
    Integer quantityAvailable,
    Integer quantityReserved) {
}