package com.wwun.acme.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wwun.acme.user.dto.auth.AuthRequestDTO;
import com.wwun.acme.user.dto.auth.UserAuthResponseDTO;
import com.wwun.acme.user.dto.role.RoleResponseDTO;
import com.wwun.acme.user.service.UserService;

@RestController
@RequestMapping("/internal/auth")
public class InternalUserAuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public InternalUserAuthController(UserService userService, PasswordEncoder passwordEncoder){
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/verify")
    public ResponseEntity<UserAuthResponseDTO> verify(@RequestBody AuthRequestDTO authRequestDTO){
        return userService.findByUsername(authRequestDTO.username())
            .filter(user -> passwordEncoder.matches(authRequestDTO.password(), user.getPassword()))
            .map(user -> { 
                    List<RoleResponseDTO> roles = user.getRoles().stream()
                    .map(role -> new RoleResponseDTO(role.getId(), role.getName(), role.getDescription()))
                    .toList();
                return ResponseEntity.ok(new UserAuthResponseDTO(user.getId(),user.getUsername(),user.getEmail(),roles));
            })
            .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

}
