package com.wwun.acme.cart.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartSummaryDTO(UUID userId, List<CartSummaryItemDTO> items, int totalItems, BigDecimal totalAmount) {}
