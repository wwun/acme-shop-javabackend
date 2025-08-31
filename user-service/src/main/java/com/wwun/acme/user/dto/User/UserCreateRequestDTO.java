package com.wwun.acme.user.dto.User;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UserCreateRequestDTO {

    @Column(unique = true)
    @NotBlank
    @Size(max=100)
    private String username;

    @Column(unique = true)
    @NotNull
    @Email(message = "Email must be formatted as user@domain.com")
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must contain at least 8 characters")
    private String password;    //wwun se recibe como string

    private List<UUID> roleIds;

    public UserCreateRequestDTO(@NotBlank @Size(max = 100) String username, @NotNull @Email String email,
            @NotBlank String password, List<UUID> roleIds) {
        this.username = username;
        this.email = email;
        this.password = password;
        //this.admin = admin != null ? admin : false;
        this.roleIds = roleIds;
    }

    public UserCreateRequestDTO() {
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
