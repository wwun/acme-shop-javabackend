package com.wwun.acme.order.exception;

public class ProductServiceUnavailableException extends RuntimeException{

    public ProductServiceUnavailableException(String message, Throwable cause){
        super(message, cause);
    }

}
