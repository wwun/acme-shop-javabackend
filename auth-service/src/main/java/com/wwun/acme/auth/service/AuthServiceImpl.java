package com.wwun.acme.user.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.wwun.acme.auth.dto.AuthRequestDTO;
import com.wwun.acme.security.JwtService;
import com.wwun.acme.auth.dto.AuthResponseDTO;
import com.wwun.acme.auth.dto.RoleResponseDTO;
import com.wwun.acme.auth.dto.UserAuthResponseDTO;
import com.wwun.acme.auth.feign.UserClient;

@Service
public class AuthServiceImpl implements AuthService{

    private final UserClient userClient;
    private final JwtService jwtService;
    
    public AuthServiceImpl(UserClient userClient, JwtService jwtService){
        this.userClient = userClient;
        this.jwtService = jwtService;        
    }

    @Override
    public AuthResponseDTO login(AuthRequestDTO authRequestDTO) {
        
        UserAuthResponseDTO user = userClient.verify(authRequestDTO);

        List<String> roles = user.roles().stream().map(RoleResponseDTO::name).toList();

        String token = jwtService.generateToken

        return new AuthResponseDTO(token, userDetails.getUsername());
    }


}