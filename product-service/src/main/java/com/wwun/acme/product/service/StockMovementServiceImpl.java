package com.wwun.acme.product.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.wwun.acme.product.entity.StockMovement;
import com.wwun.acme.product.exception.ProductNotFoundException;
import com.wwun.acme.product.repository.ProductRepository;
import com.wwun.acme.product.repository.StockMovementRepository;

public class StockMovementServiceImpl implements StockMovementService{

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;

    public StockMovementServiceImpl(StockMovementRepository stockMovementRepository, ProductRepository productRepository){
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
    }

    @Override
    public StockMovement save(StockMovement stockMovement){
        return stockMovementRepository.save(stockMovement);
    }

    @Override
    public List<StockMovement> findAllByProductId(UUID productId){
        productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));
        return stockMovementRepository.findAllByProductId(productId);
    }

    @Override
    public List<StockMovement> findAllByDate(LocalDate date){
        return null;
    }
    
    @Override
    public List<StockMovement> findAllByDateBetween(LocalDate startDate, LocalDate endDate){
        return null;
    }

}
