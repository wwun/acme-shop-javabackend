package com.wwun.acme.auth.service;

import java.util.List;
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

        String token = jwtService.generateToken(user.id(), user.username(), user.email(), roles);

        return new AuthResponseDTO(token, user.username(), user.email());
    }


}