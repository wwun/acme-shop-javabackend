package com.wwun.acme.catalog.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record InventoryBatchRequestDTO(
    @NotEmpty(message = "productIds cannot be empty")
    List<@NotNull(message = "productId cannot be null") UUID> productIds) {
}
