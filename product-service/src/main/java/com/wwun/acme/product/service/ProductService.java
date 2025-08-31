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
    List<Product> findAll();
    Optional<Product> findById(UUID id);
    Product save(ProductCreateRequestDTO productCreateRequestDTO);
    //Product save(Product product);
    Optional<Product> update(UUID id, ProductUpdateRequestDTO productUpdateRequestDTO);
    void delete(UUID id);
    Optional<Product> updateStock(UUID id, int amount);
    Optional<Product> increaseStock(UUID id, int amount);
    Optional<Product> decreaseStock(UUID id, int amount);
    BigDecimal getProductPrice(UUID id);
    List<ProductResponseDTO> getAllById(List<UUID> productsId);
}
