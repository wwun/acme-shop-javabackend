# PORT: http://localhost:8085 (direct connection)
        this service will be accessible from api gateway service

## Overview

The Cart Service manages shopping carts for users.
It stores cart items and quantities, and can calculate cart totals using product prices.

## Responsibilities

- Maintain a cart per user (or per session).
- Add, update, and remove items from the cart.
- Cache product data for performance optimization.
- Validate product existence and prices via Product Service.
- Enforce security by extracting user identity from JWT tokens.

## Tech Stack

- Java 17
- Spring Boot 3
- Redis
- Spring Cloud OpenFeign
- Spring Cloud Config
- Eureka Client
- Resilience4J

## Main Entities

- `Cart` (userId, items, total)
- `CartItem` (productId, name, price, quantity)

## Main Endpoints

- `GET /api/carts` – get current user cart
- `POST /api/carts/items` – add item to cart
- `PUT /api/carts/items` – update item quantity (JSON)
- `DELETE /api/carts/items/{productId}` – remove item from cart
- `DELETE /api/carts` – clear cart

## Security Note
The `userId` is **always extracted from the JWT token**, never from the request body, to prevent client-side tampering

## Architectural Note
This service applies **CQRS-style separation** (command/query handlers).
The design is **event-driven ready** and prepared for future Kafka integratio

## Dependencies

- Product Service
- Redis
- Config Server
- Eureka Server
- avme-commons

## How to Run
### Option A — Run with Docker Compose (recommended)
From the repository root:
```bash
docker compose up --build

### Option B — Run locally (dev mode)
From the repository root:
```bash
mvn -pl auth-service -am spring-boot:run