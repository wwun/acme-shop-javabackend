package com.wwun.acme.user.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.wwun.acme.user.dto.role.RoleCreateRequestDTO;
import com.wwun.acme.user.dto.role.RoleResponseDTO;
import com.wwun.acme.user.dto.role.RoleUpdateRequestDTO;
import com.wwun.acme.user.mapper.RoleMapper;
import com.wwun.acme.user.service.RoleService;

import jakarta.validation.Valid;

public class RoleController {

    private final RoleService roleService;
    private final RoleMapper roleMapper;

    public RoleController(RoleService roleService, RoleMapper roleMapper){
        this.roleService = roleService;
        this.roleMapper = roleMapper;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<RoleResponseDTO> save(@Valid @RequestBody RoleCreateRequestDTO roleCreateRequestDTO){
        return ResponseEntity.status(HttpStatus.CREATED).body(roleMapper.toResponseDTO(roleService.save(roleCreateRequestDTO)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<RoleResponseDTO> findById(@PathVariable UUID id){
        return ResponseEntity.status(HttpStatus.OK).body(roleMapper.toResponseDTO(roleService.findById(id).get()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<RoleResponseDTO> update(@PathVariable UUID id, @Valid @RequestBody RoleUpdateRequestDTO roleUpdateRequestDTO){
        return ResponseEntity.status(HttpStatus.OK).body(roleMapper.toResponseDTO(roleService.update(id, roleUpdateRequestDTO).get()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id){
        roleService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
