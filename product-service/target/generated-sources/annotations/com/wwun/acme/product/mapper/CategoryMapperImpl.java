package com.wwun.acme.product.mapper;

import com.wwun.acme.product.dto.CategoryCreateRequestDTO;
import com.wwun.acme.product.dto.CategoryResponseDTO;
import com.wwun.acme.product.dto.CategoryUpdateRequestDTO;
import com.wwun.acme.product.entity.Category;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-07T06:47:41-0800",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251023-0518, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class CategoryMapperImpl implements CategoryMapper {

    @Override
    public Category toEntity(CategoryCreateRequestDTO categoryCreateRequestDTO) {
        if ( categoryCreateRequestDTO == null ) {
            return null;
        }

        Category category = new Category();

        category.setName( categoryCreateRequestDTO.getName() );

        return category;
    }

    @Override
    public Category toEntity(CategoryUpdateRequestDTO CategoryUpdateRequestDTO) {
        if ( CategoryUpdateRequestDTO == null ) {
            return null;
        }

        Category category = new Category();

        category.setName( CategoryUpdateRequestDTO.getName() );

        return category;
    }

    @Override
    public CategoryResponseDTO toResponseDTO(Category category) {
        if ( category == null ) {
            return null;
        }

        CategoryResponseDTO categoryResponseDTO = new CategoryResponseDTO();

        categoryResponseDTO.setId( category.getId() );
        categoryResponseDTO.setName( category.getName() );

        return categoryResponseDTO;
    }
}
