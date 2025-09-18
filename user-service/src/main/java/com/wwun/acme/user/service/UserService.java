package com.wwun.acme.user.service;

import java.util.Optional;
import java.util.UUID;

import com.wwun.acme.user.dto.User.UserCreateRequestDTO;
import com.wwun.acme.user.dto.User.UserUpdateRequestDTO;
import com.wwun.acme.user.entity.User;

public interface UserService {

    User save(UserCreateRequestDTO userCreateRequestDTO);
    Optional<User> findById(UUID id);
    Optional<User> findByUsername(String username);
    Optional<User> update(UUID id, UserUpdateRequestDTO userUpdateRequestDTO);
    void delete(UUID id);

}