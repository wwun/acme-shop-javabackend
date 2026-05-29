package com.wwun.acme.catalog.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogProductResponseDTO(
    UUID id,
    String name,
    String description,
    BigDecimal price,
    String category,
    AvailabilityResponseDTO availability){
}
