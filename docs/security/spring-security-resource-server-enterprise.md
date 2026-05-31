# Spring Security Enterprise: OAuth2 Resource Server

Esta es la version limpia/recomendada para implementar seguridad en un proyecto nuevo con enfoque enterprise.

La version historica de Acme Shop esta en:

[spring-security-jwt-acme-shop.md](/home/william/Documents/portfolio/acme-shop/docs/security/spring-security-jwt-acme-shop.md)

Este documento no intenta explicar todo el camino de aprendizaje. Es una guia directa para implementar un flujo mas cercano a produccion.

## 1. Arquitectura Recomendada

```text
Client / Frontend / Mobile
  -> API Gateway
  -> Microservicios
```

Componentes:

```text
Authorization Server / IdP:
  Emite tokens.
  Ejemplos: Keycloak, Okta, Auth0, Azure AD/Entra ID, Cognito.

API Gateway:
  Valida token en el borde.
  Aplica seguridad gruesa: rutas publicas, CORS, rate limiting.

Microservicios:
  Actuan como OAuth2 Resource Servers.
  Validan JWT.
  Aplican autorizacion fina con @PreAuthorize.

Feign/WebClient:
  Propaga token cuando la llamada downstream representa al usuario.
```

## 2. Roles OAuth2

```text
Resource Owner:
  Usuario.

Client:
  Frontend, mobile app, Bruno/Postman, backend client.

Authorization Server:
  Emite access tokens y refresh tokens.

Resource Server:
  API protegida que recibe Bearer tokens.
```

En un sistema enterprise:

```text
auth/idp -> Authorization Server
product-service -> Resource Server
order-service -> Resource Server
payment-service -> Resource Server
account-service -> Resource Server
api-gateway -> Resource Server
```

## 3. Token Strategy

Access token:

```text
Vida corta.
Usado para llamar APIs.
Ejemplo: 5-15 minutos.
```

Refresh token:

```text
Vida mas larga.
Usado para obtener nuevo access token.
Se maneja en auth layer/client, no en resource servers.
```

Claims recomendados:

```json
{
  "sub": "william",
  "userId": "b3a5...",
  "email": "william@example.com",
  "roles": ["ROLE_USER"],
  "scope": "catalog:read orders:write",
  "iss": "https://idp.example.com",
  "aud": "acme-api",
  "iat": 1710000000,
  "exp": 1710000900
}
```

Regla:

```text
No confiar en userId enviado por body/header desde el cliente.
Leer userId desde claims firmados del JWT.
```

## 4. Dependencias

En cada microservicio Resource Server:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

Si usa Feign:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

## 5. Configuracion Enterprise Con issuer-uri

Esta es la opcion preferida cuando usas IdP real.

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://idp.example.com/realms/acme
```

Spring obtiene metadata del issuer, valida firma, expiracion e issuer.

Alternativa con JWKS:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: https://idp.example.com/.well-known/jwks.json
```

Ventaja:

```text
Los microservicios no conocen la secret privada.
Validan con llaves publicas.
Permite rotacion de llaves.
Se integra con IdPs corporativos.
```

## 6. Configuracion Con Secret Compartida

Opcion valida para proyectos propios o transicion.

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .csrf(csrf -> csrf.disable())
            .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secretKey) {
        return NimbusJwtDecoder
            .withSecretKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
            .build();
    }
}
```

Trade-off:

```text
Mas simple.
Pero todos los servicios necesitan conocer la secret.
La rotacion de secretos es mas delicada.
```

## 7. SecurityConfig Base Para Microservicio

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .csrf(csrf -> csrf.disable())
            .build();
    }
}
```

Con Resource Server ya no necesitas:

```text
JwtAuthFilter
JwtAuthConverter
validacion manual del token
```

Spring Security:

```text
lee Authorization: Bearer
extrae token
valida firma/expiracion
crea Authentication
llena SecurityContextHolder
```

## 8. Claims a Authorities

Muchos IdPs entregan roles/scopes con formatos distintos.

Spring por defecto entiende scopes como authorities tipo:

```text
SCOPE_catalog:read
SCOPE_orders:write
```

Para roles custom, crea un converter.

Ejemplo:

```java
@Bean
JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
    scopes.setAuthorityPrefix("SCOPE_");
    scopes.setAuthoritiesClaimName("scope");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
        List<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            authorities.addAll(
                roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList()
            );
        }

        return authorities;
    });

    return converter;
}
```

Y conectarlo:

```java
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
)
```

## 9. Obtener userId Desde Claims

Con Resource Server puedes leer el JWT actual:

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();

if (auth instanceof JwtAuthenticationToken jwtAuth) {
    String userId = jwtAuth.getToken().getClaimAsString("userId");
}
```

Helper recomendado:

```java
public final class SecurityUtils {

    public static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String userId = jwtAuth.getToken().getClaimAsString("userId");
            return UUID.fromString(userId);
        }

        throw new IllegalStateException("No userId found in current authentication");
    }

    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getSubject();
        }

        return String.valueOf(auth.getPrincipal());
    }
}
```

Conclusion:

```text
Necesitar userId no obliga a usar filtro custom.
Resource Server permite leer userId desde claims firmados.
```

## 10. @PreAuthorize

Roles:

```java
@PreAuthorize("hasRole('ADMIN')")
```

Requiere authority:

```text
ROLE_ADMIN
```

Scopes:

```java
@PreAuthorize("hasAuthority('SCOPE_catalog:read')")
```

Owner check:

```java
@PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(#orderId, authentication)")
```

Regla:

```text
Gateway valida token.
Microservicio decide autorizacion fina.
```

## 11. Feign Token Propagation

Resource Server valida tokens entrantes.

Feign propagation resuelve otro problema:

```text
Como reenviar el token cuando este servicio llama a otro.
```

Ejemplo:

```text
Cliente -> catalog-service
  Authorization: Bearer abc

catalog-service -> product-service
  Authorization: Bearer abc
```

### Opcion A: propagar header original

En aplicaciones servlet puedes usar `RequestContextHolder`:

```java
@Configuration
public class FeignSecurityConfig {

    @Bean
    public RequestInterceptor bearerTokenForwardingInterceptor() {
        return template -> {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attrs == null) {
                return;
            }

            String authHeader = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                template.header(HttpHeaders.AUTHORIZATION, authHeader);
            }
        };
    }
}
```

Ventaja:

```text
No depende del tipo de Authentication.
Funciona con Resource Server.
```

### Opcion B: leer JwtAuthenticationToken

Si necesitas reconstruir el header:

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();

if (auth instanceof JwtAuthenticationToken jwtAuth) {
    String tokenValue = jwtAuth.getToken().getTokenValue();
    template.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValue);
}
```

Uso:

```java
@FeignClient(name = "msvc-products", configuration = FeignSecurityConfig.class)
public interface ProductClient {
}
```

## 12. Gateway

Gateway tambien puede ser Resource Server.

WebFlux example:

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchange -> exchange
                .pathMatchers("/api/auth/**").permitAll()
                .pathMatchers("/actuator/**").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }
}
```

Gateway hace seguridad gruesa:

```text
rutas publicas
CORS
rate limiting
validacion inicial del token
```

Microservicios hacen seguridad fina:

```text
roles
scopes
ownership
reglas de negocio
```

## 13. Public Endpoints

No todo endpoint requiere login.

Ejemplo ecommerce:

```text
GET catalog/products:
  publico

POST catalog/products/batch:
  puede ser publico si solo lee catalogo

POST orders:
  requiere usuario o guest checkout bien modelado

GET orders/{id}:
  requiere owner o admin

admin catalog reindex:
  requiere admin
```

Aunque un endpoint sea publico:

```text
rate limiting
input validation
observability
cache
```

siguen siendo importantes.

## 14. Secrets

No hardcodear:

```text
jwt.secret
client secrets
database passwords
```

Opciones:

```text
Local:
  .env
  Docker Compose env

Kubernetes:
  Secrets
  External Secrets Operator

Cloud:
  AWS Secrets Manager
  AWS SSM Parameter Store
  Azure Key Vault
  GCP Secret Manager
```

Para issuer/JWKS, los microservicios no necesitan la private key.

## 15. Refresh Tokens

Resource Servers no emiten refresh tokens.

Refresh tokens pertenecen al Authorization Server/Auth layer.

Flujo:

```text
login
  -> access token corto
  -> refresh token largo

access token expira
  -> client llama /auth/refresh
  -> recibe nuevo access token
```

Buenas practicas:

```text
rotacion de refresh tokens
revocacion
almacenamiento seguro
deteccion de reuse
TTL
```

## 16. Service-to-Service Auth

No todas las llamadas internas representan a un usuario.

Dos casos:

```text
On-behalf-of user:
  propagar access token del usuario.

Backend job / system call:
  usar client credentials, service account, mTLS o IAM.
```

Ejemplo:

```text
catalog -> product:
  puede ser on-behalf-of user o public/internal read.

payment -> bank-core:
  probablemente service-to-service credentials.
```

## 17. Security Tests

Minimo por servicio:

```text
endpoint publico sin token -> 200
endpoint protegido sin token -> 401/403
token invalido -> 401
rol insuficiente -> 403
rol correcto -> 200
scope insuficiente -> 403
owner incorrecto -> 403
owner correcto -> 200
Feign con token propagation -> downstream recibe Authorization
```

Para controllers:

```text
MockMvc + spring-security-test
```

Para integracion:

```text
Testcontainers / WireMock / mock Feign clients
```

## 18. Migration Desde Filtro Custom

Pasos:

```text
1. Agregar spring-boot-starter-oauth2-resource-server.
2. Configurar issuer-uri, jwk-set-uri o JwtDecoder.
3. Reemplazar JwtAuthFilter por oauth2ResourceServer().jwt().
4. Configurar JwtAuthenticationConverter para roles/scopes.
5. Adaptar SecurityUtils para JwtAuthenticationToken.
6. Adaptar FeignSecurityConfig para propagar token.
7. Probar 401/403/200.
8. Eliminar JwtAuthFilter/JwtAuthConverter si ya no se usan.
```

## 19. Checklist Para Servicio Nuevo

```text
1. Elegir IdP/Auth Server.
2. Definir claims: userId, roles, scopes, issuer, audience.
3. Agregar security + resource-server dependencies.
4. Configurar issuer-uri/jwk-set-uri.
5. Crear SecurityConfig.
6. Crear JwtAuthenticationConverter si los roles/scopes no son default.
7. Crear SecurityUtils para leer claims.
8. Configurar @PreAuthorize.
9. Configurar Feign/WebClient token propagation.
10. Agregar tests de seguridad.
11. Configurar secretos fuera del repo.
12. Documentar public/protected/admin endpoints.
```

## 20. Que Decir En Entrevista

Version corta:

> I would implement services as OAuth2 Resource Servers. Tokens are issued by a trusted Authorization Server or IdP. Each service validates the JWT, maps claims/scopes to authorities, and uses method-level authorization with `@PreAuthorize`. User identity such as `userId` comes from signed claims, not from client-provided headers or request bodies.

Version senior:

> For production, I prefer standard OAuth2 Resource Server support over custom JWT filters. The gateway handles coarse-grained security, while services still validate tokens and enforce fine-grained authorization such as roles, scopes and ownership. For downstream calls, I propagate the user token when acting on behalf of a user, and use service credentials for backend/system calls. Secrets are externalized and security behavior is covered by 401/403 tests.

