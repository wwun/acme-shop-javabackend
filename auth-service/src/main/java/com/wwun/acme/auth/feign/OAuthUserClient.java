package com.wwun.acme.auth.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.wwun.acme.auth.dto.OAuthUserUpsertRequestDTO;
import com.wwun.acme.auth.dto.UserAuthResponseDTO;

@FeignClient(name = "msvc-users", contextId = "oauthUserClient", path = "/internal/oauth2")
public interface OAuthUserClient {
    @PostMapping("upsert")
    UserAuthResponseDTO upsert(@RequestBody OAuthUserUpsertRequestDTO oAuthUserUpsertRequestDTO);
}
