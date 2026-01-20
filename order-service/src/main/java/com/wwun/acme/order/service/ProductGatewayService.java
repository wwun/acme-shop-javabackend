package com.wwun.acme.order.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.wwun.acme.order.dto.product.ProductResponseDTO;
import com.wwun.acme.order.exception.ProductServiceUnavailableException;
import com.wwun.acme.order.feign.ProductClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class ProductGatewayService {

    private final ProductClient productClient;

    public ProductGatewayService(ProductClient productClient){
        this.productClient = productClient;
    }

    @CircuitBreaker(name = "products", fallbackMethod = "getAllByIdFallback")
    @Retry(name = "products")
    public List<ProductResponseDTO> getAllById(List<UUID> ids){
        return productClient.getAllById(ids);
    }

    private List<ProductResponseDTO> getAllByIdFallback(List<UUID> ids, Throwable ex){
        throw new ProductServiceUnavailableException("Product service unavailable", ex);
    }

    @CircuitBreaker(name = "products", fallbackMethod = "findByIdFallback")
    @Retry(name = "products")
    public ProductResponseDTO findById(UUID id){
        return productClient.findById(id);
    }

    private ProductResponseDTO findByIdFallback(UUID id, Throwable ex){
        throw new ProductServiceUnavailableException("Product service unavailabble", ex);
    }

    @CircuitBreaker(name = "products", fallbackMethod = "getProductPriceFallback")
    @Retry(name = "products")
    public BigDecimal getProductPrice(UUID id){
        return productClient.getProductPrice(id);
    }

    private BigDecimal getProductPriceFallback(UUID id, Throwable ex){
        throw new ProductServiceUnavailableException("Product service unavailable", ex);
    }
    
}