package com.wwun.acme.product.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class HealthController {
    @GetMapping("/health")
    public String health() {
        return "OK from products";
    }
}
