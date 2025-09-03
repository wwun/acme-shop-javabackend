package com.wwun.acme.user.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wwun.acme.user.dto.User.UserCreateRequestDTO;
import com.wwun.acme.user.dto.User.UserResponseDTO;
import com.wwun.acme.user.dto.User.UserUpdateRequestDTO;
import com.wwun.acme.user.mapper.UserMapper;
import com.wwun.acme.user.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper){
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> save(@Valid @RequestBody UserCreateRequestDTO userCreateRequestDTO){
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponseDTO(userService.save(userCreateRequestDTO)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable UUID id){
        return ResponseEntity.status(HttpStatus.OK).body(userMapper.toResponseDTO(userService.findById(id).get()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponseDTO> update(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequestDTO userUpdateRequestDTO){
        return ResponseEntity.status(HttpStatus.OK).body(userMapper.toResponseDTO(userService.update(id, userUpdateRequestDTO).get()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id){
        userService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
