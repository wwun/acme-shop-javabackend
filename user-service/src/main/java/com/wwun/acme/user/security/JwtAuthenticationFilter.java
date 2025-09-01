package com.wwun.acme.user.security;

import static com.wwun.acme.security.TokenJwtConfig.*;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// import com.wwun.acme.user.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter{

    // private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(/*JwtService jwtService,*/ UserDetailsService userDetailsService){
        // this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // final String authHeader = request.getHeader(HEADER_AUTHORIZATION);
        // if(authHeader == null || !authHeader.startsWith(PREFIX_HEADER)){
        //     filterChain.doFilter(request, response);
        //     return;
        // }

        // final String jwt = authHeader.substring(PREFIX_HEADER.length()).trim();

        // final String username = jwtService.extractUsername(jwt);

        // if(username != null && SecurityContextHolder.getContext().getAuthentication() == null){
        //     UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        //     if(jwtService.validateToken(jwt, userDetails)){
        //         UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        //         authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        //         SecurityContextHolder.getContext().setAuthentication(authToken);
        //     }
        // }
        // filterChain.doFilter(request, response);
    }

}