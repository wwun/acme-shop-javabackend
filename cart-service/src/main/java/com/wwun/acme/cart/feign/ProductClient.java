package com.wwun.acme.cart.feign;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.wwun.acme.cart.dto.product.ProductResponseDTO;

@FeignClient(name="msvc-products")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductResponseDTO getById(@PathVariable UUID id);

    @GetMapping("/api/products")
    List<ProductResponseDTO> getAll();

}