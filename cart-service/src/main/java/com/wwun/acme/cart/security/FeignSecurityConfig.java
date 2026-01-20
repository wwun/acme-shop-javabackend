package com.wwun.acme.cart.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.wwun.acme.security.JwtFeignInterceptor;

import feign.RequestInterceptor;

@Configuration
public class FeignSecurityConfig {

    @Bean
    public RequestInterceptor jwtFeignInterceptor(){
        return new JwtFeignInterceptor();
    }

}
