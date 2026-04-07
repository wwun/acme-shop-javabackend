package com.wwun.acme.inventory.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wwun.acme.inventory.entity.Inventory;
import com.wwun.acme.inventory.entity.StockMovement;
import com.wwun.acme.inventory.enums.StockMovementType;
import com.wwun.acme.inventory.event.OrderCreatedEvent;
import com.wwun.acme.inventory.event.OrderItemEvent;
import com.wwun.acme.inventory.exception.ConflictException;
import com.wwun.acme.inventory.exception.InsufficientStockException;
import com.wwun.acme.inventory.exception.InvalidStockAmountException;
import com.wwun.acme.inventory.exception.InventoryNotFoundException;
import com.wwun.acme.inventory.mapper.InventoryMapper;
import com.wwun.acme.inventory.repository.InventoryRepository;
import com.wwun.acme.inventory.repository.StockMovementRepository;

@Service
public class InventoryServiceImpl implements InventoryService{

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryMapper inventoryMapper;

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    public InventoryServiceImpl(InventoryRepository inventoryRepository, StockMovementRepository stockMovementRepository, InventoryMapper inventoryMapper){
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.inventoryMapper = inventoryMapper;
    }

    @Override
    public Inventory findByProductId(UUID productId){

        if(productId==null){
            log.warn("findByProductId called with productId null");
            throw new IllegalArgumentException("productId cannot be null");
        }

        log.info("Searching inventory for productId {}", productId);
        
        return inventoryRepository.findByProductId(productId).orElseThrow(() -> {
            log.error("Inventory not found for productId: {}", productId);
            return new InventoryNotFoundException("Inventory not found with productId " +productId);
        });
        
    }

    @Override
    public Inventory save(UUID productId, int initialQuantity){

        if(productId==null){
            log.warn("save called with productId null");
            throw new IllegalArgumentException("productId cannot be null");
        }

        if (initialQuantity < 0) {
            log.warn("save called with negative initialQuantity: {} for productId: {}", initialQuantity, productId);
            throw new IllegalArgumentException("initialQuantity cannot be negative");
        }

        log.info("Saving inventory for productId: {} with initialQuantity: {}", productId, initialQuantity);

        if(inventoryRepository.findByProductId(productId).isPresent()){
            log.warn("Inventory already exists for productId {}", productId);
            throw new ConflictException("Inventory already exists for productId: " + productId);
        }
        
        Inventory inventory = Inventory.builder()
            .productId(productId)
            .quantityAvailable(initialQuantity)
            .quantityReserved(0)
            .build();

        try{
            Inventory inventorySaved = inventoryRepository.save(inventory);
            log.info("Inventory saved for productId: " + productId);
            return inventorySaved;
        }catch(DataIntegrityViolationException ex){
            log.warn("Concurrent insert or duplicate inventory detected for productId: {}", productId, ex);
            throw new ConflictException("Inventory already exists for productId: " + productId);
        }
        
    }

    @Override
    public void delete(UUID productId){
        
        if(productId==null){
            log.warn("delete called with productId null");
            throw new IllegalArgumentException("productId cannot be null");
        }

        if (!inventoryRepository.existsByProductId(productId)) {
            throw new InventoryNotFoundException("Inventory not found for productId: " + productId);
        }

        inventoryRepository.deleteByProductId(productId);
        
    }

    @Override
    @Transactional
    public void reserveStock(OrderCreatedEvent orderCreatedEvent){

        if(orderCreatedEvent == null){
            log.warn("OrderCreatedEvent cannot be null to reserveStock");
            throw new IllegalArgumentException("OrderCreatedEvent cannot be null");
        }

        if (orderCreatedEvent.items() == null || orderCreatedEvent.items().isEmpty()) {
            log.warn("OrderCreatedEvent items cannot be null or empty");
            throw new IllegalArgumentException("Order items cannot be null or empty");
        }

        log.info("Reserving stock for orderId: {}", orderCreatedEvent.orderId());

        List<Inventory> inventoriesToUpdate = new ArrayList<>();

        for(OrderItemEvent item : orderCreatedEvent.items()){
            Inventory inventory = inventoryRepository.findByProductId(item.productId())
                .orElseThrow(() -> {
                    log.error("Inventory not found for productId: {}", item.productId());
                    return new InventoryNotFoundException(
                            "Inventory not found for productId: " + item.productId());
                });

            if (inventory.getQuantityAvailable() < item.quantity()) {
                log.warn("Insufficient available quantity for productId: {}. Available: {}, requested: {}",
                        item.productId(), inventory.getQuantityAvailable(), item.quantity());
                throw new InsufficientStockException(
                        "Insufficient stock for productId: " + item.productId());
            }

            inventoriesToUpdate.add(inventory);
        }

        for (int i = 0; i < orderCreatedEvent.items().size(); i++) {
            OrderItemEvent item = orderCreatedEvent.items().get(i);
            Inventory inventory = inventoriesToUpdate.get(i);

            inventory.setQuantityAvailable(inventory.getQuantityAvailable() - item.quantity());
            inventory.setQuantityReserved(inventory.getQuantityReserved() + item.quantity());
        }

        inventoryRepository.saveAll(inventoriesToUpdate);

        for (int i = 0; i < orderCreatedEvent.items().size(); i++) {
            OrderItemEvent item = orderCreatedEvent.items().get(i);
            stockMovementRepository.save(StockMovement.builder()
                .productId(item.productId())
                .orderId(orderCreatedEvent.orderId())
                .quantity(item.quantity())
                .type(StockMovementType.RESERVED)
                .build());
        }
        
    }

    @Override
    @Transactional
    public void releaseStock(UUID orderId){
        
        List<StockMovement> reservations = stockMovementRepository.findAllByOrderIdAndType(orderId, StockMovementType.RESERVED);

        for (StockMovement reservation : reservations) {
            Inventory inventory = inventoryRepository.findByProductId(reservation.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for productId: " + reservation.getProductId()));

            inventory.setQuantityAvailable(inventory.getQuantityAvailable() + reservation.getQuantity());
            inventory.setQuantityReserved(inventory.getQuantityReserved() - reservation.getQuantity());
            
            inventoryRepository.save(inventory);

            stockMovementRepository.save(StockMovement.builder()
                .productId(reservation.getProductId())
                .orderId(orderId)
                .quantity(reservation.getQuantity())
                .type(StockMovementType.RELEASED)
                .build());
        }
        
    }

    @Override
    public Inventory increaseStock(UUID productId, int quantity){

        if (quantity <= 0){
            log.warn("decreaseStock called with negative quantity: {} for productId: {}", quantity, productId);
            throw new InvalidStockAmountException("Increase amount must be positive");
        }

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> {
                    log.error("Inventory not found for productId: {}", productId);
                    return new InventoryNotFoundException("Inventory not found for productId: " + productId);
                });

        inventory.setQuantityAvailable(inventory.getQuantityAvailable() + quantity);

        log.info("Increasing stock for productId: {}", inventory.getProductId());
        Inventory inventorySaved = inventoryRepository.save(inventory);

        stockMovementRepository.save(StockMovement.builder()
            .productId(productId)
            .orderId(null)
            .quantity(quantity)
            .type(StockMovementType.RESTOCKED)
            .build()
        );

        return inventorySaved;
        
    }

    @Override
    public Inventory decreaseStock(UUID productId, int quantity) {
        if (quantity <= 0){
            log.warn("decreaseStock called with negative quantity: {} for productId: {}", quantity, productId);
            throw new InvalidStockAmountException("Decrease amount must be positive");
        }

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> {
                    log.error("Inventory not found for productId: {}", productId);
                    return new InventoryNotFoundException("Inventory not found for productId: " + productId);
                });

        if (inventory.getQuantityAvailable() == null || inventory.getQuantityAvailable() < quantity){
            log.warn("Insufficient availabl quantity for productId: {}", productId);
            throw new InsufficientStockException("Insufficient stock: " + productId);
        }

        inventory.setQuantityAvailable(inventory.getQuantityAvailable() - quantity);

        log.info("Decreasing stock for productId: {}", inventory.getProductId());
        Inventory inventorySaved = inventoryRepository.save(inventory);

        stockMovementRepository.save(StockMovement.builder()
            .productId(productId)
            .orderId(null)
            .quantity(quantity)
            .type(StockMovementType.ADJUSTED_DOWN)
            .build()
        );

        return inventorySaved;
        
    }

    @Override
    public List<StockMovement> findMovementByProductId(UUID productId){

        if(productId==null){
            log.warn("delete called with productId null");
            throw new IllegalArgumentException("productId cannot be null");
        }
        
        return stockMovementRepository.findAllByProductId(productId);
        
    }

}
