package com.wwun.acme.cart.dto.response;

import java.util.UUID;

public record CartItemResponseDTO(UUID productId, int quantity) {}
