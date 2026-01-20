package com.wwun.acme.cart.exception;

public class ProductServiceUnavailableException extends RuntimeException{

    public ProductServiceUnavailableException(String message, Throwable cause){
        super(message, cause);
    }

}
