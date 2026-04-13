package com.wwun.acme.product.service;

import java.util.List;
import java.util.UUID;

import com.wwun.acme.product.dto.CategoryCreateRequestDTO;
import com.wwun.acme.product.dto.CategoryUpdateRequestDTO;
import com.wwun.acme.product.entity.Category;

public interface CategoryService {
    List<Category> findAll();
    Category save(CategoryCreateRequestDTO categoryCreateRequestDTO);
    Category findById(UUID id);
    void delete(UUID id);
    Category update(UUID id, CategoryUpdateRequestDTO categoryUpdateRequestDTO);
}
