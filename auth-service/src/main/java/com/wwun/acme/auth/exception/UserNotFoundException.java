package com.wwun.acme.auth.exception;

public class UserNotFoundException extends RuntimeException{

    public UserNotFoundException(String message, Throwable ex){
        super(message, ex);
    }

}
