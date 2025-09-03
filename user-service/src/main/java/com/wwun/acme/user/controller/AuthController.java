package com.wwun.acme.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wwun.acme.user.dto.auth.AuthRequestDTO;
import com.wwun.acme.user.dto.auth.AuthResponseDTO;
import com.wwun.acme.user.dto.auth.RegisterRequestDTO;
import com.wwun.acme.user.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    
    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO){
        return ResponseEntity.status(HttpStatus.CREATED).body("User " + authService.register(registerRequestDTO).getUsername() + " registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO authRequestDTO){
        return ResponseEntity.ok(authService.login(authRequestDTO));
    }

}
