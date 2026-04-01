package com.wwun.acme.order.security;

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

import static com.wwun.acme.security.TokenJwtConfig.*;

public class JwtAuthFilter extends OncePerRequestFilter{

    private final JwtAuthConverter jwtAuthConverter;

    public JwtAuthFilter(JwtService jwtService){
        this.jwtAuthConverter = new JwtAuthConverter(jwtService);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        //read header and validate it
        String authHeader = request.getHeader(HEADER_AUTHORIZATION);

        if(authHeader !=null && authHeader.startsWith(PREFIX_HEADER)){
            String token = authHeader.substring(PREFIX_HEADER.length()).trim();
            Authentication auth = jwtAuthConverter.convert(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        //continue with filter chain
        filterChain.doFilter(request, response);

    }

}
