package com.wwun.acme.user.dto.role;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RoleUpdateRequestDTO {

    @NotBlank
    @Column(unique = true)
    private String name;
    
    @Size(max = 200)
    private String description;

    public RoleUpdateRequestDTO(@NotBlank String name, @Size(max = 200) String description) {
        this.name = name;
        this.description = description;
    }

    public RoleUpdateRequestDTO() {
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
