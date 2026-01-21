package com.wwun.acme.product.mapper;

import com.wwun.acme.product.dto.ProductCreateRequestDTO;
import com.wwun.acme.product.dto.ProductResponseDTO;
import com.wwun.acme.product.dto.ProductUpdateRequestDTO;
import com.wwun.acme.product.entity.Product;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-21T10:02:58-0800",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260101-2150, environment: Java 21.0.9 (Eclipse Adoptium)"
)
@Component
public class ProductMapperImpl implements ProductMapper {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public Product toEntity(ProductCreateRequestDTO productCreateRequestDTO) {
        if ( productCreateRequestDTO == null ) {
            return null;
        }

        Product product = new Product();

        product.setName( productCreateRequestDTO.getName() );
        product.setDescription( productCreateRequestDTO.getDescription() );
        product.setPrice( productCreateRequestDTO.getPrice() );
        product.setStock( productCreateRequestDTO.getStock() );

        return product;
    }

    @Override
    public Product toEntity(ProductUpdateRequestDTO productUpdateRequestDTO) {
        if ( productUpdateRequestDTO == null ) {
            return null;
        }

        Product product = new Product();

        product.setName( productUpdateRequestDTO.getName() );
        product.setDescription( productUpdateRequestDTO.getDescription() );
        product.setPrice( productUpdateRequestDTO.getPrice() );
        product.setStock( productUpdateRequestDTO.getStock() );

        return product;
    }

    @Override
    public ProductResponseDTO toResponseDTO(Product product) {
        if ( product == null ) {
            return null;
        }

        ProductResponseDTO productResponseDTO = new ProductResponseDTO();

        productResponseDTO.setId( product.getId() );
        productResponseDTO.setName( product.getName() );
        productResponseDTO.setDescription( product.getDescription() );
        productResponseDTO.setPrice( product.getPrice() );
        productResponseDTO.setStock( product.getStock() );
        productResponseDTO.setCategory( categoryMapper.toResponseDTO( product.getCategory() ) );

        return productResponseDTO;
    }
}
