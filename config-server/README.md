## PORT: http://localhost:8888

## Overview

The Config Server provides centralized configuration for all microservices in the Acme Shop platform.
Configuration files are stored in a dedicated Git repository and versioned per environment

## Responsibilities

- Serve configuration properties to all services.
- Support different profiles (dev, test, prod).
- Decouple configuration from service code.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Cloud Config Server

Each service has environment-specific configuration files

## Configuration Source

- Git repository (recommended)
- or local file system (for local development)

## Main Endpoint

- `GET /{application}/{profile}` – returns configuration for a given application and profile.

## How to Run
### Option A — Run with Docker Compose (recommended)
From the repository root:
```bash
docker compose up --build

### Option B — Run locally (dev mode)
From the repository root:
```bash
mvn -pl auth-service -am spring-boot:run