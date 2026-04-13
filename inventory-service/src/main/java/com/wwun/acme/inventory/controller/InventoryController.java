package com.wwun.acme.inventory.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wwun.acme.inventory.dto.request.InventoryCreateRequestDTO;
import com.wwun.acme.inventory.dto.request.StockAdjustRequestDTO;
import com.wwun.acme.inventory.dto.response.InventoryResponseDTO;
import com.wwun.acme.inventory.dto.response.StockMovementResponseDTO;
import com.wwun.acme.inventory.entity.StockMovement;
import com.wwun.acme.inventory.mapper.InventoryMapper;
import com.wwun.acme.inventory.mapper.StockMovementMapper;
import com.wwun.acme.inventory.service.InventoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inventories")
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryMapper inventoryMapper;
    private final StockMovementMapper stockMovementMapper;

    public InventoryController(InventoryService inventoryService, InventoryMapper inventoryMapper, StockMovementMapper stockMovementMapper){
        this.inventoryService = inventoryService;
        this.inventoryMapper = inventoryMapper;
        this.stockMovementMapper = stockMovementMapper;
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryResponseDTO> findByProductId(@PathVariable UUID productId){
        return ResponseEntity.status(HttpStatus.OK)
            .body(inventoryMapper.toResponseDTO(inventoryService.findByProductId(productId))
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryResponseDTO> save(@Valid @RequestBody InventoryCreateRequestDTO inventoryCreateRequestDTO){
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(inventoryMapper.toResponseDTO(
                inventoryService.save(inventoryCreateRequestDTO.productId(), inventoryCreateRequestDTO.initialQuantity())
            )
        );
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable UUID productId){
        inventoryService.delete(productId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/increase")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryResponseDTO> increaseStock(@Valid @RequestBody StockAdjustRequestDTO stockAdjustRequestDTO){
        return ResponseEntity.status(HttpStatus.OK)
            .body(inventoryMapper.toResponseDTO(
                inventoryService.increaseStock(stockAdjustRequestDTO.getProductId(), stockAdjustRequestDTO.getQuantity())
            )
        );
    }

    @PatchMapping("/decrease")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryResponseDTO> decreaseStock(@Valid @RequestBody StockAdjustRequestDTO stockAdjustRequestDTO){
        return ResponseEntity.status(HttpStatus.OK)
            .body(inventoryMapper.toResponseDTO(
                inventoryService.decreaseStock(stockAdjustRequestDTO.getProductId(), stockAdjustRequestDTO.getQuantity())
            )
        );
    }

    @GetMapping("/{productId}/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<StockMovementResponseDTO>> findMovementsByProductId(@PathVariable UUID productId){
        return ResponseEntity.ok(
            inventoryService.findMovementByProductId(productId)
                .stream()
                .map(stockMovementMapper::toResponseDTO)
                .toList());
    }


    @GetMapping("/health")
    public String health(){
        return "OK from inventories";
    }

}
