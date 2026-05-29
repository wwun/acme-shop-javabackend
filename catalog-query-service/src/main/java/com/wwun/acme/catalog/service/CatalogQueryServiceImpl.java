package com.wwun.acme.catalog.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.wwun.acme.catalog.dto.client.inventory.InventoryAvailabilityDTO;
import com.wwun.acme.catalog.dto.client.product.ProductResponseDTO;
import com.wwun.acme.catalog.dto.response.AvailabilityResponseDTO;
import com.wwun.acme.catalog.dto.response.CatalogProductResponseDTO;
import com.wwun.acme.catalog.enums.AvailabilityStatus;
import com.wwun.acme.catalog.exception.CatalogQueryException;
import com.wwun.acme.catalog.exception.ExternalServiceException;
import com.wwun.acme.catalog.feign.InventoryClient;
import com.wwun.acme.catalog.feign.ProductClient;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CatalogQueryServiceImpl implements CatalogQueryService{

    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    public CatalogQueryServiceImpl(ProductClient productClient, InventoryClient inventoryClient){
        this.productClient = productClient;
        this.inventoryClient = inventoryClient;
    }

    @Override
    public List<CatalogProductResponseDTO> getProductsAndInventories(List<UUID> productIds){
        if(productIds == null || productIds.isEmpty()){
            log.warn("product list received is empty");
            throw new CatalogQueryException("Product ids cannot be null or empty");
        }

        if(productIds.stream().anyMatch(id -> id == null)){
            log.warn("product list received contains null ids");
            throw new CatalogQueryException("Product ids cannot contain null values");
        }

        List<UUID> uniqueProductIds = productIds.stream().distinct().toList();

        List<ProductResponseDTO> products = getProducts(uniqueProductIds);
        List<InventoryAvailabilityDTO> inventories = getInventories(uniqueProductIds);
        
        Map<UUID, InventoryAvailabilityDTO> inventoryByProductId = inventories.stream()
            .collect(Collectors.toMap(
                InventoryAvailabilityDTO::productId,
                Function.identity(),
                (existing, duplicated) -> existing
            ));

        return products.stream().map(product -> {
            InventoryAvailabilityDTO inventory = inventoryByProductId.get(product.id());

            AvailabilityResponseDTO availability = buildAvailability(inventory);

            return new CatalogProductResponseDTO(
                product.id(),
                product.name(),
                product.description(),
                product.price(),
                product.category() != null ? product.category().name() : null,
                availability
            );

        }).toList();

    }

    private List<ProductResponseDTO> getProducts(List<UUID> productIds){
        try{
            return productClient.getProductsById(productIds);
        }catch(FeignException ex){
            log.error("Error calling product-service. status={}, message={}", ex.status(), ex.getMessage());
            throw new ExternalServiceException("Unable to retrieve products from product-service");
        }
    }

    private List<InventoryAvailabilityDTO> getInventories(List<UUID> productIds){
        try{
            return inventoryClient.getInventoriesByProductIds(productIds);
        }catch(FeignException ex){
            log.error("Error calling inventory-service. status={}, message={}", ex.status(), ex.getMessage());
            throw new ExternalServiceException("Unable to retrieve inventory availability from inventory-service");
        }
    }

    private AvailabilityResponseDTO buildAvailability(InventoryAvailabilityDTO inventory){

        if(inventory == null){
            return new AvailabilityResponseDTO(null, null, AvailabilityStatus.UNKNOWN);
        }

        int availableToSell = inventory.quantityAvailable() - inventory.quantityReserved();
        AvailabilityStatus status;
        
        if(availableToSell <= 0){
            status = AvailabilityStatus.OUT_OF_STOCK;
        }else if(availableToSell <= 5){
            status = AvailabilityStatus.LOW_STOCK;
        }else{
            status = AvailabilityStatus.IN_STOCK;
        }

        return new AvailabilityResponseDTO(
            inventory.quantityAvailable(),
            inventory.quantityReserved(),
            status
        );

    }

}
