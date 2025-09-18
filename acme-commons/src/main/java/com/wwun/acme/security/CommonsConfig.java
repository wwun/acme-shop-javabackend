package com.wwun.acme.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonsConfig {

    @Bean
    public JwtService jwtService(){
        return new JwtService();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtService jwtService){
        return new JwtAuthFilter(jwtService);
    }
}
