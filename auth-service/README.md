# PORT: http://localhost:8089 (direct connection)
        this service will be accessible from api gateway service

## Overview

The Auth Service is responsible for **authentication and token issuance**.
It supports both **JWT-based authentication** and **OAuth2 login with Google**, issuing JWT tokens used across the platform.

## Responsibilities

- Authenticate users using username/password (Spring Security).
- Authenticate users via OAuth2 (Google).
- Issue JWT tokens after successful authentication.
- Create or update users through the User Service.
- Act as the security boundary for identity management.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Security (OAuth2 Login, JWT)
- Spring Cloud OpenFeign
- Spring Cloud Config
- Eureka Client

## Authentication Flow

1. JWT Spring security
    1.1. User submits credentials to `/api/auth/login`
    1.2. Auth Service validates credentials via User Service
    1.3. JWT token is generated and returned
    1.4. Client uses the token through the API Gateway

2. OAuth2 google
    2.1. User initiates login at `/oauth2/authorization/google`
    2.2. Google authenticates the user
    2.3. Auth Service receives OAuth2 user information
    2.4. User is upserted in User Service with default role (`ROLE_USER`)
    2.5. JWT token is generated and returned to the client

1.5, 2.5. The client uses the JWT token on subsequent requests via the API Gateway.

## Main Endpoints

- `POST /api/auth/login` – Username/password authentication
- `GET /oauth2/authorization/google` – Start Google OAuth2 login
- `GET /login/oauth2/code/google` – OAuth2 callback


## Dependencies

- User Service (for user creation/lookup).
- Config Server.
- Eureka Server.
- acme-commons` (shared security utilities)

## Environment Variables

OAuth2 requires external credentials:

- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

These values are intentionally **not included** in the repositor

## How to Run
### Option A — Run with Docker Compose (recommended)
From the repository root:
```bash
docker compose up --build

### Option B — Run locally (dev mode)
From the repository root:
```bash
mvn -pl auth-service -am spring-boot:run