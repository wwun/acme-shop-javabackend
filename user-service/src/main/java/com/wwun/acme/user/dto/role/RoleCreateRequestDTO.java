package com.wwun.acme.user.dto.role;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RoleCreateRequestDTO {

    @NotBlank
    @Column(unique = true)
    private String name;
    
    @Size(max = 200)
    private String description;

    public RoleCreateRequestDTO(@NotBlank String name, @Size(max = 200) String description) {
        this.name = name;
        this.description = description;
    }

    public RoleCreateRequestDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
