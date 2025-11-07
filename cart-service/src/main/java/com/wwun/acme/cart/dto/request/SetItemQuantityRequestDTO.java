package com.wwun.acme.cart.dto.request;

import java.util.UUID;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SetItemQuantityRequestDTO(@NotNull UUID productId, @NotNull @Min(1) Integer quantity) {}
