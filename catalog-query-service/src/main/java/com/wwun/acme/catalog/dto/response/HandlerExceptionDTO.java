package com.wwun.acme.catalog.dto.response;

import java.time.Instant;

public record HandlerExceptionDTO(
    
    String errorType,
    String errorMessage,
    Integer statusCode,
    Instant timestamp){

}
