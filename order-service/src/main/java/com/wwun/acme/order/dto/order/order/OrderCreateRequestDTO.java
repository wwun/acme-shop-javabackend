package com.wwun.acme.order.dto.order.order;

import java.util.List;

import com.wwun.acme.order.dto.order.orderItem.OrderItemCreateRequestDTO;

import jakarta.validation.constraints.NotEmpty;

public class OrderCreateRequestDTO {

    // @NotNull
    // private UUID userId;

    @NotEmpty(message ="Order must have at least one item")
    private List<OrderItemCreateRequestDTO> items;

    // public UUID getUserId() {
    //     return userId;
    // }

    // public void setUserId(UUID userId) {
    //     this.userId = userId;
    // }

    public List<OrderItemCreateRequestDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemCreateRequestDTO> items) {
        this.items = items;
    }
    
}
