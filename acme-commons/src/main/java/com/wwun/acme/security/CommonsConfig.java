package com.wwun.acme.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonsConfig {

    @Bean
    public JwtService jwtService(@Value("${jwt.secret}") String secretKey, @Value("${jwt.expiration}") long jwtExpiration){
        return new JwtService(secretKey, jwtExpiration);
    }

}
