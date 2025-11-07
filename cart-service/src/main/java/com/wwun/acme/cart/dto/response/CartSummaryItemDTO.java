package com.wwun.acme.cart.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CartSummaryItemDTO(UUID productId, String name, BigDecimal price, int quantity) {

}
