package com.wwun.acme.product.controller;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.wwun.acme.product.dto.HandlerExceptionDTO;
import com.wwun.acme.product.exception.CategoryNotFoundException;
import com.wwun.acme.product.exception.InsufficientStockException;
import com.wwun.acme.product.exception.InvalidProductException;
import com.wwun.acme.product.exception.InvalidStockAmountException;
import com.wwun.acme.product.exception.ProductNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<HandlerExceptionDTO> handleProductNotFoundException(ProductNotFoundException e){
        HandlerExceptionDTO error = setErrorValues("PRODUCT_NOT_FOUND", e.getMessage(), HttpStatus.NOT_FOUND.value(), new Date());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidProductException.class)
    public ResponseEntity<HandlerExceptionDTO> handleInvalidProductException(InvalidProductException e){
        HandlerExceptionDTO error = setErrorValues("INVALID_PRODUCT", e.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidStockAmountException.class)
    public ResponseEntity<HandlerExceptionDTO> handleInvalidStockAmountException(InvalidStockAmountException e){
        HandlerExceptionDTO error = setErrorValues("INVALID_STOCK_AMOUNT", e.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<HandlerExceptionDTO> handleInsufficientStockException(InsufficientStockException e){
        HandlerExceptionDTO error = setErrorValues("INSUFFICIENT_STOCK", e.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<HandlerExceptionDTO> handleIllegalArgumentException(IllegalArgumentException e) {
        HandlerExceptionDTO error = setErrorValues("INVALID_ARGUMENT", e.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<HandlerExceptionDTO> handleCategoryNotFoundException(CategoryNotFoundException e){
        HandlerExceptionDTO error = setErrorValues("INVALID_CATEGORY", e.getMessage(), HttpStatus.NOT_FOUND.value(), new Date());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<HandlerExceptionDTO> handleValidationErrors(MethodArgumentNotValidException e){
        HandlerExceptionDTO error = setErrorValues("VALIDATION_ERROR", e.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<HandlerExceptionDTO> handleInvalidFormat(HttpMessageNotReadableException e){
        HandlerExceptionDTO error = setErrorValues("MALFORMED_JSON",e.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    //InvalidCategoryException

    public HandlerExceptionDTO setErrorValues(String errorType, String message, int statusCode, Date date){
        return new HandlerExceptionDTO(errorType, message, statusCode, date);        
    }
    
}
