package com.wwun.acme.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;

public class JwtFeignInterceptor implements RequestInterceptor{
    
    public void apply(RequestTemplate template){
        String token = SecurityUtils.getCurrentToken();
        
        if(token==null || token.isBlank())
            return;

        template.header(TokenJwtConfig.HEADER_AUTHORIZATION, TokenJwtConfig.PREFIX_HEADER + " " +token);
    }

}
