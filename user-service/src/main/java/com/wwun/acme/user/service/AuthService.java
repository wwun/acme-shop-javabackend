package com.wwun.acme.user.service;

import com.wwun.acme.user.dto.auth.AuthRequestDTO;
import com.wwun.acme.user.dto.auth.AuthResponseDTO;
import com.wwun.acme.user.dto.auth.RegisterRequestDTO;
import com.wwun.acme.user.entity.User;

public interface AuthService {

    User register(RegisterRequestDTO dto);
    AuthResponseDTO login(AuthRequestDTO dto);

}
