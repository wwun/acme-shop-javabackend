package com.wwun.acme.security;

import javax.crypto.SecretKey;

import org.springframework.http.HttpHeaders;

import io.jsonwebtoken.Jwts;

public class TokenJwtConfig {

    //public static final SecretKey SECRET_KEY = Jwts.SIG.HS256.key().build();
    public static final String PREFIX_HEADER = "Bearer";
    public static final String HEADER_AUTHORIZATION = HttpHeaders.AUTHORIZATION;    //"Authorization";
    public static final String CONTENT_TYPE = "application/json";

}
