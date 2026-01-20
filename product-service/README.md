## PORT: http://localhost:8077 (direct connection)
         this service will be accessible from api gateway servi

## Overview

The Product Service is responsible for managing products, prices, and stock.
It exposes APIs for retrieving product catalogs and detailed product information.

## Responsibilities

- CRUD operations for products.
- Manage product prices and stock levels.
- Provide product data to Cart and Order services via REST or Feign clients.
- Enforce role-based access control

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Data JPA / Hibernate
- PostgreSQL
- Spring Cloud Config
- Eureka Client

## Main Entities

- `Product` (id, name, description, price, stock, category, movements)
- `Categories` (id, name, product list)
- `StockMovement` (id, date, quantity, operation, product)

## Main Endpoints

# Product
- `GET /api/products` – list all products (ADMIN, USER)
- `GET /api/products/{id}` – get product by ID (ADMIN, USER)
- `GET /api/products/listids` – get multiple products by ID list (ADMIN only)
- `POST /api/products` – create product (ADMIN only)
- `PUT /api/products/{id}` – update product (ADMIN only)
- `PATCH /api/products/{id}/{stock}/{operation}/` update product stock (INCREASE, DECREASE, SET values allowed as operation) (ADMIN only)
- `DELETE /api/products/{id}` – delete product (ADMIN only)

# Categories
- `POST /api/categories` – create category (ADMIN)
- `PATCH /api/categories/{id}` – update category by id (ADMIN)
- `DELET /api/categories/{id}` – delete category by id (ADMIN)
- `GET /api/categories/{id}` – get category by id (ADMIN, USER)
- `GET /api/categories` – get all categories (ADMIN, USER)

## Dependencies

- Config Server
- Eureka Server
- acme-commons

## How to Run
### Option A — Run with Docker Compose (recommended)
From the repository root:
```bash
docker compose up --build

### Option B — Run locally (dev mode)
From the repository root:
```bash
mvn -pl order-service -am spring-boot:run