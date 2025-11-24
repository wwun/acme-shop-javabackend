package com.wwun.acme.auth.dto;

public record AuthResponseDTO (String token, String username, String email) {}