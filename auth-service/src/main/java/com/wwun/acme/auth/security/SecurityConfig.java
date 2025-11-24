package com.wwun.acme.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        return http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/actuator/**", "/health").permitAll()
            .requestMatchers("/auth/**", "/oauth2/**").permitAll()
            .anyRequest().authenticated())
            .oauth2Login(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable()).build();
    }

}
