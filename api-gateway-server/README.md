# PORT: http://localhost:8090

# API Gateway - Acme Shop

## Overview

The API Gateway is the single entry point for all external clients.
It handles routing, security enforcement, and cross-cutting concerns for the Acme Shop microservices ecosystem.

This service represents the **edge layer** of the system

# Responsibilities

- Route incoming HTTP requests to the correct microservice.
- Validate JWT tokens and propagate authenticated user context.
- Apply resiliency patterns (circuit breaker, retry, timeouts) via Resilience4J.
- Centralize logging, metrics, and gateway-level concerns.

# Tech Stack

- Java 17
- Spring Boot 3
- Spring Cloud Gateway
- Spring Security (JWT)
- Resilience4J
- Spring Cloud Config
- Eureka Client

# Main Routes

- `GET /api/products/**` → Product Service
- `GET /api/carts/**` → Cart Service
- `GET /api/orders/**` → Order Service
- `GET /api/users/**` → User Service
- `GET /api/auth/**` → Auth Service

# Security

- Acts as an OAuth2 Resource Server and validates JWTs for all protected routes
- Public routes: /api/auth/**, /oauth2/**, /login/oauth2/**, Swagger, health
- For authenticated requests, the gateway forwards the request to downstream services (Authorization header preserve

# Config and Discovery

- Retrieves configuration from Config Server.
- Registers as a client in Eureka for service discovery.

# How to Run
### Option A — Run as part of the full system (recommended)
From the repository root:
```bash
docker compose up --build

### Option B — Run locally (dev mode)
From the repository root:
```bash
mvn -pl api-gateway-server -am spring-boot:run