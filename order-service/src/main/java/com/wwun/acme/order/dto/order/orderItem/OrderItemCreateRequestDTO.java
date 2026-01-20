package com.wwun.acme.order.dto.order.orderItem;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class OrderItemCreateRequestDTO {

    // @NotNull
    // private UUID orderId;

    @NotNull
    private UUID productId;

    @NotNull
    @PositiveOrZero
    private Integer quantity;

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    // public UUID getOrderId() {
    //     return orderId;
    // }

    // public void setOrderId(UUID orderId) {
    //     this.orderId = orderId;
    // }

}
