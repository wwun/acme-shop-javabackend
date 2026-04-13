package com.wwun.acme.inventory.security;

import static com.wwun.acme.security.TokenJwtConfig.HEADER_AUTHORIZATION;
import static com.wwun.acme.security.TokenJwtConfig.PREFIX_HEADER;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.wwun.acme.security.JwtAuthConverter;
import com.wwun.acme.security.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthFilter extends OncePerRequestFilter{

    private final JwtAuthConverter jwtAuthConverter;

    public JwtAuthFilter(JwtService jwtService){
        this.jwtAuthConverter = new JwtAuthConverter(jwtService);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER_AUTHORIZATION);
        if(header != null && header.startsWith(PREFIX_HEADER)){
            String token = header.substring(PREFIX_HEADER.length()).trim();
            Authentication auth = jwtAuthConverter.convert(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
        
    }

}
