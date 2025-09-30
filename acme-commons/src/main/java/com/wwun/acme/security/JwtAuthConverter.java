package com.wwun.acme.security;

import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class JwtAuthConverter {

    private final JwtService jwtService;

    public JwtAuthConverter(JwtService jwtService){
        this.jwtService = jwtService;
    }

    public Authentication convert(String token){
        if(!jwtService.isTokenValid(token)){
            return null;
        }

        String username = jwtService.extractUsername(token);
        List<String> roles = jwtService.extractRoles(token);
        String userId = jwtService.extractUserId(token);

        AuthUserPrincipal principal = new AuthUserPrincipal(UUID.fromString(userId), username);

        //convert to authorities
        List<SimpleGrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new).toList();

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

}
