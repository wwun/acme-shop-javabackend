package com.wwun.acme.product.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wwun.acme.product.entity.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID>{
    List<StockMovement> findAllByProductId(UUID ProductId);
    List<StockMovement> findAllByDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    @Query("SELECT sm FROM StockMovement sm WHERE CAST(sm.date AS LocalDate) = :date")
    List<StockMovement> findAllByDate(@Param("date") LocalDate date);
}
