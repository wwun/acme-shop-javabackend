package com.wwun.acme.user.dto.auth;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RegisterRequestDTO {

    @NotBlank
    @Size(max=100)
    private String username;

    @NotNull
    @Email(message = "Email must be formatted as user@domain.com")  //verificar patron
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must contain at least 8 characters")
    private String password;    //(encriptado con BCrypt)

    private List<String> roles;

    public RegisterRequestDTO(String username, String password, String email, List<String> roles) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }

    public RegisterRequestDTO() {
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

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

}
