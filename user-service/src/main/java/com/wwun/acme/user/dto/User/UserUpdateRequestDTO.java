package com.wwun.acme.user.dto.User;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UserUpdateRequestDTO {

    @Column(unique = true)
    @NotNull
    @Email(message = "Email must be formatted as user@domain.com")
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must contain at least 8 characters")
    private String password;

    private List<UUID> roleIds;

    public UserUpdateRequestDTO(@NotNull @Email String email, @NotBlank String password,
            List<UUID> roleIds) {
        this.email = email;
        this.password = password;
        this.roleIds = roleIds;
    }

    public UserUpdateRequestDTO() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<UUID> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<UUID> roleIds) {
        this.roleIds = roleIds;
    }
    
}
