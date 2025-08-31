package com.wwun.acme.product.dto;

import com.wwun.acme.product.enums.StockOperation;

public class StockMovementResponseDTO {
    private Integer quantity;
    private StockOperation operation;
    
    public Integer getQuantity() {
        return quantity;
    }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    public StockOperation getOperation() {
        return operation;
    }
    public void setOperation(StockOperation operation) {
        this.operation = operation;
    }

    
}
