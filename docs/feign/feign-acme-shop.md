# Feign en Acme Shop

Este documento consolida las notas de Feign y las alinea con la implementacion real de Acme Shop.

Feign es un cliente HTTP declarativo. Permite que un microservicio llame a otro usando una interfaz Java, en vez de construir requests HTTP manualmente.

## Como Usar Este Documento

```text
Secciones 1-9:
  Entender Feign y como se usa en Acme Shop.

Secciones 10-12:
  Paso a paso para replicarlo rapido.

Secciones 13-17:
  Criterio senior/enterprise: errores, resilience, security, contratos y alternativas.
```

## 1. Feign vs API Gateway

Feign y API Gateway no resuelven el mismo problema.

```text
API Gateway:
  Cliente externo/front/mobile -> microservicios.

Feign:
  Microservicio -> microservicio.
```

Ejemplo:

```text
Frontend -> API Gateway -> catalog-query-service
catalog-query-service -> product-service con Feign
catalog-query-service -> inventory-service con Feign
```

Frase de entrevista:

> The gateway is the external entry point. Feign is for internal service-to-service communication.

## 2. Que Hace `@FeignClient`

Ejemplo:

```java
@FeignClient(name = "msvc-products")
public interface ProductClient {

    @PostMapping("/api/products/batch")
    List<ProductResponseDTO> getProductsById(@RequestBody List<UUID> productIds);
}
```

Esto le dice a Spring:

```text
Crea una implementacion HTTP para esta interfaz.
Cuando alguien llame getProductsById, haz un POST real a /api/products/batch del servicio msvc-products.
```

El desarrollador llama:

```java
productClient.getProductsById(productIds);
```

Feign hace:

```text
resolver servicio
construir request HTTP
serializar body
enviar request
deserializar response
retornar DTO
```

## 3. `@EnableFeignClients`

La aplicacion consumidora debe habilitar Feign:

```java
@EnableFeignClients
@SpringBootApplication
public class CatalogQueryApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogQueryApplication.class, args);
    }
}
```

Sin esto, Spring no crea los beans de tus interfaces Feign.

## 4. Service Discovery

Con Eureka:

```java
@FeignClient(name = "msvc-products")
```

Feign usa el nombre del servicio y Eureka/Spring Cloud LoadBalancer resuelve instancias disponibles.

Sin Eureka, puedes usar URL fija:

```java
@FeignClient(name = "products", url = "${clients.products.url}")
```

En proyectos modernos con Kubernetes, muchas empresas usan DNS interno de Kubernetes o service mesh en vez de Eureka.

```text
Eureka:
  bueno para aprender Spring Cloud y proyectos Netflix OSS.

Kubernetes DNS/service mesh:
  mas comun en plataformas cloud modernas.
```

## 5. Rutas: Base URL vs Path

El nombre/URL del servicio no debe mezclar la ruta del endpoint.

Correcto:

```java
@FeignClient(name = "msvc-products")
public interface ProductClient {
    @GetMapping("/api/products/{id}")
    ProductResponseDTO getById(@PathVariable UUID id);
}
```

La ruta vive en el metodo.

Evitar:

```java
@FeignClient(name = "msvc-products", url = "http://localhost:8001/api/products")
```

y luego repetir rutas raras en los metodos.

Separacion:

```text
Service discovery/base URL:
  como llegar al servicio.

Feign method mapping:
  que endpoint llamar dentro del servicio.
```

## 6. Feign En Acme Shop

### auth-service -> user-service

Archivos:

- [UserClient](/home/william/Documents/portfolio/acme-shop/auth-service/src/main/java/com/wwun/acme/auth/feign/UserClient.java)
- [OAuthUserClient](/home/william/Documents/portfolio/acme-shop/auth-service/src/main/java/com/wwun/acme/auth/feign/OAuthUserClient.java)
- [UserGatewayService](/home/william/Documents/portfolio/acme-shop/auth-service/src/main/java/com/wwun/acme/auth/service/UserGatewayService.java)

Uso:

```text
auth-service no accede directamente a la BD de users.
Llama endpoints internos de user-service.
```

### order-service -> product-service

Archivos:

- [ProductClient](/home/william/Documents/portfolio/acme-shop/order-service/src/main/java/com/wwun/acme/order/feign/ProductClient.java)
- [ProductGatewayService](/home/william/Documents/portfolio/acme-shop/order-service/src/main/java/com/wwun/acme/order/service/ProductGatewayService.java)

Uso:

```text
order-service consulta productos/precios para crear ordenes.
```

### cart-service -> product-service

Archivos:

- [ProductClient](/home/william/Documents/portfolio/acme-shop/cart-service/src/main/java/com/wwun/acme/cart/feign/ProductClient.java)
- [ProductGatewayService](/home/william/Documents/portfolio/acme-shop/cart-service/src/main/java/com/wwun/acme/cart/service/ProductGatewayService.java)

Uso:

```text
cart-service obtiene datos de producto para summary.
```

### catalog-query-service -> product/inventory

Archivos:

- [ProductClient](/home/william/Documents/portfolio/acme-shop/catalog-query-service/src/main/java/com/wwun/acme/catalog/feign/ProductClient.java)
- [InventoryClient](/home/william/Documents/portfolio/acme-shop/catalog-query-service/src/main/java/com/wwun/acme/catalog/feign/InventoryClient.java)

Uso:

```text
catalog-query-service compone producto + disponibilidad.
```

### inventory-service -> order-service

Archivo:

- [OrderClient](/home/william/Documents/portfolio/acme-shop/inventory-service/src/main/java/com/wwun/acme/inventory/feign/OrderClient.java)

Uso:

```text
inventory-service puede notificar/cambiar estado de orden despues de eventos de stock.
```

## 7. Gateway Service Pattern

En varios servicios no se usa el Feign client directamente desde el service principal. Se envuelve en un `GatewayService`.

Ejemplo:

```text
OrderServiceImpl
  -> ProductGatewayService
  -> ProductClient
```

Ventajas:

```text
Centraliza manejo de errores.
Evita que FeignException se filtre por todo el dominio.
Permite agregar retries/circuit breakers/fallbacks en un solo lugar.
Hace mas facil mockear en tests.
```

Frase de entrevista:

> I prefer wrapping Feign clients behind a gateway/service adapter so the domain service does not depend directly on HTTP exceptions or downstream contract details.

## 8. DTO Contracts

El DTO del cliente Feign debe coincidir con el JSON que devuelve el servicio llamado.

No tiene que ser la misma clase Java del servicio remoto.

Ejemplo:

```text
product-service devuelve:
{
  "id": "...",
  "name": "Smartphone",
  "category": {
    "id": "...",
    "name": "Electronics"
  }
}
```

Entonces el cliente debe tener campos compatibles:

```java
public record ProductResponseDTO(
    UUID id,
    String name,
    String description,
    BigDecimal price,
    CategoryResponseDTO category
) {}
```

Si el JSON trae `category`, el record debe llamarse `category`, no `categoryResponseDTO`, salvo que uses `@JsonProperty("category")`.

Regla:

```text
Feign DTO modela el contrato HTTP, no la entidad interna.
```

## 9. Batch y N+1

Problema N+1:

```text
catalog-query-service obtiene 20 productos.
Luego llama inventory-service 20 veces, una por producto.
```

Eso escala mal.

Mejor:

```text
1 llamada a product-service batch.
1 llamada a inventory-service batch.
```

Ejemplo:

```java
@PostMapping("/api/products/batch")
List<ProductResponseDTO> getProductsById(@RequestBody List<UUID> productIds);
```

Aunque sea lectura, `POST` es aceptable para batch/query con body.

Frase de entrevista:

> I avoid service-to-service N+1 calls by exposing batch endpoints for read composition.

## 10. Paso a Paso Para Agregar Feign

### Paso 1. Dependencia

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

### Paso 2. Habilitar Feign

```java
@EnableFeignClients
@SpringBootApplication
public class ServiceApplication {
}
```

### Paso 3. Crear DTO cliente

```java
public record ProductResponseDTO(
    UUID id,
    String name,
    BigDecimal price
) {}
```

### Paso 4. Crear Feign client

```java
@FeignClient(name = "msvc-products", configuration = FeignSecurityConfig.class)
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductResponseDTO getById(@PathVariable UUID id);
}
```

### Paso 5. Crear GatewayService

```java
@Service
public class ProductGatewayService {

    private final ProductClient productClient;

    public ProductGatewayService(ProductClient productClient) {
        this.productClient = productClient;
    }

    public ProductResponseDTO getById(UUID productId) {
        try {
            return productClient.getById(productId);
        } catch (FeignException.NotFound ex) {
            throw new ProductNotFoundException("Product not found: " + productId);
        } catch (FeignException ex) {
            throw new ExternalServiceException("Product service unavailable");
        }
    }
}
```

### Paso 6. Usar GatewayService

```java
ProductResponseDTO product = productGatewayService.getById(productId);
```

## 11. Seguridad: Propagar Token

Si el servicio destino exige JWT, Feign debe mandar `Authorization`.

Config:

```java
@Configuration
public class FeignSecurityConfig {

    @Bean
    public RequestInterceptor jwtFeignInterceptor(){
        return new JwtFeignInterceptor();
    }
}
```

Client:

```java
@FeignClient(name = "msvc-products", configuration = FeignSecurityConfig.class)
public interface ProductClient {
}
```

`RequestTemplate` representa la request que Feign esta construyendo antes de enviarla.

```java
template.header("Authorization", "Bearer " + token);
```

Frase clave:

> Before Feign sends the real HTTP request, the interceptor can modify the request template and add headers such as Authorization.

## 12. Paso a Paso Para Probar

```text
1. Levantar Eureka/config/gateway/servicios.
2. Hacer login y obtener token.
3. Llamar endpoint que usa Feign.
4. Verificar que el downstream recibe Authorization si lo requiere.
5. Apagar downstream y revisar error controlado.
6. Probar IDs inexistentes y revisar mapeo de 404.
```

## 13. Manejo de Errores

No dejar que `FeignException` se filtre como error generico.

Separar:

```text
4xx downstream:
  request invalida, recurso no existe, forbidden.

5xx / timeout / connection:
  dependencia externa fallando.
```

Ejemplo:

```java
catch (FeignException.NotFound ex) {
    throw new ProductNotFoundException(...);
}
catch (FeignException.Forbidden ex) {
    throw new ExternalServiceException("Forbidden calling product-service");
}
catch (FeignException ex) {
    throw new ExternalServiceException("Product service unavailable");
}
```

## 14. Resilience4j

Feign debe tener timeouts, retries y circuit breaker con cuidado.

Reglas:

```text
Timeout:
  siempre.

Retry:
  solo para operaciones idempotentes o errores transitorios.

Circuit breaker:
  proteger al servicio consumidor cuando downstream falla.

Fallback:
  solo si puedes devolver una respuesta honesta.
```

Ejemplo:

```text
catalog -> product:
  si inventory falla, tal vez availability UNKNOWN.

payment -> bank-core:
  no inventar success con fallback.
```

## 15. Feign vs WebClient vs RestClient

### Feign

Bueno para:

```text
HTTP declarativo entre microservicios.
Interfaces simples.
Spring Cloud/Eureka.
```

### WebClient

Bueno para:

```text
reactivo/no bloqueante.
streaming.
muchas llamadas concurrentes.
Spring WebFlux.
```

Evitar usar `.block()` sin criterio, porque convierte el flujo en bloqueante.

### RestClient

Spring moderno introdujo `RestClient` como cliente HTTP síncrono y fluido.

Bueno para:

```text
proyectos nuevos sin Spring Cloud Feign.
clientes HTTP simples.
control mas explicito que Feign.
```

Decision practica:

```text
Proyecto Spring Cloud con service discovery:
  Feign sigue siendo una buena opcion.

Proyecto nuevo simple o sin discovery:
  RestClient puede ser suficiente.

Proyecto reactivo:
  WebClient.
```

## 16. Lecciones Aprendidas: Catalog Query

### Gateway route vs Feign route

En catalog hubo dos rutas distintas que no deben mezclarse:

```text
Cliente externo:
  Bruno/browser -> API Gateway -> catalog-query-service

Llamadas internas:
  catalog-query-service -> product-service con Feign
  catalog-query-service -> inventory-service con Feign
```

El gateway usa rutas como:

```yaml
- id: msvc-catalogs
  uri: lb://msvc-catalogs
  predicates:
    - Path=/api/catalogs/**
```

Feign usa nombres de servicio:

```java
@FeignClient(name = "msvc-products")
public interface ProductClient {
}
```

Regla mental:

```text
Gateway route:
  resuelve entrada externa.

Feign client:
  resuelve comunicacion interna servicio-a-servicio.
```

### Body del endpoint batch

El endpoint de catalog no recibe un array directo. Recibe un objeto:

```json
{
  "productIds": [
    "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
  ]
}
```

Esto debe coincidir con:

```java
public record InventoryBatchRequestDTO(List<UUID> productIds) {
}
```

Si se manda solo:

```json
[
  "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
]
```

no coincide con el contrato del controller.

### 401/403 desde downstream

Catalog puede estar arriba y aun asi devolver error si las llamadas Feign a product/inventory fallan.

Casos:

```text
401:
  No llego token o el token es invalido.

403:
  El token existe, pero no tiene rol/scope suficiente.

500:
  El servicio agregador puede estar envolviendo mal una FeignException.
```

Mejor practica:

```text
No convertir todos los errores Feign en 500 generico.
Mapear:
  FeignException.Unauthorized -> 401 o ExternalServiceException clara
  FeignException.Forbidden -> 403 o ExternalServiceException clara
  FeignException.NotFound -> dato faltante / unavailable / 404 segun contrato
```

Respuesta senior:

> When debugging an aggregator service, I separate edge routing from downstream calls. A gateway 401 means the route exists but authentication failed. A direct service 500 may actually hide a downstream Feign 401/403, so I inspect logs and map FeignException explicitly instead of leaking a generic internal error.

## 17. Produccion / Enterprise

En empresas grandes, Feign o cualquier cliente HTTP debe tener:

```text
timeouts
retries controlados
circuit breakers
bulkheads si aplica
observabilidad
correlation id
token propagation o service credentials
contratos versionados
tests de integracion/contract tests
```

No basta con que compile.

Pregunta senior:

```text
Que pasa si el servicio llamado esta lento, caido, devuelve 404, 500 o cambia su contrato?
```

## 18. Checklist Para Servicio Nuevo

```text
1. Definir si realmente necesitas llamada sincrona.
2. Crear endpoint remoto estable.
3. Crear DTO cliente compatible con JSON.
4. Crear FeignClient.
5. Agregar @EnableFeignClients.
6. Agregar FeignSecurityConfig si necesita token propagation.
7. Envolver Feign en GatewayService.
8. Mapear FeignException a errores del dominio/aplicacion.
9. Agregar timeout.
10. Agregar circuit breaker/retry si aplica.
11. Evitar N+1 con batch endpoints.
12. Agregar tests.
```

## 19. Que Decir En Entrevista

Version corta:

> I use Feign for synchronous service-to-service HTTP calls. I keep the Feign interface focused on the remote contract and wrap it in a gateway service to handle errors, resilience and mapping. For secured downstream services, I propagate the Bearer token with a Feign RequestInterceptor.

Version senior:

> I treat Feign calls as remote network calls, not local method calls. That means I design for timeouts, failures, contract compatibility, observability and security propagation. I avoid N+1 service calls by using batch endpoints, and for critical workflows I avoid relying only on synchronous calls when an event-driven or transactional pattern is more appropriate.
