# Eureka en Acme Shop

Este documento consolida las notas de Eureka y las alinea con la implementacion real de Acme Shop.

Eureka pertenece al tema de service discovery: como un servicio encuentra a otro sin conocer su host y puerto exactos.

## 1. Idea Principal

En microservicios, las direcciones cambian:

```text
product-service puede correr en:
  localhost:8077
  product-service:8001
  10.0.1.15:8001
  varias instancias al mismo tiempo
```

No queremos que `order-service`, `cart-service`, `catalog-query-service` o el gateway tengan URLs hardcodeadas para cada instancia.

Eureka resuelve eso con un registro central:

```text
Servicio arranca
  -> se registra en Eureka

Otro servicio necesita llamarlo
  -> pregunta por el nombre logico
  -> obtiene una instancia disponible
```

Ejemplo:

```text
catalog-query-service -> msvc-products
Eureka sabe donde vive msvc-products
Spring Cloud LoadBalancer elige una instancia
Feign hace la llamada HTTP real
```

## 2. Conceptos Clave

### Eureka Server

Es el directorio de servicios.

Guarda informacion como:

```text
nombre del servicio
host
puerto
estado
metadata
instancias disponibles
```

En Acme:

[EurekaServerApplication.java](/home/william/Documents/portfolio/acme-shop/eureka-server/src/main/java/com/wwun/acme/eureka_server/EurekaServerApplication.java)

```java
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {
}
```

### Eureka Client

Es cualquier microservicio que se registra en Eureka.

Ejemplos en Acme:

```text
msvc-products
msvc-orders
msvc-inventories
msvc-carts
msvc-users
msvc-auth
msvc-catalogs
msvc-gateway
```

### Heartbeat

Cada cliente manda latidos periodicos a Eureka.

Si deja de mandar heartbeats, Eureka puede marcarlo como no disponible.

Esto no significa que Eureka "arregla" el servicio. Solo actualiza el registro para que otros no lo usen si esta caido.

### Service Name

El nombre logico del servicio viene de:

```yaml
spring:
  application:
    name: msvc-products
```

Ese nombre es el que usan Feign y Gateway:

```java
@FeignClient(name = "msvc-products")
```

```yaml
uri: lb://msvc-products
```

## 3. Eureka en Acme Shop

Flujo general:

```text
config-server arranca
eureka-server arranca
microservicios arrancan y se registran
api-gateway arranca y usa discovery
clientes llaman gateway
gateway resuelve servicios por lb://
servicios llaman otros servicios con Feign usando nombres Eureka
```

Ejemplo real:

```text
Bruno
  -> localhost:8090/api/catalogs
  -> api-gateway-server
  -> lb://msvc-catalogs
  -> catalog-query-service
  -> Feign msvc-products
  -> Feign msvc-inventories
```

Eureka participa en:

```text
gateway -> catalog
catalog -> product
catalog -> inventory
order -> product
cart -> product
auth -> user
```

## 4. Configuracion del Eureka Server

Dependencia:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

Clase principal:

```java
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

Configuracion tipica:

```yaml
spring:
  application:
    name: eureka-server

server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

### `register-with-eureka=false`

Significa:

```text
El Eureka Server no se registra a si mismo como si fuera otro microservicio.
```

### `fetch-registry=false`

Significa:

```text
El Eureka Server no necesita descargar el registro de otro Eureka Server.
```

En un setup local simple, Eureka es el centro del registro.

## 5. Configuracion de un Cliente Eureka

Dependencia:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

Configuracion local:

```yaml
spring:
  application:
    name: msvc-products

server:
  port: 8001

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
```

Configuracion Docker:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka
```

La diferencia importante:

```text
Local:
  localhost:8761

Docker Compose:
  eureka-server:8761
```

Dentro de Docker, `localhost` apunta al propio contenedor, no al contenedor de Eureka.

## 6. `defaultZone` y `/eureka`

La URL:

```text
http://eureka-server:8761/eureka
```

es el endpoint donde los clientes registran y consultan servicios.

Internamente, el registro usa rutas como:

```text
/eureka/apps/{applicationName}
```

Por eso los clientes configuran el path `/eureka`.

## 7. Feign + Eureka

Feign y Eureka resuelven problemas distintos.

Eureka:

```text
Dice donde esta un servicio.
```

Feign:

```text
Construye y ejecuta la request HTTP.
```

Ejemplo:

```java
@FeignClient(name = "msvc-products")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductResponseDTO getById(@PathVariable("id") UUID id);
}
```

Flujo:

```text
ProductClient.getById(id)
  -> Feign ve name = msvc-products
  -> Spring Cloud LoadBalancer pregunta por instancias
  -> Eureka devuelve instancias registradas
  -> LoadBalancer elige una
  -> Feign envia HTTP GET /api/products/{id}
```

Nota importante:

```text
Eureka no reemplaza Feign.
Feign no reemplaza Eureka.
```

Puedes usar:

```text
Feign + Eureka
RestClient + Eureka/LoadBalancer
WebClient + Eureka/LoadBalancer
Feign con URL fija
Feign con Kubernetes DNS
```

## 8. Gateway + Eureka

El API Gateway tambien puede usar Eureka.

Ruta tipica:

```yaml
- id: msvc-products
  uri: lb://msvc-products
  predicates:
    - Path=/api/products/**
```

`lb://` significa:

```text
Usa Spring Cloud LoadBalancer.
Busca el servicio por nombre logico.
No uses una URL fija.
```

Flujo:

```text
Cliente externo
  -> api-gateway-server
  -> ruta Path=/api/products/**
  -> lb://msvc-products
  -> instancia real de product-service
```

Respuesta senior:

> The gateway is the external entry point. Eureka helps the gateway resolve logical service names into live instances. Internal services can also use Eureka through Feign or another HTTP client.

## 9. Postman/Bruno No Habla Con Eureka

Esto es clave.

Sin gateway:

```text
Bruno/Postman debe llamar directamente al puerto real del servicio.
```

Ejemplo:

```text
http://localhost:8077/api/products
```

Con gateway:

```text
Bruno/Postman llama al gateway.
```

Ejemplo:

```text
http://localhost:8090/api/products
```

Eureka sirve para que:

```text
gateway encuentre servicios
servicios encuentren otros servicios
```

No para que el navegador o Bruno llamen a:

```text
http://localhost:8761/msvc-products/api/products
```

Eso no es el rol de Eureka.

## 10. Multiples Instancias

Una razon para usar Eureka es poder tener varias instancias del mismo servicio.

Ejemplo:

```text
msvc-products:
  instancia 1 -> product-service:8001
  instancia 2 -> product-service-2:8001
  instancia 3 -> product-service-3:8001
```

Todas se registran bajo:

```text
MSVC-PRODUCTS
```

Spring Cloud LoadBalancer puede elegir una instancia.

Configuracion local para puertos dinamicos:

```yaml
server:
  port: ${PORT:0}

eureka:
  instance:
    instance-id: ${spring.application.name}:${random.value}
```

En Docker Compose normalmente usas servicios/replicas o contenedores separados. En Kubernetes, esto se maneja con pods y Services.

## 11. `@EnableEurekaClient` Ya No Suele Ser Necesario

En versiones modernas de Spring Cloud, normalmente basta con:

```text
spring-cloud-starter-netflix-eureka-client
```

La auto-configuracion registra el cliente.

`@EnableDiscoveryClient` puede aparecer, como en el gateway de Acme:

[ApiGatewayServerApplication.java](/home/william/Documents/portfolio/acme-shop/api-gateway-server/src/main/java/com/wwun/acme/api_gateway_server/ApiGatewayServerApplication.java)

```java
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayServerApplication {
}
```

Pero en muchos proyectos modernos no hace falta ponerlo explicitamente si la dependencia y configuracion estan bien.

## 12. Eureka en Docker Compose

En Acme:

[docker-compose.yml](/home/william/Documents/portfolio/acme-shop/docker-compose.yml)

```yaml
eureka-server:
  ports:
    - "8761:8761"
  depends_on:
    config-server:
      condition: service_healthy
  healthcheck:
    test: ["CMD-SHELL", "wget -qO- http://localhost:8761/actuator/health | grep -q '\"status\"\\s*:\\s*\"UP\"'"]
```

Los servicios dependen de Eureka:

```yaml
product-service:
  depends_on:
    eureka-server:
      condition: service_healthy
    config-server:
      condition: service_healthy
```

Esto ayuda al arranque local.

Importante:

```text
depends_on no garantiza que el servicio destino este funcional a nivel de negocio.
Solo controla orden/health inicial.
```

Por eso tambien se usan:

```text
timeouts
retries
circuit breakers
health checks
observabilidad
```

## 13. Como Probar Eureka en Acme

1. Levantar servicios:

```bash
docker compose up -d
```

2. Abrir dashboard:

```text
http://localhost:8761
```

3. Verificar que aparezcan aplicaciones:

```text
MSVC-PRODUCTS
MSVC-ORDERS
MSVC-INVENTORIES
MSVC-CATALOGS
MSVC-GATEWAY
```

4. Probar via gateway:

```text
http://localhost:8090/api/products
```

5. Si gateway falla, probar directo:

```text
http://localhost:8077/api/products
```

6. Ver logs:

```bash
docker compose logs --tail=100 eureka-server
docker compose logs --tail=100 api-gateway-server
docker compose logs --tail=100 product-service
```

## 14. Como Leer Errores

### Servicio no aparece en Eureka

Posibles causas:

```text
spring.application.name mal configurado
defaultZone incorrecto
Eureka no esta arriba
servicio no tiene dependencia eureka-client
servicio falla antes de registrarse
Docker usa localhost en vez de eureka-server
```

### Gateway devuelve 404

Puede significar:

```text
La ruta no existe.
El path no coincide.
La config del gateway no se cargo.
El servicio no esta registrado aun.
```

### Gateway devuelve 503

Puede significar:

```text
La ruta existe, pero no hay instancias disponibles para lb://nombre-servicio.
```

### Gateway devuelve 401

Eso no es Eureka.

Significa:

```text
La ruta probablemente matcheo, pero seguridad bloqueo por token ausente/invalido.
```

## 15. Lo Academico vs Produccion

### Academico / curso

Eureka es muy comun en cursos de Spring Cloud porque muestra claramente:

```text
service discovery
registro dinamico
Feign con nombres logicos
gateway con lb://
varias instancias locales
```

Es excelente para aprender microservicios.

### Produccion real

En produccion moderna, depende del entorno.

En Docker Compose o VM-based deployments:

```text
Eureka puede tener sentido.
```

En Kubernetes:

```text
normalmente no se usa Eureka.
Kubernetes ya trae service discovery con DNS interno.
```

Ejemplo Kubernetes:

```text
http://product-service.default.svc.cluster.local
```

o simplemente:

```text
http://product-service
```

si estas en el mismo namespace.

En empresas grandes tambien puedes ver:

```text
Kubernetes Service Discovery
service mesh: Istio, Linkerd, Consul
API Gateway / ingress controller
cloud load balancers
DNS interno
```

Respuesta senior:

> Eureka is useful for Spring Cloud service discovery, especially in VM or Docker Compose style deployments. In Kubernetes, I would usually rely on Kubernetes Services and DNS, possibly with a service mesh, rather than adding Eureka. The important concept is service discovery; Eureka is one implementation.

## 16. Eureka vs Kubernetes DNS

Eureka:

```text
La aplicacion se registra como cliente.
Los servicios consultan un registry.
Spring Cloud integra Feign/Gateway con ese registry.
```

Kubernetes:

```text
Los pods no necesitan registrarse en Eureka.
Kubernetes mantiene Services y Endpoints.
DNS interno resuelve nombres.
```

Comparacion:

| Tema | Eureka | Kubernetes |
| --- | --- | --- |
| Discovery | Eureka registry | Kubernetes Service DNS |
| Registro | App cliente se registra | Kubernetes controla pods/endpoints |
| Load balancing | Spring Cloud LoadBalancer | kube-proxy/service mesh/load balancer |
| Uso tipico | Spring Cloud classic, VMs, Compose | Cloud-native moderno |
| Complejidad extra | Requiere server Eureka | Ya viene con el cluster |

## 17. Mejores Practicas

Para Acme/local:

```text
1. Usar nombres claros: msvc-products, msvc-orders.
2. No hardcodear URLs internas si ya usas Eureka.
3. Usar `lb://service-name` en gateway.
4. Usar `@FeignClient(name = "service-name")`.
5. No usar localhost dentro de Docker para otros servicios.
6. Revisar dashboard Eureka.
7. Revisar actuator health.
8. Tener timeouts/circuit breakers aunque Eureka funcione.
```

Para produccion:

```text
1. No asumir que Eureka es obligatorio.
2. Si estas en Kubernetes, preferir Services/DNS.
3. Si usas Eureka, desplegarlo de forma altamente disponible.
4. Monitorear registro, heartbeats y servicios sin instancias.
5. Tener retries/timeouts/circuit breakers.
6. No tratar service discovery como resiliencia completa.
7. Versionar contratos entre servicios.
```

## 18. Checklist Para Agregar Un Servicio Con Eureka

```text
1. Agregar dependencia eureka-client.
2. Definir spring.application.name.
3. Configurar eureka.client.service-url.defaultZone.
4. En Docker, usar eureka-server:8761/eureka.
5. Exponer actuator health.
6. Levantar eureka-server antes del servicio.
7. Verificar dashboard.
8. Si otro servicio lo consume, usar @FeignClient(name = "...").
9. Si el gateway lo expone, agregar ruta lb://...
10. Probar directo y por gateway.
```

## 19. Paso a Paso Para Implementar Eureka

Este paso a paso aplica al estilo Acme/Spring Cloud clasico.

### Paso 1. Crear `eureka-server`

Dependencia:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

Clase principal:

```java
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

Configuracion:

```yaml
spring:
  application:
    name: eureka-server

server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

### Paso 2. Configurar cada microservicio como cliente

Dependencia:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

Configuracion local:

```yaml
spring:
  application:
    name: msvc-products

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
```

Configuracion Docker:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka
```

### Paso 3. Usar nombres logicos en Feign

```java
@FeignClient(name = "msvc-products")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductResponseDTO getById(@PathVariable("id") UUID id);
}
```

No usar:

```java
@FeignClient(name = "products", url = "http://localhost:8077")
```

si el servicio ya esta registrado en Eureka.

### Paso 4. Usar `lb://` en Gateway

```yaml
- id: msvc-products
  uri: lb://msvc-products
  predicates:
    - Path=/api/products/**
```

### Paso 5. Levantar en orden

```text
1. config-server
2. eureka-server
3. microservicios
4. api-gateway
```

En Docker Compose esto se modela con:

```yaml
depends_on:
  eureka-server:
    condition: service_healthy
```

### Paso 6. Verificar

Dashboard:

```text
http://localhost:8761
```

Actuator:

```bash
curl http://localhost:8761/actuator/health
```

Gateway:

```bash
curl http://localhost:8090/api/products
```

### Paso 7. Diagnosticar

```text
Servicio no aparece en Eureka:
  revisar application.name, defaultZone, dependencia eureka-client y logs.

Gateway 404:
  ruta no matchea o config no cargada.

Gateway 503:
  ruta existe, pero no hay instancia disponible para lb://service.

Gateway 401:
  no es Eureka; es seguridad.
```

## 20. Aclaracion Importante Para Produccion

Eureka no es "malo", pero tampoco es automaticamente la opcion moderna para todo.

En un proyecto nuevo tipo banking enterprise, si el despliegue sera en Kubernetes, normalmente no propondria Eureka como primera opcion.

Propuesta mas moderna:

```text
Kubernetes Service DNS
API Gateway / ingress
timeouts + retries controlados
circuit breakers
observabilidad
opcional: service mesh
```

WebClient, RestClient o Feign no reemplazan Eureka.

Ellos son clientes HTTP:

```text
Feign:
  cliente HTTP declarativo.

RestClient:
  cliente HTTP sincronico moderno de Spring.

WebClient:
  cliente HTTP reactivo.
```

Lo que reemplaza a Eureka es el mecanismo de discovery:

```text
Kubernetes Services/DNS
Consul
service mesh
cloud service discovery
DNS interno
```

Respuesta senior:

> I would not present Eureka as the default choice for a new cloud-native banking platform. I would first check the runtime platform. If we run on Kubernetes, I would use Kubernetes Services and DNS, with gateway/ingress and possibly service mesh. Eureka is still useful to understand service discovery and may appear in Spring Cloud legacy or VM-based systems, but it is not mandatory in modern Kubernetes deployments.

## 21. Version Corta Para Entrevista

> Eureka is a service discovery registry. Services register themselves with a logical name, and clients such as Spring Cloud Gateway or Feign resolve that name to a live instance through Spring Cloud LoadBalancer. In Acme Shop, the gateway uses `lb://msvc-products` and Feign uses `@FeignClient(name = "msvc-products")`, so services do not need hardcoded host and port values.

## 22. Version Senior

> I treat Eureka as one implementation of service discovery, not as the architecture itself. It is useful in Spring Cloud or VM-based deployments, but in Kubernetes I would usually use Kubernetes Services, DNS and possibly a service mesh. Regardless of the discovery mechanism, I still need timeouts, retries, circuit breakers, observability and contract management, because service discovery only tells me where a service is; it does not make remote calls reliable.

## 23. Que No Decir

No decir:

```text
Eureka hace que los servicios nunca fallen.
Eureka reemplaza Feign.
Eureka reemplaza API Gateway.
Postman llama a Eureka para llegar al servicio.
En produccion siempre se usa Eureka.
```

Mejor decir:

```text
Eureka resuelve discovery.
Feign/RestClient/WebClient hacen HTTP.
Gateway expone entrada externa.
Kubernetes puede reemplazar Eureka como mecanismo de discovery.
Resiliencia requiere patrones adicionales.
```
