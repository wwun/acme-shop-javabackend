package com.wwun.acme.product.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wwun.acme.product.dto.CategoryCreateRequestDTO;
import com.wwun.acme.product.dto.CategoryResponseDTO;
import com.wwun.acme.product.dto.CategoryUpdateRequestDTO;
import com.wwun.acme.product.entity.Category;
import com.wwun.acme.product.mapper.CategoryMapper;
import com.wwun.acme.product.service.CategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    public CategoryController(CategoryService categoryService, CategoryMapper categoryMapper){
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(@Valid @RequestBody CategoryCreateRequestDTO categoryCreateRequestDTO){
        Category category = categoryService.save(categoryCreateRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryMapper.toResponseDTO(category));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryUpdateRequestDTO categoryUpdateRequestDTO){
        Optional<Category> category = categoryService.update(id, categoryUpdateRequestDTO);
        return 
        ResponseEntity.status(HttpStatus.OK).body(categoryMapper.toResponseDTO(category.get()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable UUID id){
        categoryService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/{id}")
    public CategoryResponseDTO getCategoryById(@PathVariable UUID id){
        Category category = categoryService.findById(id).get();
        return categoryMapper.toResponseDTO(category);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories(){
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.findAll()
            .stream()
            .map(categoryMapper::toResponseDTO)
            .toList());
    }

}
