package com.wwun.acme.order.dto.order.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.wwun.acme.order.entity.OrderItem;

public class OrderResponseDTO {

    private UUID id;
    private UUID userId;
    private LocalDateTime orderDate;
    private BigDecimal total;
    private List<OrderItem> items;

    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }    
    public LocalDateTime getOrderDate() {
        return orderDate;
    }
    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }
    public BigDecimal getTotal() {
        return total;
    }
    public void setTotal(BigDecimal total) {
        this.total = total;
    }
    public List<OrderItem> getItems() {
        return items;
    }
    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
    public UUID getUserId() {
        return userId;
    }
    public void setUserId(UUID userId) {
        this.userId = userId;
    }

}
