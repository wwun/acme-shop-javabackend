## PORT: http://localhost:8088 (direct connection)
         this service will be accessible from api gateway servic


## Overview

The Order Service manages the **creation and lifecycle of orders**.
It orchestrates data from Cart, Product, and User services to ensure order consistency.

## Responsibilities

- Create orders from a user's current cart.
- Validate product availability and prices.
- Persist order details and statuses.
- Expose order history for users.
- Enforce role-based access control

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Data JPA / Hibernate
- PostgreSQL
- Spring Cloud OpenFeign
- Spring Cloud Config
- Eureka Client
- Resilience4J (product service integration)

## Main Entities

- `Order` (id, userId, orderDate, total, items)
- `OrderItem` (id, orderId, productId, quantity, priceAtPurchase)

## Main Endpoints

### Orders
- `POST /api/orders` – Create order (USER, ADMIN)
- `GET /api/orders/{id}` – Get order by id (USER, ADMIN)
- `GET /api/orders/user/{userId}` – Get orders by user (ADMIN, USER*)
- `PUT /api/orders/{id}` – Update order (ADMIN, USER)
- `DELETE /api/orders/{id}` – Delete order (ADMIN, USER)

\* `userId` is validated against the authenticated user or admin role.

### Order Items
- `GET /api/orderItems/{orderId}/items`
- `POST /api/orderItems/{orderId}/items` (ADMIN)
- `PUT /api/orderItems/{orderId}/items/{id}` (ADMIN)
- `DELETE /api/orderItems/{orderId}/items/{id}` (ADMIN)

## Dependencies

- Cart Service
- Product Service
- User Service
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
