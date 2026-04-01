package com.wwun.acme.order.dto.order;

import java.time.Instant;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HandlerExceptionDTO {

    String errorType;
    String errorMessage;
    Integer statusCode;
    Instant timestamp;

}
