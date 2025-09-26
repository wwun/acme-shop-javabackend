package com.wwun.acme.security;

import java.util.UUID;

public class AuthUserPrincipal {

    private UUID userId;
    private String username;
    
    public AuthUserPrincipal(UUID userId, String username){
        this.userId = userId;
        this.username = username;
    }

    public UUID getUserId(){
        return userId;
    }

    public String getUsername(){
        return username;
    }

}
