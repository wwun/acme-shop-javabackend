package com.wwun.acme.inventory.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record InventoryCreateRequestDTO(
    @NotNull
    UUID productId,

    @NotNull
    @PositiveOrZero
    Integer initialQuantity){
        
    }