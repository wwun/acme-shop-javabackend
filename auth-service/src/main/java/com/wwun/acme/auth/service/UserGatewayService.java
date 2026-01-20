package com.wwun.acme.auth.service;

import org.springframework.stereotype.Service;

import com.wwun.acme.auth.dto.AuthRequestDTO;
import com.wwun.acme.auth.dto.OAuthUserUpsertRequestDTO;
import com.wwun.acme.auth.dto.UserAuthResponseDTO;
import com.wwun.acme.auth.exception.UserNotFoundException;
import com.wwun.acme.auth.feign.OAuthUserClient;
import com.wwun.acme.auth.feign.UserClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class UserGatewayService {

    private OAuthUserClient oAuthUserClient;
    private UserClient userClient;

    public UserGatewayService(UserClient userClient){
        this.userClient = userClient;
    }

    @CircuitBreaker(name = "users", fallbackMethod = "verifyFallback")
    @Retry(name = "users")
    public UserAuthResponseDTO verify(AuthRequestDTO authRequestDTO){
        return userClient.verify(authRequestDTO);
    }

    private UserAuthResponseDTO verifyFallback(AuthRequestDTO authRequestDTO, Throwable ex){
        throw new UserNotFoundException("User authorization issue", ex);
    }

    @CircuitBreaker(name = "users", fallbackMethod = "upsertVerify")
    @Retry(name = "users")
    public UserAuthResponseDTO upsert(OAuthUserUpsertRequestDTO oAuthUserUpsertRequestDTO){
        return oAuthUserClient.upsert(oAuthUserUpsertRequestDTO);
    }

    private UserAuthResponseDTO upsert(AuthRequestDTO authRequestDTO, Throwable ex){
        throw new UserNotFoundException("User not found", ex);
    }

}
