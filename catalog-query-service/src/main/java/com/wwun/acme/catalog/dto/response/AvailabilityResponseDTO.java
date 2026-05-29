package com.wwun.acme.catalog.dto.response;

import com.wwun.acme.catalog.enums.AvailabilityStatus;

public record AvailabilityResponseDTO(
    Integer quantityAvailable,
    Integer quantityReserved,
    AvailabilityStatus status){
}
