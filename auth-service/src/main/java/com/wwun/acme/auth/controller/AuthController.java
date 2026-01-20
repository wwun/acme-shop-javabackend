package com.wwun.acme.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wwun.acme.auth.dto.AuthRequestDTO;
import com.wwun.acme.auth.dto.AuthResponseDTO;
import com.wwun.acme.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO authRequestDTO){
        AuthResponseDTO authResponseDTO = authService.login(authRequestDTO);
        return ResponseEntity.ok(authResponseDTO);
    }

    @GetMapping("/health")
    public String health() {
        return "OK from login";
    }

}
