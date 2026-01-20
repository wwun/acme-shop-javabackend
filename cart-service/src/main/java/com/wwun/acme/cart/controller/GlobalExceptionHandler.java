package com.wwun.acme.cart.controller;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.wwun.acme.cart.dto.HandlerExceptionDTO;
import com.wwun.acme.cart.exception.ProductServiceUnavailableException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductServiceUnavailableException.class)
    public ResponseEntity<HandlerExceptionDTO> handleProductServiceUnavailable(ProductServiceUnavailableException ex){
        HandlerExceptionDTO error = new HandlerExceptionDTO("INVALID_USER", ex.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

}
