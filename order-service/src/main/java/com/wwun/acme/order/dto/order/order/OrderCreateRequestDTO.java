package com.wwun.acme.order.dto.order.order;

import java.util.List;

import com.wwun.acme.order.dto.order.orderItem.OrderItemCreateRequestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public class OrderCreateRequestDTO {

    @NotBlank(message = "idempotency key is required")
    private String idempotencyKey;

    @NotEmpty(message ="Order must have at least one item")
    private List<OrderItemCreateRequestDTO> items;

    public List<OrderItemCreateRequestDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemCreateRequestDTO> items) {
        this.items = items;
    }
    
}
