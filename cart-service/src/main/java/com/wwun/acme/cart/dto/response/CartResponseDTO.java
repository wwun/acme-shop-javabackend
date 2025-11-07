package com.wwun.acme.cart.dto.response;

import java.util.List;
import java.util.UUID;

public record CartResponseDTO(UUID userId, List<CartItemResponseDTO> items) {}
