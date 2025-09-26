package com.wwun.acme.security;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.wwun.acme.security.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static com.wwun.acme.security.TokenJwtConfig.*;

public class JwtAuthFilter extends OncePerRequestFilter{

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService){
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        //read header and validate it
        String authHeader = request.getHeader(HEADER_AUTHORIZATION);

        if(authHeader == null || !authHeader.startsWith(PREFIX_HEADER)){
            filterChain.doFilter(request, response);
            return;
        }

        //extract the token
        String token = authHeader.substring(PREFIX_HEADER.length()).trim();

        //validate token
        if(jwtService.isTokenValid(token)){
            String username = jwtService.extractUsername(token);
            List<String> roles = jwtService.extractRoles(token);
            String userId = jwtService.extractUserId(token);

            AuthUserPrincipal principal = new AuthUserPrincipal(UUID.fromString(userId), username);

            //convert to authorities
            List<SimpleGrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new).toList();

            //create authentication objet to set into the context
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);

            // usernamePasswordAuthenticationToken.setDetails(userId);

            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        }

        //continue with filter chain
        filterChain.doFilter(request, response);

    }    

}
