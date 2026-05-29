package com.wwun.acme.catalog.feign;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.wwun.acme.catalog.dto.client.product.ProductResponseDTO;

@FeignClient(name = "msvc-products")
public interface ProductClient {

    @PostMapping("/api/products/batch")
    List<ProductResponseDTO> getProductsById(@RequestBody List<UUID> productsId);

}
