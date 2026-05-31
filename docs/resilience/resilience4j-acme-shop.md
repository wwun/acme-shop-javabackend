# Resilience4j en Acme Shop

Este documento consolida las notas de Resilience4j y las alinea con la implementacion real de Acme Shop.

Resilience4j pertenece al tema de tolerancia a fallos en sistemas distribuidos. Sirve para aceptar una realidad basica de microservicios:

```text
Las llamadas remotas fallan.
```

Pueden fallar por:

- servicio caido
- red lenta
- timeout
- saturacion
- deploy en progreso
- base de datos lenta en el servicio destino
- errores intermitentes
- demasiadas llamadas simultaneas

## 1. Idea Principal

En codigo local, llamar un metodo parece simple:

```java
productService.getById(id)
```

Pero en microservicios, muchas veces eso realmente es:

```text
cart-service
  -> HTTP / Feign
  -> product-service
  -> DB
```

Esa llamada no debe tratarse como una llamada local.

Debe tener:

- timeout
- manejo de errores
- circuit breaker
- retry controlado
- fallback si aplica
- metricas
- logs

## 2. Patrones Principales

### Circuit Breaker

Evita seguir llamando a un servicio que esta fallando.

Estados:

```text
CLOSED
  llamadas pasan normalmente

OPEN
  llamadas se bloquean rapido
  no se insiste contra un servicio roto

HALF_OPEN
  se dejan pasar algunas llamadas de prueba
  si funcionan, vuelve a CLOSED
  si fallan, vuelve a OPEN
```

El objetivo no es "arreglar" el servicio caido. Es proteger al sistema que llama.

### Retry

Reintenta una operacion cuando hay fallas temporales.

Sirve para errores transitorios:

- timeout breve
- connection reset
- 503 temporal
- red inestable

Pero puede ser peligroso si se usa sin criterio.

No se debe reintentar automaticamente cualquier `POST` que pueda causar efectos secundarios.

Ejemplo peligroso:

```text
POST /payments
```

Si el primer intento cobro pero la respuesta se perdio, un retry podria duplicar el cobro si no hay idempotencia.

### Time Limiter

Limita cuanto tiempo esperas una respuesta.

Sin timeout, una llamada lenta puede consumir threads/conexiones y hacer lento todo el sistema.

### Rate Limiter

Limita cuantas llamadas se permiten en una ventana de tiempo.

Puede proteger:

- login
- password reset
- APIs caras
- servicios downstream con cuota

### Bulkhead

Aisla recursos para que una dependencia lenta no consuma todo el pool.

Ejemplo:

```text
payment-service -> fraud-service lento
```

Con bulkhead, ese problema no deberia bloquear todas las llamadas de `payment-service`.

## 3. Conceptos De Configuracion

`slidingWindowSize`:

Numero de llamadas que se usan para calcular errores/lentitud.

`failureRateThreshold`:

Porcentaje de fallas que abre el circuito.

`waitDurationInOpenState`:

Tiempo que el circuito queda abierto antes de probar de nuevo.

`permittedNumberOfCallsInHalfOpenState`:

Cantidad de llamadas permitidas en estado half-open.

`slowCallDurationThreshold`:

Duracion a partir de la cual una llamada se considera lenta.

`slowCallRateThreshold`:

Porcentaje de llamadas lentas que puede abrir el circuito.

`timeoutDuration`:

Tiempo maximo que se permite para una operacion protegida por TimeLimiter.

## 4. Resilience4j En Acme Shop

En Acme Shop, Resilience4j aparece principalmente en servicios que llaman otros servicios por Feign.

Patron usado:

```text
Service real
  -> GatewayService / adapter
      -> FeignClient
```

Ejemplo:

```text
cart-service
  -> ProductGatewayService
  -> ProductClient
  -> product-service
```

Esto es una buena practica: el servicio de dominio no deberia depender directamente de detalles HTTP, Feign exceptions o fallbacks.

## 5. Cart Service

Archivo:

[ProductGatewayService.java](/home/william/Documents/portfolio/acme-shop/cart-service/src/main/java/com/wwun/acme/cart/service/ProductGatewayService.java)

```java
@CircuitBreaker(name = "products", fallbackMethod = "getByIdFallback")
@Retry(name = "products")
public ProductResponseDTO getById(UUID id){
    return productClient.getById(id);
}
```

Esto significa:

- la llamada a `product-service` esta protegida por circuit breaker
- si falla de forma transitoria, puede aplicar retry
- si el circuit breaker/falla se activa, llama a fallback

Fallback:

```java
private ProductResponseDTO getByIdFallback(UUID id, Throwable ex){
    throw new ProductServiceUnavailableException("Cannot load product info", ex);
}
```

Este fallback no inventa un producto falso.

Hace algo mas correcto para este dominio:

```text
si no puedo cargar info de producto, respondo error controlado
```

Eso es mejor que devolver datos inventados.

## 6. Order Service

Archivo:

[ProductGatewayService.java](/home/william/Documents/portfolio/acme-shop/order-service/src/main/java/com/wwun/acme/order/service/ProductGatewayService.java)

```java
@CircuitBreaker(name = "products", fallbackMethod = "getAllByIdFallback")
@Retry(name = "products")
public List<ProductResponseDTO> getAllById(List<UUID> ids){
    return productClient.getAllById(ids);
}
```

Uso:

```text
order-service necesita product-service para validar/preciar productos.
```

Si `product-service` no responde, `order-service` no deberia crear una orden con precios inventados.

Fallback:

```java
private List<ProductResponseDTO> getAllByIdFallback(List<UUID> ids, Throwable ex){
    throw new ProductServiceUnavailableException("Product service unavailable", ex);
}
```

Esto mantiene consistencia:

```text
si no puedo confirmar productos/precios, no creo la orden.
```

## 7. Auth Service

Archivo:

[UserGatewayService.java](/home/william/Documents/portfolio/acme-shop/auth-service/src/main/java/com/wwun/acme/auth/service/UserGatewayService.java)

```java
@CircuitBreaker(name = "users", fallbackMethod = "verifyFallback")
@Retry(name = "users")
public UserAuthResponseDTO verify(AuthRequestDTO authRequestDTO){
    return userClient.verify(authRequestDTO);
}
```

Uso:

```text
auth-service necesita user-service para verificar credenciales/datos.
```

Si `user-service` no esta disponible, auth no deberia autenticar por fallback.

Correcto:

```text
fallar cerrado
```

En seguridad, fallback no debe conceder acceso.

## 8. Dependencias En Acme

Servicios con dependencia Resilience4j:

- `cart-service`
- `order-service`
- `auth-service`
- `inventory-service`
- `catalog-query-service`

Dependencia comun:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

Si usas anotaciones como `@CircuitBreaker` y `@Retry`, normalmente tambien necesitas AOP disponible.

En algunos starters de Spring Cloud puede venir transitivamente, pero si las anotaciones no se aplican, revisar:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

## 9. Configuracion Por YAML

Forma recomendada para configuracion mantenible:

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 20s
        permitted-number-of-calls-in-half-open-state: 3
        slow-call-duration-threshold: 2s
        slow-call-rate-threshold: 50
    instances:
      products:
        base-config: default

  retry:
    instances:
      products:
        max-attempts: 3
        wait-duration: 200ms
```

La clave importante:

```yaml
instances:
  products:
```

debe coincidir con:

```java
@CircuitBreaker(name = "products")
@Retry(name = "products")
```

## 10. Fallback Correcto vs Fallback Peligroso

Fallback correcto:

```text
devolver error controlado
devolver cache si es seguro
devolver respuesta degradada si no rompe negocio
encolar operacion si el flujo lo permite
```

Fallback peligroso:

```text
inventar precios
inventar stock
autenticar si user-service falla
marcar pago como aprobado si payment provider falla
crear orden sin validar datos criticos
```

En Acme, para order/cart/auth, el fallback que lanza una exception controlada es una decision sana.

No siempre fallback significa "devolver algo bonito".

A veces fallback correcto es:

```text
fail fast con mensaje controlado
```

## 11. Retry Con Cuidado

Retry es util, pero no gratis.

Cada retry aumenta carga.

Ejemplo:

```text
100 requests
3 retries
= hasta 300 llamadas al servicio caido
```

Si todos los servicios reintentan agresivamente, puedes empeorar una caida.

Reglas:

- retry corto
- pocos intentos
- backoff
- jitter
- solo en errores transitorios
- cuidado con operaciones no idempotentes

Para `GET /products/{id}`:

```text
retry puede tener sentido
```

Para `POST /payments`:

```text
retry solo con idempotency key y contrato claro
```

## 12. Observaciones De Mejora En Acme

Estas no son criticas para entender el patron, pero si son buenas lecciones.

### Cart fallback de getAll

En `cart-service`, el metodo:

```java
public List<ProductResponseDTO> getAll()
```

tiene fallback:

```java
private ProductResponseDTO getAllFallback(Throwable ex)
```

La firma deberia devolver el mismo tipo compatible:

```java
private List<ProductResponseDTO> getAllFallback(Throwable ex)
```

Resilience4j necesita encontrar un fallback compatible con el metodo protegido.

### Auth upsert fallback

En `auth-service`, el metodo:

```java
@CircuitBreaker(name = "users", fallbackMethod = "upsertVerify")
public UserAuthResponseDTO upsert(...)
```

debe tener un fallback con ese nombre y firma compatible.

Tambien se observa que el constructor solo recibe `UserClient`, pero existe campo `OAuthUserClient`.

Esto deberia revisarse para evitar `NullPointerException` si se llama `upsert`.

### Config visible

Las anotaciones existen, pero conviene tener configuracion explicita por servicio:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      products: ...
      users: ...
```

Asi no dependes de defaults que quizas no recuerdes en entrevista.

## 13. Como Probar Resilience4j

Pruebas manuales:

1. Levantar todos los servicios.
2. Probar flujo normal.
3. Bajar `product-service`.
4. Llamar `cart-service` u `order-service`.
5. Verificar que no queda colgado.
6. Verificar respuesta controlada.
7. Revisar logs.
8. Levantar `product-service` de nuevo.
9. Esperar que el circuit breaker permita recuperar.

Pruebas tecnicas:

- simular `FeignException`
- simular timeout
- simular 5xx
- verificar fallback
- verificar que no se crea orden con datos incompletos
- verificar metricas de circuit breaker

## 14. Donde Aplicarlo En Acme

Buenos candidatos:

- `order-service -> product-service`
- `cart-service -> product-service`
- `auth-service -> user-service`
- `catalog-query-service -> product-service`
- `catalog-query-service -> inventory-service`
- llamadas futuras a `payment-service`

No todo necesita fallback complejo.

Pero toda llamada remota importante deberia tener:

- timeout
- manejo de exception
- metricas
- estrategia de degradacion o fail-fast

## 15. Paso a Paso Para Implementar En Un Servicio

### Paso 1. Agregar dependencia

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

Si usas anotaciones y no se activan:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### Paso 2. Encapsular Feign En Un Gateway Service

```java
@Service
public class ProductGatewayService {
    private final ProductClient productClient;

    public ProductGatewayService(ProductClient productClient) {
        this.productClient = productClient;
    }
}
```

### Paso 3. Proteger La Llamada

```java
@CircuitBreaker(name = "products", fallbackMethod = "getByIdFallback")
@Retry(name = "products")
public ProductResponseDTO getById(UUID id) {
    return productClient.getById(id);
}
```

### Paso 4. Crear Fallback Compatible

```java
private ProductResponseDTO getByIdFallback(UUID id, Throwable ex) {
    throw new ProductServiceUnavailableException("Product service unavailable", ex);
}
```

La firma debe coincidir:

- mismos parametros del metodo original
- opcionalmente `Throwable` al final
- retorno compatible

### Paso 5. Configurar YAML

```yaml
resilience4j:
  circuitbreaker:
    instances:
      products:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 20s
  retry:
    instances:
      products:
        max-attempts: 3
        wait-duration: 200ms
```

### Paso 6. Probar Falla Real

No basta con compilar.

Probar:

- servicio destino caido
- servicio destino lento
- 500
- 404 si aplica
- timeout
- recuperacion

## 16. Version Corta Para Entrevista

> Resilience4j helps protect service-to-service calls. In Acme Shop I use circuit breakers and retries around Feign clients through gateway services, so remote failures are handled explicitly instead of leaking low-level HTTP failures into business logic.

## 17. Version Senior

> I do not treat Feign calls as local method calls. I wrap them with resilience policies: timeouts, circuit breakers, retries only when safe, and fallbacks that preserve domain consistency. For order and payment flows, I prefer fail-fast or idempotent/event-driven recovery over fake fallback data, because returning invented prices, stock or payment status would corrupt the business process.

## 18. Que No Decir

No diria:

Resilience4j evita que los servicios fallen.

Retry siempre mejora disponibilidad.

Fallback siempre debe devolver datos fake.

Circuit breaker reemplaza timeout.

Puedo hacer retry de pagos sin idempotencia.

Si tengo Eureka, no necesito circuit breaker.

Si tengo Kubernetes, no necesito Resilience4j.

## 19. Fuentes Utiles

- Resilience4j official docs: https://resilience4j.readme.io/docs/getting-started
- Spring Cloud CircuitBreaker docs: https://docs.spring.io/spring-cloud-circuitbreaker/docs/current/reference/html/
