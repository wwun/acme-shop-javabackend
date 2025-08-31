package com.wwun.acme.product.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wwun.acme.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, UUID>{
}
