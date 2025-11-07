package com.wwun.acme.cart.entity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public class Cart {

    @NotNull
    private UUID userId;

    private List<CartItem> items;

    private BigDecimal total = BigDecimal.ZERO;
    
    public Cart(UUID userId, List<CartItem> items, BigDecimal total) {
        this.userId = userId;
        this.items = items;
        this.total = total;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
    
}
