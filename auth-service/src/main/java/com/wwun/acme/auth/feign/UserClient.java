package com.wwun.acme.auth.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.wwun.acme.auth.dto.AuthRequestDTO;
import com.wwun.acme.auth.dto.UserAuthResponseDTO;

@FeignClient(name = "msvc-user", path = "internal/auth")
public interface UserClient {

    @PostMapping("/verify")
    UserAuthResponseDTO verify(@RequestBody AuthRequestDTO authRequestDTO);
}
