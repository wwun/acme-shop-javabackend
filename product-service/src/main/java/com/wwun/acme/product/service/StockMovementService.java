package com.wwun.acme.product.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.wwun.acme.product.entity.StockMovement;

public interface StockMovementService {

    StockMovement save(StockMovement StockMovement);
    List<StockMovement> findAllByProductId(UUID ProductId);
    List<StockMovement> findAllByDate(LocalDate date);
    List<StockMovement> findAllByDateBetween(LocalDate startDate, LocalDate endDate);

}
