package com.wwun.acme.user.dto.role;

import java.util.UUID;

public class RoleResponseDTO {

    private UUID id;
    private String name;    
    private String description;

    public RoleResponseDTO(UUID id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public RoleResponseDTO() {
    }

    public UUID getId(){
        return id;
    }
    public void setId(UUID id){
        this.id = id;
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
