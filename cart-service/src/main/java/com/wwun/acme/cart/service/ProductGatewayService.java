package com.wwun.acme.cart.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.wwun.acme.cart.dto.product.ProductResponseDTO;
import com.wwun.acme.cart.exception.ProductServiceUnavailableException;
import com.wwun.acme.cart.feign.ProductClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class ProductGatewayService {

    ProductClient productClient;
    private static final Logger log = LoggerFactory.getLogger(ProductGatewayService.class);

    public ProductGatewayService(ProductClient productClient){
        this.productClient = productClient;
    }

    @CircuitBreaker(name = "products", fallbackMethod = "getByIdFallback")
    @Retry(name = "products")
    public ProductResponseDTO getById(UUID id){
        return productClient.getById(id);
    }

    private ProductResponseDTO getByIdFallback(UUID id, Throwable ex){
        throw new ProductServiceUnavailableException("Cannot load product info", ex);
    }

    @CircuitBreaker(name = "products", fallbackMethod = "getAllFallback")
    @Retry(name = "products")
    public List<ProductResponseDTO> getAll(){
        return productClient.getAll();
    }

    private ProductResponseDTO getAllFallback(Throwable ex){
        throw new ProductServiceUnavailableException("Cannot load product info", ex);
    }
    
}
