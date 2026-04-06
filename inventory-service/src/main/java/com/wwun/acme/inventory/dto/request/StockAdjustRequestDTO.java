package com.wwun.acme.inventory.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustRequestDTO {

    @NotNull
    UUID productId;

    @NotNull
    @PositiveOrZero
    Integer quantity;

}
