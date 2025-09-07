package com.wwun.acme.product.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        return http.authorizeHttpRequests(authz -> authz
            .requestMatchers("/api/products/health").permitAll()
            .requestMatchers("/actuator/health").permitAll()
            .anyRequest().permitAll())  //.anyRequest().authenticated())
            .csrf(csrf -> csrf.disable())
            .build();
    }

}
