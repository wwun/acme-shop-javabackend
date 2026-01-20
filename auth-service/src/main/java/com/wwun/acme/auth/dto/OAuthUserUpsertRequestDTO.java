package com.wwun.acme.auth.dto;

import com.wwun.acme.security.AuthProviderEnum;

public record OAuthUserUpsertRequestDTO(
    AuthProviderEnum authProvider,
    String providerSub,
    String email,
    String name,
    String picture){}