package com.wwun.acme.order.feign;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.wwun.acme.order.dto.product.ProductResponseDTO;

@FeignClient(name="msvc-products")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductResponseDTO findById(@PathVariable("id") UUID id);

    @GetMapping("/api/products/{id}/price")
    BigDecimal getProductPrice(@PathVariable("id") UUID id);

    @PostMapping("/api/products/listids")
    List<ProductResponseDTO> getAllById(@RequestBody List<UUID> productsId);

}