package com.wwun.acme.product.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wwun.acme.product.dto.ProductCreateRequestDTO;
import com.wwun.acme.product.dto.ProductResponseDTO;
import com.wwun.acme.product.dto.ProductUpdateRequestDTO;
import com.wwun.acme.product.entity.Product;

public interface ProductService {
    List<ProductResponseDTO> findAll();
    ProductResponseDTO findById(UUID id);
    Product save(ProductCreateRequestDTO productCreateRequestDTO);
    //Product save(Product product);
    Optional<Product> update(UUID id, ProductUpdateRequestDTO productUpdateRequestDTO);
    void delete(UUID id);
    BigDecimal getProductPrice(UUID id);
    List<ProductResponseDTO> getAllById(List<UUID> productsId);
}
