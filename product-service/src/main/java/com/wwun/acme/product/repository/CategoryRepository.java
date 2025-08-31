package com.wwun.acme.product.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wwun.acme.product.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, UUID>{

}
