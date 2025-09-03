package com.wwun.acme.api_gateway_server.security;

import com.wwun.acme.security.JwtService;
import static com.wwun.acme.security.TokenJwtConfig.*;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered{

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService){
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if(exchange.getRequest().getURI().getPath().startsWith("/api/auth")){
            return chain.filter(exchange);
        }

        if(authHeader == null || !authHeader.startsWith(PREFIX_HEADER)){
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(PREFIX_HEADER.length()).trim();

        try{
            
            if(!jwtService.isTokenValid(token)){
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String username = jwtService.extractUsername(token);
            List<String> roles = jwtService.extractRoles(token);
            String rolesHeader = String.join(",", roles);

            exchange = exchange.mutate().request(exchange.getRequest().mutate().header("X-Auth-Username", username).header("X-Auth-Roles", rolesHeader).build()).build();
            return chain.filter(exchange);
        }catch(Exception ex){
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

    }

    @Override
    public int getOrder() {
        return -1;
    }

}
