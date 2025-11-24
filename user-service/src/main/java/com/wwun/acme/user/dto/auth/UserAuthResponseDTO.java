package com.wwun.acme.user.dto.auth;

import java.util.List;
import java.util.UUID;

import com.wwun.acme.user.dto.role.RoleResponseDTO;

public record UserAuthResponseDTO(UUID id, String username, String email, List<RoleResponseDTO> roles) {}
