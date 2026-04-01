package com.wwun.acme.order.dto.order.orderItem;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemUpdateRequestDTO {

    @NotNull
    private UUID id;

    @NotNull
    private UUID orderId;
    
    @NotNull
    private UUID productId;

    @NotNull
    @PositiveOrZero
    private Integer quantity;

}
