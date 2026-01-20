package com.wwun.acme.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wwun.acme.user.dto.auth.OAuthUserUpsertRequestDTO;
import com.wwun.acme.user.dto.auth.UserAuthResponseDTO;
import com.wwun.acme.user.entity.User;
import com.wwun.acme.user.mapper.UserMapper;
import com.wwun.acme.user.service.UserService;

@RestController
@RequestMapping("/internal/oauth2")
public class InternalUserOAuth2Controller {
    
    private final UserService userService;
    private final UserMapper userMapper;

    public InternalUserOAuth2Controller(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @PostMapping("/upsert")
    public ResponseEntity<UserAuthResponseDTO> upsert(@RequestBody OAuthUserUpsertRequestDTO oAuthUserUpsertRequestDTO) {
        User user = userService.upsertOAuthUser(oAuthUserUpsertRequestDTO);
        return ResponseEntity.ok(userMapper.toAuthResponseDTO(user));
    }
}
