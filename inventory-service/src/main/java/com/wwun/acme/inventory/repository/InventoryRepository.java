package com.wwun.acme.inventory.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wwun.acme.inventory.entity.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, UUID>{

    Optional<Inventory> findByProductId(UUID productId);
    void deleteByProductId(UUID productId);

}
