package com.wwun.acme.product.dto;

import java.util.Date;

public class HandlerExceptionDTO {
    String errorType;
    String errorMessage;
    Integer statusCode;
    Date date;
    
    public HandlerExceptionDTO(String errorType, String errorMessage, Integer statusCode, Date date) {
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.statusCode = statusCode;
        this.date = date;
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
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }

}
