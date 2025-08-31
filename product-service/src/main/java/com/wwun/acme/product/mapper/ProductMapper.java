package com.wwun.acme.product.mapper;

import org.mapstruct.Mapper;

import com.wwun.acme.product.dto.ProductCreateRequestDTO;
import com.wwun.acme.product.dto.ProductResponseDTO;
import com.wwun.acme.product.dto.ProductUpdateRequestDTO;
import com.wwun.acme.product.entity.Product;

@Mapper(componentModel = "spring", uses = CategoryMapper.class)  // This annotation tells MapStruct to generate a Spring bean for this mapper, ademas Esto le indica a MapStruct: “Si necesitas convertir un Category, usa el CategoryMapper
public interface ProductMapper {
    Product toEntity(ProductCreateRequestDTO productCreateRequestDTO);  // Converts DTO to Entity for creation
    Product toEntity(ProductUpdateRequestDTO productUpdateRequestDTO);  // Converts DTO to Entity for updating
    //void updateEntityFromDTO(ProductUpdateRequestDTO productUpdateRequestDTO, @MappingTarget Product product);  // Updates an existing entity with values from the DTO, MappingTarget is used to indicate that the product parameter is an existing entity that should be updated
    ProductResponseDTO toResponseDTO(Product product);  //converts Entity to Response DTO for sending back to the client
    //mappers should return the entity as Product and not an optional, optional is used more in services or repositories where a result could be empty
}
