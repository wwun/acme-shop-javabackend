package com.wwun.acme.user.dto.User;

import java.util.List;
import java.util.UUID;

import com.wwun.acme.user.dto.role.RoleCreateRequestDTO;

public class UserResponseDTO {

    private UUID id;
    private String username;
    private String email;
    private List<RoleCreateRequestDTO> roles;

    public UserResponseDTO(UUID id, String username, String email, List<RoleCreateRequestDTO> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }

    public UserResponseDTO() {
    }

    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public List<RoleCreateRequestDTO> getRoles() {
        return roles;
    }
    public void setRoles(List<RoleCreateRequestDTO> roles) {
        this.roles = roles;
    }

}
