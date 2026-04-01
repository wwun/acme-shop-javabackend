package com.wwun.acme.product.entity;

import java.time.Instant;
import java.util.UUID;

import com.wwun.acme.product.enums.StockOperation;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Instant timestamp;

    @NotNull
    @PositiveOrZero
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private StockOperation operation;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // public StockMovement() {
    // }

    // public StockMovement(LocalDateTime date, @PositiveOrZero Integer quantity, StockOperation operation, Product product) {
    //     this.date = date;
    //     this.quantity = quantity;
    //     this.operation = operation;
    //     this.product = product;
    // }

    // public UUID getId() {
    //     return id;
    // }

    // public void setId(UUID id) {
    //     this.id = id;
    // }

    // public LocalDateTime getDate() {
    //     return date;
    // }

    // public void setDate(LocalDateTime date) {
    //     this.date = date;
    // }

    // public Integer getQuantity() {
    //     return quantity;
    // }

    // public void setQuantity(Integer quantity) {
    //     this.quantity = quantity;
    // }

    // public StockOperation getOperation() {
    //     return operation;
    // }

    // public void setOperation(StockOperation operation) {
    //     this.operation = operation;
    // }

    // public Product getProduct() {
    //     return product;
    // }

    // public void setProduct(Product product) {
    //     this.product = product;
    // }

}
