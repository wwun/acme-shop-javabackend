package com.wwun.acme.auth.service;

import com.wwun.acme.auth.dto.AuthRequestDTO;
import com.wwun.acme.auth.dto.AuthResponseDTO;

public interface AuthService {

    AuthResponseDTO login(AuthRequestDTO dto);

}
