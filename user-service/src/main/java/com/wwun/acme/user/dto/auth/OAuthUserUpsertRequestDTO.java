package com.wwun.acme.user.dto.auth;

import com.wwun.acme.security.AuthProviderEnum;

public record OAuthUserUpsertRequestDTO(
    AuthProviderEnum authProvider,
    String providerSub,
    String email,
    String name,
    String picture){}