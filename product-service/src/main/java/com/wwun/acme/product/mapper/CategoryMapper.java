package com.wwun.acme.product.mapper;

import org.mapstruct.Mapper;

import com.wwun.acme.product.dto.CategoryCreateRequestDTO;
import com.wwun.acme.product.dto.CategoryResponseDTO;
import com.wwun.acme.product.dto.CategoryUpdateRequestDTO;
import com.wwun.acme.product.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    Category toEntity(CategoryCreateRequestDTO categoryCreateRequestDTO);
    Category toEntity(CategoryUpdateRequestDTO CategoryUpdateRequestDTO);
    CategoryResponseDTO toResponseDTO(Category category);
}
