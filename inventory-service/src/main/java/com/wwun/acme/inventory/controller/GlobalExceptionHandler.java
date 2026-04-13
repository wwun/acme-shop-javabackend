package com.wwun.acme.inventory.controller;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.wwun.acme.inventory.dto.response.HandlerExceptionDTO;
import com.wwun.acme.inventory.exception.ConflictException;
import com.wwun.acme.inventory.exception.InsufficientStockException;
import com.wwun.acme.inventory.exception.InvalidStockAmountException;
import com.wwun.acme.inventory.exception.InventoryNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<HandlerExceptionDTO> conflictExceptionHanlder(ConflictException ex){
        HandlerExceptionDTO error = new HandlerExceptionDTO("CONFLICT", ex.getMessage(), HttpStatus.CONFLICT.value(), Instant.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<HandlerExceptionDTO> insufficientStockExceptionDTO(InsufficientStockException ex){
        HandlerExceptionDTO error = new HandlerExceptionDTO("INSUFFICIENT_STOCK", ex.getMessage(), HttpStatus.BAD_REQUEST.value(), Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(InvalidStockAmountException.class)
    public ResponseEntity<HandlerExceptionDTO> invalidStockAmountExceptionHanlder(InvalidStockAmountException ex){
        HandlerExceptionDTO error = new HandlerExceptionDTO("INVALID_STOCK_AMOUNT", ex.getMessage(), HttpStatus.BAD_REQUEST.value(), Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<HandlerExceptionDTO> inventoryNotFoundExceptionHanlder(InventoryNotFoundException ex){
        HandlerExceptionDTO error = new HandlerExceptionDTO("INVENTORY_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND.value(), Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
}
