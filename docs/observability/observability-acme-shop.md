# Observabilidad en Acme Shop

Este documento consolida las notas de Actuator, Micrometer, Prometheus y Grafana, y las alinea con la implementacion real de Acme Shop.

Observabilidad no es solo "ver si la app esta arriba". Es poder responder:

```text
que esta pasando?
donde esta fallando?
desde cuando?
a quien afecta?
que tan grave es?
como lo pruebo?
como lo alerto?
```

## 1. Idea Principal

En microservicios, no basta con que cada servicio tenga logs sueltos.

Necesitas señales:

```text
logs
metrics
traces
```

Y necesitas correlacionarlas.

En Acme Shop, el stack actual cubre principalmente metricas y health:

```text
Spring Boot Actuator
  -> Micrometer
  -> /actuator/prometheus
  -> Prometheus
  -> Grafana
```

## 2. Actuator

Spring Boot Actuator expone endpoints operativos.

Ejemplos:

```text
/actuator/health
/actuator/info
/actuator/metrics
/actuator/prometheus
/actuator/loggers
/actuator/env
/actuator/mappings
```

En Acme, se usa sobre todo:

```text
/actuator/health
/actuator/metrics
/actuator/prometheus
```

`/actuator/health` sirve para saber si la app y sus dependencias estan saludables.

`/actuator/metrics` lista metricas internas.

`/actuator/prometheus` expone metricas en formato que Prometheus puede scrapear.

## 3. Micrometer

Micrometer es la capa de instrumentacion de metricas en Spring Boot.

Piensa en Micrometer como:

```text
la API que usa tu aplicacion para crear metricas
```

Y Prometheus como:

```text
el sistema que recolecta y almacena esas metricas
```

Micrometer puede crear:

- counters
- timers
- gauges
- distribution summaries

## 4. Prometheus

Prometheus recolecta metricas con modelo pull.

Eso significa:

```text
Prometheus va a cada microservicio
lee /actuator/prometheus
guarda series temporales
```

No es el microservicio el que empuja metricas a Prometheus.

En Acme:

[prometheus.yml](/home/william/Documents/portfolio/acme-shop/prometheus.yml)

```yaml
global:
  scrape_interval: 5s

scrape_configs:
  - job_name: 'user-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['user-service:8001']
```

Dentro de Docker, Prometheus usa nombres de servicio:

```text
user-service:8001
product-service:8001
order-service:8001
```

No usa:

```text
localhost
```

porque `localhost` dentro del contenedor de Prometheus seria Prometheus mismo.

## 5. Grafana

Grafana es la capa visual.

Flujo:

```text
microservicios
  -> /actuator/prometheus
  -> Prometheus
  -> Grafana
  -> dashboards/alertas
```

Grafana no recolecta metricas directamente de los servicios en este flujo.

Grafana consulta Prometheus.

## 6. Configuracion En Los Microservicios

Ejemplo real:

[product-service/application.yml](/home/william/Documents/portfolio/acme-shop/product-service/src/main/resources/application.yml)

```yaml
management:
    endpoints:
        web:
            exposure:
                include: health, info, metrics, prometheus
    metrics:
        tags:
            application: ${spring.application.name}
    prometheus:
        metrics:
            export:
                enabled: true
    endpoint:
        health:
            show-details: always
        prometheus:
            enabled: true
```

Puntos importantes:

`include: health, info, metrics, prometheus`

Expone endpoints necesarios para monitoreo.

`application: ${spring.application.name}`

Agrega tag comun para identificar servicio en metricas.

`show-details: always`

Muestra detalle de health. Muy util en desarrollo, pero en produccion puede exponer informacion sensible si el endpoint es publico.

## 7. Dependencias

Cada microservicio observable necesita:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Actuator expone endpoints.

Micrometer Prometheus registry habilita:

```text
/actuator/prometheus
```

## 8. Seguridad De Actuator

En Acme, varios servicios permiten:

```java
.requestMatchers("/actuator/**").permitAll()
```

Esto facilita Prometheus local.

Pero en produccion no conviene exponer todo Actuator publicamente.

Mejor:

- exponer solo endpoints necesarios
- proteger con red interna
- proteger con auth
- usar puerto de management privado
- no exponer `/actuator/env`, `/actuator/beans`, `/actuator/heapdump` publicamente

Regla:

```text
health y prometheus para monitoreo interno
no todo actuator abierto a internet
```

## 9. Metricas Tecnicas

Spring Boot/Micrometer exponen automaticamente:

- `http.server.requests`
- JVM memory
- JVM GC
- CPU
- threads
- disk
- uptime
- datasource/Hikari si aplica
- cache si esta instrumentado

En Prometheus los nombres se normalizan:

```text
http_server_requests_seconds_count
jvm_memory_used_bytes
process_cpu_usage
```

Pero en `/actuator/metrics` usas el nombre Micrometer:

```text
/actuator/metrics/jvm.memory.used
/actuator/metrics/http.server.requests
```

## 10. Metricas Custom En Acme

Acme tiene metricas custom en varios servicios.

### Cart Metrics

Archivo:

[CartMetrics.java](/home/william/Documents/portfolio/acme-shop/cart-service/src/main/java/com/wwun/acme/cart/metric/CartMetrics.java)

Metricas:

```text
cart_commands_total{type="add"}
cart_commands_total{type="remove"}
cart_commands_total{type="setQty"}
cart_commands_total{type="clear"}
cart_queries_total{type="getCart"}
cart_queries_total{type="getSummary"}
cart_product_fallbacks_total
cart_query_latency_seconds{type="summary"}
```

Esto es bueno porque separa:

- comandos
- queries
- fallbacks
- latencia

### Order Metrics

Archivo:

[OrderMetrics.java](/home/william/Documents/portfolio/acme-shop/order-service/src/main/java/com/wwun/acme/order/metric/OrderMetrics.java)

```text
orders_created_total
```

Archivo:

[PerformanceAspect.java](/home/william/Documents/portfolio/acme-shop/order-service/src/main/java/com/wwun/acme/order/aspect/PerformanceAspect.java)

```text
order_service_execution_time{class="...", method="..."}
```

Mide tiempo de ejecucion de metodos en `OrderServiceImpl`.

### Product Cache Metrics

Archivo:

[CacheMetrics.java](/home/william/Documents/portfolio/acme-shop/product-service/src/main/java/com/wwun/acme/product/metric/CacheMetrics.java)

```text
cache_hits_total
cache_misses_total
```

Sirve para analizar efectividad de Redis/cache.

### Inventory Metrics

Archivo:

[InventoryMetrics.java](/home/william/Documents/portfolio/acme-shop/inventory-service/src/main/java/com/wwun/acme/inventory/metric/InventoryMetrics.java)

```text
inventory_reservations_total{result="success"}
inventory_reservations_total{result="failed"}
inventory_adjustments_total{type="..."}
```

Nota: Micrometer puede normalizar nombres con puntos a formato Prometheus con underscores.

## 11. Counters, Timers Y Gauges

### Counter

Solo sube.

Ejemplo:

```java
meterRegistry.counter("orders_created_total").increment();
```

Usos:

- ordenes creadas
- fallbacks
- errores
- cache hits/misses

### Timer

Mide duracion y cantidad.

Ejemplo:

```java
Timer.Sample sample = Timer.start(meterRegistry);
try {
    return joinPoint.proceed();
} finally {
    sample.stop(meterRegistry.timer("order_service_execution_time"));
}
```

Usos:

- latencia de servicio
- tiempo de procesamiento
- llamadas externas

### Gauge

Mide un valor que sube y baja.

Usos:

- cola pendiente
- outbox pending
- stock actual si fuera apropiado
- conexiones activas

No usar gauge para eventos que deberian contarse.

## 12. Bug Detectado En Prometheus

En `prometheus.yml`, el job de inventory aparece asi:

```yaml
- job_name: 'inventory-service'
  metrics_path: '/actuator/prometheus'
  static_configs:
    - targets: ['auth-service:8001']
```

Deberia apuntar a:

```yaml
- job_name: 'inventory-service'
  metrics_path: '/actuator/prometheus'
  static_configs:
    - targets: ['inventory-service:8001']
```

Esta es una leccion importante:

```text
si Prometheus scrapea el target equivocado, tus dashboards mienten.
```

Observabilidad mal configurada puede ser peor que no tenerla, porque genera falsa confianza.

## 13. Logs

Acme usa logs con SLF4J en varios servicios.

Logs sirven para responder:

```text
que paso en este request/evento?
que exception ocurrio?
que datos operativos ayudan a diagnosticar?
```

Pero en microservicios necesitas que los logs tengan contexto:

- service name
- timestamp
- level
- request id
- trace id
- user id si aplica y no viola privacidad
- order id/payment id si aplica

Pendiente para Acme:

- correlation id
- trace id en logs
- logs estructurados JSON
- centralizar logs con Loki/ELK/OpenSearch

## 14. Traces

Trazas distribuidas responden:

```text
por donde paso este request?
cuanto tardo en cada servicio?
que llamada fallo?
```

Ejemplo:

```text
api-gateway
  -> order-service
  -> product-service
  -> inventory-service
  -> kafka
```

Acme todavia no tiene tracing completo.

Para enterprise, agregaria:

- OpenTelemetry
- trace id/span id
- propagation HTTP y Kafka
- Jaeger/Tempo
- logs con trace id

## 15. Dashboards

Dashboards basicos:

Infra:

- JVM memory
- CPU
- GC
- threads
- uptime
- pod/container restart si aplica

HTTP:

- requests per second
- error rate
- latency p50/p95/p99
- status codes
- endpoint mas lento

Business:

- orders created
- inventory reservations success/failed
- cart commands
- cache hit/miss ratio
- outbox pending/failed
- kafka consumer lag si se agrega

## 16. Alertas

Alertas utiles:

- servicio caido: `up == 0`
- error rate alto
- latencia p95 alta
- memoria alta
- Redis down
- Kafka down
- DB down
- circuit breaker abierto
- fallbacks altos
- outbox pending creciendo
- stock reservations failed creciendo

No alertar por todo.

Alertar por sintomas que requieren accion.

## 17. Paso a Paso Para Implementar En Un Servicio

### Paso 1. Agregar dependencias

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Paso 2. Configurar endpoints

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  metrics:
    tags:
      application: ${spring.application.name}
  endpoint:
    health:
      show-details: always
```

### Paso 3. Permitir Prometheus internamente

En seguridad local:

```java
.requestMatchers("/actuator/**").permitAll()
```

En produccion:

```text
permitir solo desde red interna/monitoring
```

### Paso 4. Agregar target a Prometheus

```yaml
- job_name: 'my-service'
  metrics_path: '/actuator/prometheus'
  static_configs:
    - targets: ['my-service:8001']
```

### Paso 5. Crear metricas custom si aportan valor

```java
Counter.builder("payments_created_total")
    .tag("result", "success")
    .register(registry);
```

### Paso 6. Crear dashboards

Separar:

- infra
- HTTP
- negocio

### Paso 7. Crear alertas

Minimo:

- service down
- high error rate
- high latency
- dependency down

## 18. Lo Que Falta Para Production Level

Acme ya tiene:

- Actuator
- Micrometer
- Prometheus endpoint
- Prometheus
- Grafana
- metricas custom
- healthchecks Docker

Falta para nivel production fuerte:

- tracing distribuido
- correlation id
- logs centralizados
- structured JSON logs
- alertas versionadas
- dashboards versionados
- SLO/SLI
- error budgets
- kafka lag metrics
- outbox metrics completas
- security audit events

## 19. Version Corta Para Entrevista

> In Acme Shop I use Spring Boot Actuator and Micrometer to expose health and metrics from each microservice. Prometheus scrapes `/actuator/prometheus`, and Grafana visualizes JVM, HTTP and custom business metrics such as orders created, cart commands, cache hits and inventory reservations.

## 20. Version Senior

> I separate observability into metrics, logs and traces. Metrics tell me that something is wrong, logs explain what happened, and traces show where a request spent time across services. Acme currently covers metrics and health with Actuator, Micrometer, Prometheus and Grafana; the next production step would be OpenTelemetry tracing, correlation IDs, centralized structured logs and alerting based on SLOs.

## 21. Que No Decir

No diria:

Actuator es observabilidad completa.

Prometheus y Grafana son lo mismo.

Grafana recolecta metricas directamente.

Logs son suficientes para microservicios.

`include: *` esta bien en produccion publica.

Si el dashboard esta verde, el sistema esta sano.

Metricas custom siempre deben tener muchos labels.

## 22. Fuentes Utiles

- Spring Boot Actuator endpoints: https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
- Spring Boot Actuator metrics: https://docs.spring.io/spring-boot/reference/actuator/metrics.html
- Micrometer concepts: https://docs.micrometer.io/micrometer/reference/concepts.html
- OpenTelemetry docs: https://opentelemetry.io/docs/
