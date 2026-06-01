# API Gateway en Acme Shop

Este documento consolida las notas de API Gateway y las alinea con la implementacion real de Acme Shop.

El objetivo no es solo recordar como configurar Spring Cloud Gateway, sino entender que problema resuelve, que parte es academica, que parte sirve en produccion y como explicarlo en entrevistas.

## 1. Idea Principal

Un API Gateway es la entrada principal para clientes externos.

Sin gateway:

```text
Frontend / Bruno / mobile app
  -> product-service
  -> order-service
  -> cart-service
  -> inventory-service
```

El cliente necesita conocer muchos servicios, puertos, rutas y reglas.

Con gateway:

```text
Frontend / Bruno / mobile app
  -> api-gateway-server
      -> product-service
      -> order-service
      -> cart-service
      -> inventory-service
      -> catalog-query-service
```

El cliente conoce una sola entrada:

```text
http://localhost:8090
```

Y el gateway decide a que microservicio enviar cada request.

## 2. Que Hace Un API Gateway

Un gateway normalmente se usa para:

- enrutar requests a microservicios internos
- esconder puertos y ubicaciones reales
- validar autenticacion en el borde
- aplicar CORS
- agregar logging centralizado
- aplicar filtros a requests/responses
- aplicar rate limiting
- aplicar circuit breakers/retries/timeouts
- exponer una API publica mas estable que la estructura interna

En una arquitectura de microservicios, el gateway pertenece al borde del sistema.

```text
Internet / Cliente
  -> Gateway
  -> Servicios internos
```

## 3. Gateway No Es Lo Mismo Que Eureka

Gateway y Eureka resuelven problemas distintos.

Gateway:

```text
Por donde entra el request?
A que servicio se enruta?
Que reglas de borde aplican?
```

Eureka:

```text
Donde esta registrado el servicio?
Que instancia viva puedo usar?
```

En Acme Shop se usan juntos:

```yaml
uri: lb://msvc-products
```

`lb://` significa:

- usa Spring Cloud LoadBalancer
- busca el servicio por nombre logico
- resuelve instancias registradas en Eureka

Entonces:

```text
Cliente
  -> api-gateway-server /api/products/**
  -> lb://msvc-products
  -> instancia real de product-service
```

## 4. Gateway No Es Lo Mismo Que Spring Security

Spring Security protege endpoints.

Spring Cloud Gateway enruta requests.

Pero se complementan:

```text
Gateway:
  - recibe request externo
  - aplica CORS
  - valida token en el borde
  - enruta al servicio

Microservicio:
  - valida token tambien si aplica
  - aplica reglas finas con @PreAuthorize
  - ejecuta logica de negocio
```

En Acme Shop, el gateway usa Spring Security WebFlux como OAuth2 Resource Server.

Archivo:

[SecurityConfig.java](/home/william/Documents/portfolio/acme-shop/api-gateway-server/src/main/java/com/wwun/acme/api_gateway_server/security/SecurityConfig.java)

```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
```

Eso significa:

- espera `Authorization: Bearer <token>`
- valida el JWT
- permite rutas publicas como `/api/auth/**`
- bloquea rutas protegidas sin token valido

## 5. Gateway En Acme Shop

Servicio:

[api-gateway-server](/home/william/Documents/portfolio/acme-shop/api-gateway-server)

Responsabilidades actuales:

- entrada por puerto `8090`
- configuracion desde Config Server
- registro en Eureka
- rutas hacia microservicios usando `lb://`
- OAuth2 Resource Server para validar JWT
- CORS centralizado
- filtro global de logging

Aplicacion principal:

[ApiGatewayServerApplication.java](/home/william/Documents/portfolio/acme-shop/api-gateway-server/src/main/java/com/wwun/acme/api_gateway_server/ApiGatewayServerApplication.java)

```java
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayServerApplication.class, args);
    }
}
```

`@EnableDiscoveryClient` permite integrarse con discovery.

En versiones modernas de Spring Cloud muchas veces no es estrictamente necesario si tienes la dependencia correcta, pero dejarlo explicito ayuda a entender que el gateway participa en service discovery.

## 6. Configuracion Local Del Gateway

Archivo local:

[application.yml](/home/william/Documents/portfolio/acme-shop/api-gateway-server/src/main/resources/application.yml)

```yaml
spring:
    application:
        name: msvc-gateway
    config: 
        import: optional:configserver:http://config-server:8888
```

Esto significa:

- el gateway se llama `msvc-gateway`
- descarga su configuracion desde Config Server
- las rutas reales no viven necesariamente en este archivo local

En Acme Shop, muchas rutas vienen del repositorio externo de configuracion.

## 7. Configuracion De Rutas

Una ruta de Spring Cloud Gateway tiene tres piezas principales:

```yaml
- id: msvc-products
  uri: lb://msvc-products
  predicates:
    - Path=/api/products/**
```

`id`:

Identificador interno de la ruta.

`uri`:

Destino. Puede ser:

```yaml
http://localhost:8077
```

o:

```yaml
lb://msvc-products
```

`predicates`:

Condiciones que hacen match con un request.

Ejemplo:

```yaml
Path=/api/products/**
```

Si el request entra por:

```text
GET /api/products
```

el gateway usa esa ruta.

## 8. Predicates

Un predicate decide si una ruta aplica o no.

El mas comun:

```yaml
Path=/api/products/**
```

Otros predicates existen, por ejemplo:

```yaml
Method=GET,POST
Header=X-Tenant, .+
Query=color
Cookie=session, .+
```

Pero en produccion no conviene abusar de predicates raros para seguridad.

Ejemplo academico:

```yaml
Header=Token, \d+
```

Eso sirve para aprender que Gateway puede matchear headers, pero no reemplaza seguridad real.

Para autenticacion moderna:

- Spring Security
- OAuth2 Resource Server
- JWT
- scopes/roles
- policies

## 9. Filters

Los filtros modifican o procesan requests/responses.

Ejemplos:

```yaml
filters:
  - AddRequestHeader=X-Gateway, acme
  - AddResponseHeader=X-Source, gateway
  - StripPrefix=1
```

Tipos principales:

- filtros globales
- filtros por ruta

## 10. Global Filters

Un `GlobalFilter` corre para todas las requests que pasan por el gateway.

En Acme Shop existe:

[LoggingGlobalFilter.java](/home/william/Documents/portfolio/acme-shop/api-gateway-server/src/main/java/com/wwun/acme/api_gateway_server/filter/LoggingGlobalFilter.java)

```java
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {
    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("Request path ", exchange.getRequest().getPath());
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            log.info("Response status ", exchange.getResponse().getStatusCode());
        }));
    }
}
```

Conceptos:

`ServerWebExchange`:

Representa request y response en WebFlux.

`GatewayFilterChain`:

Permite continuar con la cadena de filtros.

`Mono<Void>`:

Spring Cloud Gateway es reactivo. Por eso usa Reactor y devuelve `Mono`.

`getOrder()`:

Define prioridad. Numeros mas bajos se ejecutan antes.

Nota: el filtro actual tiene una intencion correcta, pero los logs podrian escribirse mejor:

```java
log.info("Request path: {}", exchange.getRequest().getPath());
log.info("Response status: {}", exchange.getResponse().getStatusCode());
```

## 11. Route-Specific Filters

Son filtros que solo aplican a una ruta.

Ejemplo:

```yaml
- id: msvc-orders
  uri: lb://msvc-orders
  predicates:
    - Path=/api/orders/**
  filters:
    - StripPrefix=1
```

Se usan cuando una ruta necesita tratamiento especial.

Ejemplos reales:

- quitar prefijos
- agregar headers
- circuit breaker para un servicio lento
- retry para endpoints idempotentes
- rate limiting por ruta

## 12. StripPrefix

`StripPrefix` elimina segmentos del path antes de enviar el request al microservicio.

Ejemplo:

```yaml
filters:
  - StripPrefix=1
```

Request al gateway:

```text
/api/orders/123
```

Si quitas un segmento:

```text
/orders/123
```

Esto solo debe usarse si el controller interno espera esa ruta.

En Acme Shop muchos controllers ya usan rutas tipo:

```java
@RequestMapping("/api/products")
```

Entonces si el gateway recibe `/api/products/**`, normalmente no necesitas `StripPrefix`.

Regla mental:

Si el microservicio ya expone `/api/products`, no quites `/api`.

Si el microservicio expone `/products` o `/`, quizas si necesitas `StripPrefix`.

## 13. CORS En El Gateway

El CORS suele configurarse en el borde, porque el frontend habla con el gateway.

En Acme Shop:

```java
@Bean
public CorsWebFilter corsWebFilter(){
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("*"));
    config.setAllowedMethods(List.of("GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
    config.setAllowCredentials(true);
    ...
}
```

Puntos importantes:

- `Authorization` permite enviar Bearer token
- `Content-Type` permite JSON
- `Idempotency-Key` fue necesario para crear ordenes de forma segura
- `OPTIONS` es importante para preflight requests del navegador

En produccion, evitaria:

```java
config.setAllowedOriginPatterns(List.of("*"));
config.setAllowCredentials(true);
```

Para produccion seria mejor:

```java
config.setAllowedOrigins(List.of("https://app.acme.com"));
```

o manejarlo por ambiente.

## 14. Seguridad En El Gateway

En Acme Shop el gateway hace seguridad gruesa:

```java
.authorizeExchange(exchange -> exchange
    .pathMatchers("/api/auth/**").permitAll()
    .pathMatchers("/actuator/**").permitAll()
    .pathMatchers("/api/health").permitAll()
    .pathMatchers("/webjars/**").permitAll()
    .pathMatchers("/swagger-ui.html", "/swagger-ui/**").permitAll()
    .pathMatchers("/v3/api-docs/**").permitAll()
    .pathMatchers("/api/oauth2/**", "/oauth2/**","/login/oauth2/**").permitAll()
    .anyExchange().authenticated()
)
.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
```

Eso significa:

- auth, health y docs pueden ser publicos
- todo lo demas requiere token
- el token se valida en el gateway

Importante:

El gateway no deberia ser la unica defensa.

Los microservicios tambien deben proteger reglas sensibles:

- roles
- ownership
- scopes
- permisos de negocio
- operaciones admin

Frase senior:

> El gateway bloquea trafico obvio no autenticado, pero la autorizacion fina vive en los microservicios.

## 15. API Gateway En Docker Compose

En `docker-compose.yml`, los clientes externos llaman:

```text
localhost:8090
```

Pero dentro de Docker, los servicios se comunican por nombres de contenedor/servicio.

El gateway no deberia usar:

```text
localhost:8077
```

para llamar otros servicios dentro de Docker.

Usa:

```yaml
uri: lb://msvc-products
```

si hay Eureka, o:

```yaml
uri: http://product-service:8001
```

si se usa nombre Docker directo.

## 16. Como Probar El Gateway

1. Levantar infraestructura:

```bash
docker compose up --build
```

2. Verificar gateway:

```bash
curl http://localhost:8090/actuator/health
```

3. Probar ruta publica:

```bash
curl http://localhost:8090/api/auth/...
```

4. Probar ruta protegida sin token:

```bash
curl http://localhost:8090/api/products
```

Resultado esperado:

```text
401 Unauthorized
```

Eso puede ser buena senal: significa que la ruta existe y la seguridad esta bloqueando.

5. Probar con token:

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8090/api/products
```

6. Si falla por gateway, probar directo al servicio:

```bash
curl http://localhost:8077/api/products
```

Esto separa:

- problema de gateway
- problema del servicio
- problema de seguridad
- problema de discovery

## 17. Como Leer Errores

### 404 desde gateway

Posibles causas:

- ruta no existe
- path no coincide
- config server no cargo la ruta
- gateway no recibio configuracion actualizada
- el request usa `/api/catalogs` pero la ruta configurada es otra

Interpretacion:

```text
El gateway no encontro una ruta que matchee.
```

### 401 desde gateway

Posibles causas:

- ruta existe
- Spring Security bloqueo por token faltante/invalido
- Bearer token mal pegado
- token expirado

Interpretacion:

```text
La ruta matcheo, pero seguridad rechazo.
```

Pasar de 404 a 401 muchas veces es progreso.

### 403 desde gateway o servicio

Posibles causas:

- token valido
- usuario autenticado
- rol/scope insuficiente

Interpretacion:

```text
Se quien eres, pero no tienes permiso.
```

### 503 desde gateway

Posibles causas:

- ruta existe
- `lb://service` no tiene instancias disponibles
- servicio no esta registrado en Eureka
- servicio esta caido

Interpretacion:

```text
Gateway sabe a donde quiere ir, pero no encuentra instancia viva.
```

### ECONNREFUSED

Posibles causas:

- puerto no esta escuchando
- contenedor no esta arriba
- servicio fallo al arrancar
- estas llamando el puerto equivocado

Interpretacion:

```text
No hay proceso aceptando conexiones en host:puerto.
```

## 18. Lo Academico vs Produccion

### Academico / curso

Ejemplos utiles para aprender:

```yaml
Header=Token, \d+
SetResponseHeader=Content-Type, text/plain
AddRequestParameter=name, William
SampleCookieGatewayFilterFactory
```

Sirven para entender:

- predicates
- filters
- request mutation
- response mutation
- orden de filtros

Pero no son lo que venderia como arquitectura enterprise.

### Mas realista

En una arquitectura mas seria, mostraria:

- OAuth2 Resource Server en gateway
- CORS controlado
- routes claras
- rate limiting
- circuit breakers
- timeouts
- retry solo en operaciones idempotentes
- correlation id
- structured logs
- metrics por ruta
- tracing distribuido
- tests de rutas/security

## 19. API Gateway En Kubernetes

Kubernetes no reemplaza automaticamente al API Gateway.

Kubernetes puede reemplazar a Eureka para service discovery.

Pero el gateway sigue siendo util para:

- entrada publica
- autenticacion central
- control de rutas externas
- CORS
- rate limiting
- politicas de borde
- observability en el edge

En Kubernetes aparecen varias opciones:

- Ingress Controller
- Kubernetes Gateway API
- Spring Cloud Gateway
- Kong
- NGINX
- Traefik
- Apigee
- AWS API Gateway
- Azure API Management
- service mesh ingress gateway

La decision depende de la empresa y plataforma.

## 20. Paso a Paso Para Implementar Spring Cloud Gateway

### Paso 1. Crear modulo gateway

Dependencias:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

Si usas Eureka:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### Paso 2. Definir nombre y puerto

```yaml
spring:
  application:
    name: msvc-gateway

server:
  port: 8090
```

### Paso 3. Conectar Config Server si aplica

```yaml
spring:
  config:
    import: optional:configserver:http://config-server:8888
```

### Paso 4. Crear rutas

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: msvc-products
          uri: lb://msvc-products
          predicates:
            - Path=/api/products/**

        - id: msvc-orders
          uri: lb://msvc-orders
          predicates:
            - Path=/api/orders/**
```

### Paso 5. Configurar seguridad

```java
@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
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
```

### Paso 6. Configurar CORS

```java
@Bean
public CorsWebFilter corsWebFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://app.acme.com"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsWebFilter(source);
}
```

### Paso 7. Agregar logging/correlation id

Minimo:

```java
log.info("Request path: {}", exchange.getRequest().getPath());
log.info("Response status: {}", exchange.getResponse().getStatusCode());
```

Mejor:

- correlation id
- duration
- route id
- status code
- user id si no contiene datos sensibles

### Paso 8. Probar por gateway y directo

Siempre probar ambos:

```text
gateway -> servicio
servicio directo
```

Esto ayuda a saber si falla:

- routing
- auth
- discovery
- microservicio
- config

## 21. Version Corta Para Entrevista

> Spring Cloud Gateway is the edge entry point of Acme Shop. It receives external HTTP requests, matches routes using predicates such as path, resolves services through `lb://` and Eureka, applies cross-cutting concerns such as CORS, JWT validation and logging, and forwards the request to the correct microservice.

## 22. Version Senior

> I use the gateway for edge concerns, not for business logic. It handles coarse-grained authentication, CORS, routing, rate limiting and observability. The microservices still enforce fine-grained authorization and business rules. In a Kubernetes-based platform, I would evaluate whether Spring Cloud Gateway is the right edge gateway or whether the platform already provides API gateway capabilities through Ingress/Gateway API, Kong, Apigee or a cloud API gateway.

## 23. Que No Decir

No diria:

Gateway reemplaza microservicios.

Gateway reemplaza Spring Security.

Gateway reemplaza Eureka.

Gateway reemplaza Kubernetes.

Todo debe pasar por filtros custom de JWT.

StripPrefix siempre se usa.

Si el gateway valida JWT, los microservicios ya no necesitan seguridad.

## 24. Fuentes Utiles

- Spring Cloud Gateway official docs: https://docs.spring.io/spring-cloud-gateway/reference/index.html
- Kubernetes Services: https://kubernetes.io/docs/concepts/services-networking/service/
- Kubernetes Gateway API: https://gateway-api.sigs.k8s.io/docs/introduction/
