## Overview

The Eureka Server provides service discovery for the Acme Shop microservices.
All services register themselves and discover other services dynamically at runtime

## Responsibilities

- Maintain registry of available service instances.
- Enable client-side load balancing and discovery.
- Support dynamic scaling and fault tolerance

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Cloud Netflix Eureka Server

## Main Endpoint

- Web dashboard: `http://localhost:8761`

## How to Run
### Option A — Run with Docker Compose (recommended)
From the repository root:
```bash
docker compose up --build

### Option B — Run locally (dev mode)
From the repository root:
```bash
mvn -pl auth-service -am spring-boot:run