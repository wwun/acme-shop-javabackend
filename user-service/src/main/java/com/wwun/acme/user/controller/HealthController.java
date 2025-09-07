package com.wwun.acme.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {
    @GetMapping("/users/health")
    public String userHealth() {
        return "OK from users";
    }
    @GetMapping("/auth/health")
    public String health() {
        return "OK from auth";
    }
}