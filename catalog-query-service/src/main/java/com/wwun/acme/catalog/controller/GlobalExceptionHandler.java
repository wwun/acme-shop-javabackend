package com.wwun.acme.catalog.controller;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.wwun.acme.catalog.dto.response.HandlerExceptionDTO;
import com.wwun.acme.catalog.exception.CatalogQueryException;
import com.wwun.acme.catalog.exception.ExternalServiceException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CatalogQueryException.class)
    public ResponseEntity<HandlerExceptionDTO> catalogQueryExceptionHandler(CatalogQueryException ex){
        HandlerExceptionDTO error = new HandlerExceptionDTO("CATALOG_QUERY_ERROR", ex.getMessage(), HttpStatus.BAD_REQUEST.value(), Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<HandlerExceptionDTO> externalServiceExceptionHandler(ExternalServiceException ex){
        HandlerExceptionDTO error = new HandlerExceptionDTO("EXTERNAL_SERVICE_ERROR", ex.getMessage(), HttpStatus.BAD_GATEWAY.value(), Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<HandlerExceptionDTO> genericExceptionHandler(Exception ex){
        HandlerExceptionDTO error = new HandlerExceptionDTO("INTERNAL_SERVER_ERROR", "Unexpected error while building catalog response", HttpStatus.INTERNAL_SERVER_ERROR.value(), Instant.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

}
