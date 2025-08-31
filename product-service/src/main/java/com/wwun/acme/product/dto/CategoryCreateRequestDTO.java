package com.wwun.acme.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryCreateRequestDTO {

    @NotBlank
    @Size(max=100)
    private String name;


    public CategoryCreateRequestDTO(String name) {
        this.name = name;
    }

    

    public CategoryCreateRequestDTO() {
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
        
}
