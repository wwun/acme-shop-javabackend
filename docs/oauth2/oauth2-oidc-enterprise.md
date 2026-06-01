# OAuth2/OIDC Enterprise

Este documento resume como disenaria OAuth2/OIDC para un sistema enterprise/banking.

La recomendacion principal:

```text
No implementar un Identity Provider casero si el sistema va a produccion real.
Usar un IdP estandar: Keycloak, Okta, Auth0, Azure AD/Entra ID, AWS Cognito, Ping, ForgeRock, etc.
```

Tu aplicacion no deberia reinventar:

- MFA
- password policy
- reset password
- account lockout
- device trust
- risk-based authentication
- token signing key rotation
- federation
- SSO
- audit de login

## 1. OAuth2 Vs OIDC

OAuth2:

```text
autorizacion delegada
```

OIDC:

```text
identidad sobre OAuth2
```

Para login empresarial moderno, normalmente se usa:

```text
OpenID Connect Authorization Code Flow
```

Con:

- `issuer`
- `client-id`
- `client-secret` o PKCE
- `redirect-uri`
- `scopes`
- `id_token`
- `access_token`
- `refresh_token` si aplica
- JWKS para validar firma

## 2. Arquitectura Recomendada

Flujo enterprise:

```text
Browser/Mobile
  -> Gateway / BFF
  -> IdP OIDC login
  -> tokens emitidos por IdP
  -> API Gateway valida token
  -> microservicios validan token como Resource Servers
```

Servicios:

```text
IdP / Authorization Server
  emite tokens

API Gateway
  valida token en el borde
  aplica seguridad gruesa

Microservicios
  tambien validan token
  aplican seguridad fina

Backend-to-backend
  usa client credentials o token exchange
```

## 3. Roles OAuth2 En Banking

| Rol | Ejemplo banking |
| --- | --- |
| Resource Owner | Cliente bancario o empleado |
| Client | Web app, mobile app, BFF, backend job |
| Authorization Server | Azure AD/Okta/Keycloak/Cognito |
| Resource Server | account-service, payment-service, customer-service |

Importante:

```text
Resource Server valida tokens.
Authorization Server emite tokens.
Client pide tokens.
```

No mezclar esos roles en entrevista.

## 4. Resource Server En Microservicios

Cada microservicio protegido deberia configurarse como Resource Server:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://idp.example.com/realms/bank
```

Spring descarga metadata OIDC y JWKS desde el issuer.

El servicio valida:

- firma
- issuer
- expiration
- audience si se configura
- claims/scopes/roles

El servicio no necesita un filtro JWT custom para lo basico.

## 5. Gateway Y Microservicios

Patron comun:

```text
Gateway valida token
Microservicios tambien validan token
```

Por que ambos?

Gateway:

- bloquea trafico no autenticado temprano
- centraliza CORS/rate limiting/routing
- aplica reglas gruesas

Microservicios:

- no confian ciegamente en la red
- aplican `@PreAuthorize`
- validan ownership
- resisten llamadas internas mal configuradas

Frase senior:

> The gateway is not my only security boundary. Services still validate tokens and enforce domain authorization.

## 6. Claims, Roles Y Scopes

Claims:

```text
datos dentro del token
```

Ejemplos:

- `sub`
- `email`
- `customerId`
- `employeeId`
- `roles`
- `scope`
- `aud`
- `iss`

Scopes:

```text
permisos delegados de acceso
```

Ejemplos:

- `accounts.read`
- `payments.write`
- `cards.read`

Roles:

```text
perfil o autoridad de negocio
```

Ejemplos:

- `ROLE_CUSTOMER`
- `ROLE_BANK_EMPLOYEE`
- `ROLE_ADMIN`

En enterprise conviene definir una estrategia clara:

```text
scopes para permisos API
roles/groups para perfiles
claims para identidad/contexto
```

## 7. `userId` En Resource Server

No necesitas un filtro custom solo para tener `userId`.

Con Resource Server puedes leerlo desde claims:

```java
@AuthenticationPrincipal Jwt jwt
```

Ejemplo:

```java
UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));
```

O crear un converter:

```text
JwtAuthenticationConverter
  -> authorities desde roles/scopes
  -> principal basado en sub/userId
```

Esto es mas estandar que crear un `JwtAuthFilter` propio para todo.

## 8. Access Token, ID Token Y Refresh Token

ID Token:

```text
lo consume el client para saber quien se autentico
```

Access Token:

```text
lo usan APIs/resource servers para autorizar requests
```

Refresh Token:

```text
lo usa el client/auth layer para renovar tokens
```

Microservicios normalmente NO reciben refresh tokens.

Refresh tokens deben guardarse con mucho cuidado:

- httpOnly secure cookie
- server-side session
- encrypted storage mobile
- rotacion
- revocacion

## 9. Authorization Code Flow

Para web/backend:

```text
Authorization Code Flow
```

Para SPA/mobile moderno:

```text
Authorization Code Flow + PKCE
```

No usar implicit flow en sistemas modernos.

## 10. Client Credentials

Para llamadas backend-to-backend sin usuario:

```text
Client Credentials Flow
```

Ejemplo:

```text
settlement-job -> payment-service
fraud-service -> risk-service
```

Ese token representa una aplicacion/servicio, no un usuario.

No usar token de usuario cuando la operacion es realmente de sistema.

## 11. On-Behalf-Of / Token Exchange

Cuando un servicio llama otro servicio representando al usuario:

```text
frontend -> catalog-service -> product-service
```

Opciones:

- propagar access token del usuario
- token exchange / on-behalf-of
- BFF mantiene sesion y llama APIs con token adecuado

La decision depende del IdP, arquitectura y reglas de auditoria.

Lo importante:

```text
no inventar headers de userId como fuente de verdad
usar claims firmados o tokens emitidos por IdP
```

## 12. Secrets Y Key Rotation

En produccion:

- client secrets no van en Git
- no van hardcodeados en docker-compose
- se guardan en secret manager
- se rotan periodicamente
- se usan llaves asimetricas para JWT
- se valida JWKS desde IdP

Opciones:

- Kubernetes Secrets + External Secrets
- AWS Secrets Manager
- Azure Key Vault
- HashiCorp Vault
- GCP Secret Manager

## 13. Banking: Recomendacion Realista

Para tu sistema bancario nuevo, recomendaria:

```text
Keycloak local/dev
Spring OAuth2 Resource Server en gateway y microservicios
Authorization Code + PKCE para frontend
Client Credentials para jobs/backend-to-backend
roles/scopes claros
JWT firmado con llaves asimetricas
JWKS/issuer-uri
secret manager para client secrets
tests de 401/403
```

Si quieres simular cloud:

- Keycloak en Docker/Kubernetes para local
- luego documentar equivalencia con Okta/Azure AD/Cognito

Eso es mucho mas enterprise que un `auth-service` casero emitiendo tokens HS256 para todo.

## 14. Acme Vs Banking Enterprise

| Tema | Acme actual | Banking enterprise |
| --- | --- | --- |
| Login social | `auth-service` con OAuth2 Login | IdP central OIDC |
| Token interno | JWT emitido por auth-service | JWT emitido por IdP |
| Firma | secret compartida HS256 | llaves asimetricas + JWKS |
| Gateway | Resource Server | Resource Server |
| Microservicios | filtro custom o mixto | Resource Server estandar |
| Secrets | variables/docker-compose | secret manager |
| Refresh tokens | no completo | manejado por IdP/client |
| MFA | no | IdP |
| SSO | limitado | IdP |

## 15. Tests De Seguridad

Tests importantes:

- request sin token -> 401
- token invalido -> 401
- token expirado -> 401
- token valido sin scope -> 403
- usuario A intenta leer recurso de usuario B -> 403
- admin puede acceder a endpoint admin -> 200
- callback OAuth2 maneja error del provider
- user upsert no crea duplicados
- roles/scopes se mapean bien

## 16. Observabilidad

Medir:

- login success/failure
- OAuth2 callback errors
- invalid token count
- 401/403 por endpoint
- latencia con IdP
- JWKS fetch errors
- token validation errors
- refresh failures
- suspicious login patterns

No loguear:

- access tokens
- refresh tokens
- ID tokens completos
- client secrets
- authorization codes

## 17. Respuesta Para Entrevista

Version corta:

> OAuth2 is for delegated authorization, and OIDC adds identity through ID tokens. For production login, I would use an external IdP and configure services as OAuth2 Resource Servers.

Version banking:

> In a banking system, I would not build my own IdP unless there is a strong reason. I would use an enterprise IdP such as Okta, Azure AD, Keycloak or Cognito, validate JWTs with issuer-uri/JWKS in every service, use scopes and claims for authorization, and keep refresh tokens and client secrets outside application code.

Version senior:

> I separate authentication, token issuance, token validation and domain authorization. The IdP authenticates and issues tokens; the gateway and services validate tokens; services enforce ownership and business permissions. For user-to-service calls I propagate or exchange tokens, and for service-to-service calls I use client credentials.

## 18. Que No Diria

No diria:

OAuth2 es lo mismo que JWT.

OAuth2 es solo login con Google.

OIDC y OAuth2 son exactamente lo mismo.

El gateway valida token, entonces los microservicios no necesitan seguridad.

El access token de Google debe autorizar mis APIs internas.

Refresh tokens se mandan a todos los microservicios.

Client secrets pueden ir en Git.

Un header `X-User-Id` enviado por el cliente es suficiente para autorizacion.

