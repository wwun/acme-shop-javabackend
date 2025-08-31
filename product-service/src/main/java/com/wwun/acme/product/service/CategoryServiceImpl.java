package com.wwun.acme.product.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wwun.acme.product.dto.CategoryCreateRequestDTO;
import com.wwun.acme.product.dto.CategoryUpdateRequestDTO;
import com.wwun.acme.product.entity.Category;
import com.wwun.acme.product.exception.CategoryNotFoundException;
import com.wwun.acme.product.exception.InvalidCategoryException;
import com.wwun.acme.product.mapper.CategoryMapper;
import com.wwun.acme.product.repository.CategoryRepository;

@Service
public class CategoryServiceImpl implements CategoryService{
    
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper){
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return categoryRepository.findById(id);
    }

    @Override
    @Transactional
    public Category save(CategoryCreateRequestDTO categoryCreateRequestDTO) {
        Category category = categoryMapper.toEntity(categoryCreateRequestDTO);
        if(category.getName()==null || category.getName().equals("")){
            throw new InvalidCategoryException("Invalid category data in DTo");
        }
        return categoryRepository.save(category);
    }

    @Transactional
    private Category save(Category category){
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if(categoryRepository.findById(id).isEmpty()){
            throw new CategoryNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Optional<Category> update(UUID id, CategoryUpdateRequestDTO categoryUpdateRequestDTO) {
        if(categoryRepository.findById(id).isEmpty()){
            throw new CategoryNotFoundException("Category not found with id: " + id);
        }

        Category category = categoryMapper.toEntity(categoryUpdateRequestDTO);

        if(category.getName()==null || category.getName().isBlank())
            throw new InvalidCategoryException("Invalid category data in DTo");
        
        return categoryRepository.findById(id)
            .map(existingCategory -> {
                existingCategory.setName(category.getName());
                return categoryRepository.save(existingCategory);
            });
    }

    
}
