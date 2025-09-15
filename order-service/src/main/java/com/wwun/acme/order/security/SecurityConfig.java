package com.wwun.acme.order.security;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

@Component
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        return http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/orders/health").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated())
                //.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .build();
    }
}
