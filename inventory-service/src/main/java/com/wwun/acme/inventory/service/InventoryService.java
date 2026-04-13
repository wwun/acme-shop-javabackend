package com.wwun.acme.inventory.service;

import java.util.List;
import java.util.UUID;

import com.wwun.acme.inventory.entity.Inventory;
import com.wwun.acme.inventory.entity.StockMovement;
import com.wwun.acme.inventory.event.OrderCreatedEvent;

public interface InventoryService {

    Inventory findByProductId(UUID productId);
    Inventory save(UUID productId, int initialQuantity);
    void delete(UUID productId);

    void reserveStock(OrderCreatedEvent orderCreatedEvent);
    void releaseStock(UUID orderId);

    Inventory increaseStock(UUID productId, int quantity);
    Inventory decreaseStock(UUID productId, int quantity);

    List<StockMovement> findMovementByProductId(UUID productId);

}
