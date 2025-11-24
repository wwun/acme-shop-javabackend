package com.wwun.acme.auth.dto;

import java.util.List;
import java.util.UUID;

public record UserAuthResponseDTO(UUID id, String username, String email, List<RoleResponseDTO> roles){}