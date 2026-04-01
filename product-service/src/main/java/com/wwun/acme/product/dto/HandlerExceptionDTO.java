package com.wwun.acme.product.dto;

import java.time.Instant;
import java.util.Date;

public class HandlerExceptionDTO {
    String errorType;
    String errorMessage;
    Integer statusCode;
    Instant timestamp;
    
    public HandlerExceptionDTO(String errorType, String errorMessage, Integer statusCode, Instant timestamp) {
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.statusCode = statusCode;
        this.timestamp = timestamp;
    }
    
    public String getErrorType() {
        return errorType;
    }
    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }
    public String getErrorMessage() {
        return errorMessage;
    }
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    public Integer getStatusCode() {
        return statusCode;
    }
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
    public Instant getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

}
