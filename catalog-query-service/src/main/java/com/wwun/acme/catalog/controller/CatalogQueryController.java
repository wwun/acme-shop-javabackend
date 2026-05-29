package com.wwun.acme.catalog.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wwun.acme.catalog.dto.InventoryBatchRequestDTO;
import com.wwun.acme.catalog.dto.response.CatalogProductResponseDTO;
import com.wwun.acme.catalog.service.CatalogQueryService;

@RestController
@RequestMapping("/api/catalogs")
public class CatalogQueryController {

    private final CatalogQueryService catalogQueryService;

    public CatalogQueryController(CatalogQueryService catalogQueryService){
        this.catalogQueryService = catalogQueryService;
    }

    @PostMapping
    public ResponseEntity<List<CatalogProductResponseDTO>> getProductsAndInventories(@RequestBody InventoryBatchRequestDTO inventoryBatchRequestDTO){
        return ResponseEntity.ok().body(catalogQueryService.getProductsAndInventories(inventoryBatchRequestDTO.productIds()));
    }

}