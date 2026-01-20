package com.wwun.acme.auth.service;

import java.util.List;
import org.springframework.stereotype.Service;

import com.wwun.acme.auth.dto.AuthRequestDTO;
import com.wwun.acme.auth.dto.AuthResponseDTO;
import com.wwun.acme.auth.dto.RoleResponseDTO;
import com.wwun.acme.auth.dto.UserAuthResponseDTO;
import com.wwun.acme.security.JwtService;

@Service
public class AuthServiceImpl implements AuthService{

    private final UserGatewayService userGatewayService;
    private final JwtService jwtService;
    
    public AuthServiceImpl(UserGatewayService userGatewayService, JwtService jwtService){
        this.userGatewayService = userGatewayService;
        this.jwtService = jwtService;        
    }

    @Override
    public AuthResponseDTO login(AuthRequestDTO authRequestDTO) {
        
        UserAuthResponseDTO user = userGatewayService.verify(authRequestDTO);

        List<String> roles = user.roles().stream().map(RoleResponseDTO::name).toList();

        String token = jwtService.generateToken(user.id(), user.username(), user.email(), roles);

        return new AuthResponseDTO(token, user.username(), user.email());
    }


}