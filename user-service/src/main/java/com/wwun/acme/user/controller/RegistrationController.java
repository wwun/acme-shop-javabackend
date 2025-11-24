package com.wwun.acme.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wwun.acme.user.dto.auth.RegisterRequestDTO;
import com.wwun.acme.user.service.RegistrationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/registration")
public class RegistrationController {

    private final RegistrationService registrationService;
    
    public RegistrationController(RegistrationService registrationService){
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO){
        return ResponseEntity.status(HttpStatus.CREATED).body("User " + registrationService.register(registerRequestDTO).getUsername() + " registered successfully");
    }

}
