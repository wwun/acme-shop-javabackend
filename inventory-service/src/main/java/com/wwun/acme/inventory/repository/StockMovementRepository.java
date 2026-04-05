package com.wwun.acme.inventory.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wwun.acme.inventory.entity.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID>{

    List<StockMovement> findAllByProductId(UUID productId);

}
