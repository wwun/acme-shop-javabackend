package com.wwun.acme.user.entity;

import java.util.List;
import java.util.UUID;

import com.wwun.acme.security.AuthProviderEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true)
    @NotBlank
    @Size(max=100)
    private String username;

    @Column(unique = true)
    @NotNull
    @Email(message = "Email must be formatted as user@domain.com")  //verificar patron
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must contain at least 8 characters")
    private String password;    //(encriptado con BCrypt)

    @ManyToMany
    @JoinTable(
        name="user_roles",
        joinColumns = @JoinColumn(name="user_id"),
        inverseJoinColumns = @JoinColumn(name="role_id")
    )
    private List<Role> roles;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProviderEnum authProvider;

    @Column(name = "provider_sub", unique = false)
    private String providerSub;

    public User(String username, String password, String email, List<Role> roles, AuthProviderEnum authProvider, String providerSub) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.roles = roles;
        this.authProvider = authProvider;
        this.providerSub = providerSub; 
    }

    public User() {
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public AuthProviderEnum getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(AuthProviderEnum authProvider) {
        this.authProvider = authProvider;
    }

    public String getProviderSub() {
        return providerSub;
    }

    public void setProviderSub(String providerSub) {
        this.providerSub = providerSub;
    }
    
}
