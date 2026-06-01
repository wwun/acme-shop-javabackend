package com.wwun.acme.catalog.dto.client.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponseDTO(UUID id, String name, String description, BigDecimal price, CategoryResponseDTO category) {

}
