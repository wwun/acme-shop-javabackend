package com.wwun.acme.user.service;

import java.util.Optional;
import java.util.UUID;

import com.wwun.acme.user.dto.role.RoleCreateRequestDTO;
import com.wwun.acme.user.dto.role.RoleUpdateRequestDTO;
import com.wwun.acme.user.entity.Role;

public interface RoleService {

    Role save(RoleCreateRequestDTO roleCreateRequestDTO);
    Optional<Role> findById(UUID id);
    Optional<Role> update(UUID id, RoleUpdateRequestDTO roleUpdateRequestDTO);
    void delete(UUID id);

}