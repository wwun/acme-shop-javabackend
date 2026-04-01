package com.wwun.acme.product.dto;

import com.wwun.acme.product.enums.StockOperation;

public record StockMovementResponseDTO(Integer quantity, StockOperation operation){

}