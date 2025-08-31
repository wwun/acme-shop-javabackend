package com.wwun.acme.order.dto.order.order;

import java.util.List;
import java.util.UUID;

import com.wwun.acme.order.dto.order.orderItem.OrderItemUpdateRequestDTO;

import jakarta.validation.constraints.NotNull;

public class OrderUpdateRequestDTO {

    @NotNull
    private UUID userId;

    private List<OrderItemUpdateRequestDTO> items;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public List<OrderItemUpdateRequestDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemUpdateRequestDTO> items) {
        this.items = items;
    }

}
