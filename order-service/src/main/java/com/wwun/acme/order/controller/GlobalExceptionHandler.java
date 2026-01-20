package com.wwun.acme.order.controller;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.wwun.acme.order.dto.order.HandlerExceptionDTO;
import com.wwun.acme.order.exception.InvalidOrderException;
import com.wwun.acme.order.exception.InvalidOrderItemException;
import com.wwun.acme.order.exception.OrderItemNotFoundException;
import com.wwun.acme.order.exception.OrderNotFoundException;
import com.wwun.acme.order.exception.ProductServiceUnavailableException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<HandlerExceptionDTO> handleOrderNotFoundException(OrderNotFoundException e){
        HandlerExceptionDTO error = new HandlerExceptionDTO("ORDER_NOT_FOUND", e.getMessage(), HttpStatus.NOT_FOUND.value(), new Date());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<HandlerExceptionDTO> handleInvalidOrderException(InvalidOrderException e){
        HandlerExceptionDTO error = new HandlerExceptionDTO("INVALID_ORDER", e.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(OrderItemNotFoundException.class)
    public ResponseEntity<HandlerExceptionDTO> handleOrderItemNotFoundException(OrderNotFoundException e){
        HandlerExceptionDTO error = setErrorValues("ORDER_ITEM_NOT_FOUND", e.getMessage(), HttpStatus.NOT_FOUND.value(), new Date());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidOrderItemException.class)
    public ResponseEntity<HandlerExceptionDTO> handleInvalidOrderItemException(InvalidOrderItemException e){
        HandlerExceptionDTO error = new HandlerExceptionDTO("INVALID_ORDER_ITEM", e.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ProductServiceUnavailableException.class)
    public ResponseEntity<HandlerExceptionDTO> handlerProductServiceUnavailableException(ProductServiceUnavailableException e){
        HandlerExceptionDTO error = new HandlerExceptionDTO("PRODUCT_SERVICE_UNAVAILABLE", e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE.value(), new Date());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    public HandlerExceptionDTO setErrorValues(String errorType, String errorMessage, Integer statusCode, Date date){
        return new HandlerExceptionDTO(errorType, errorMessage, statusCode, date);
    }
}
