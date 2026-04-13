package com.wwun.acme.inventory.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wwun.acme.inventory.entity.StockMovement;
import com.wwun.acme.inventory.enums.StockMovementType;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID>{

    List<StockMovement> findAllByProductId(UUID productId);
    List<StockMovement> findAllByOrderIdAndType(UUID orderId, StockMovementType type);

}
