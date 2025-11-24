package com.wwun.acme.user.service;

import com.wwun.acme.user.dto.auth.RegisterRequestDTO;
import com.wwun.acme.user.entity.User;

public interface RegistrationService {

    User register(RegisterRequestDTO dto);
    
}
