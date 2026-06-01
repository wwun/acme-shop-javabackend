# Service Discovery Enterprise

Este documento es la version production-level para un sistema nuevo, como el sistema bancario.

No esta centrado en Eureka. Esta centrado en el problema real:

```text
Como un servicio encuentra a otro de forma confiable en produccion.
```

## 1. Decision Principal

La pregunta no es:

```text
Uso Eureka o no?
```

La pregunta correcta es:

```text
En que plataforma voy a desplegar?
Como se descubren servicios ahi?
Como se balancean llamadas?
Como se protegen fallos de red?
Como se observa el trafico?
```

## 2. Recomendacion Para Un Sistema Bancario Nuevo

Si el sistema nuevo corre en Kubernetes:

```text
No empezaria con Eureka.
Usaria Kubernetes Services + DNS interno.
```

Arquitectura recomendada:

```text
Client / Frontend
  -> API Gateway / Ingress
  -> Kubernetes Service
  -> Pod del microservicio
  -> llamada interna a otro Kubernetes Service
```

Ejemplo:

```text
payment-service
  -> account-service
```

La llamada interna puede resolverse por DNS:

```text
http://account-service
```

o con namespace:

```text
http://account-service.banking.svc.cluster.local
```

## 3. Componentes

### Kubernetes Service

Representa un nombre estable para un grupo de pods.

```text
Pods cambian.
IPs cambian.
Service DNS permanece estable.
```

Ejemplo:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: account-service
spec:
  selector:
    app: account-service
  ports:
    - port: 80
      targetPort: 8080
```

### DNS Interno

Kubernetes crea DNS para servicios:

```text
account-service
account-service.namespace
account-service.namespace.svc.cluster.local
```

### API Gateway / Ingress

Entrada externa.

Responsabilidades:

```text
routing externo
TLS
autenticacion inicial
rate limiting
CORS
request size limits
observabilidad edge
```

### Service Mesh

Opcional, no siempre necesario.

Ejemplos:

```text
Istio
Linkerd
Consul
```

Puede agregar:

```text
mTLS entre servicios
traffic splitting
retries/timeouts declarativos
circuit breaking
observabilidad de red
policy
```

Pero tambien agrega complejidad operativa.

## 4. Cliente HTTP No Es Discovery

Feign, RestClient y WebClient no reemplazan service discovery.

Son clientes HTTP.

### Feign

Bueno para:

```text
contratos HTTP declarativos
Spring Cloud
interfaces claras
```

### RestClient

Bueno para:

```text
Spring moderno
llamadas sincronas
menos magia que Feign
```

### WebClient

Bueno para:

```text
reactivo
alta concurrencia IO-bound
streaming
backpressure
```

Service discovery responde:

```text
A donde llamo?
```

Cliente HTTP responde:

```text
Como construyo y ejecuto la request?
```

## 5. Patron Recomendado Para Banking

Para un sistema bancario nuevo:

```text
Kubernetes Service DNS para discovery
API Gateway/Ingress para entrada externa
RestClient o Feign para llamadas sincronas internas
Kafka/eventos para procesos asincronos
timeouts estrictos
retries solo si son seguros
circuit breaker
observabilidad completa
security con OAuth2 Resource Server
```

Ejemplo:

```text
payment-service recibe solicitud de pago
  -> valida idempotency key
  -> guarda intento en DB
  -> llama account-service o ledger-service
  -> publica evento PaymentAuthorized/PaymentFailed
```

No depende de Eureka.

Depende de:

```text
DNS interno
contratos claros
resiliencia
transacciones
observabilidad
idempotencia
seguridad
```

## 6. Cuando Si Usaria Eureka

Eureka puede tener sentido si:

```text
el sistema usa Spring Cloud clasico
corre en VMs
corre en Docker Compose o ECS sin discovery equivalente
ya existe Eureka en la empresa
se necesita compatibilidad con servicios legacy
```

No lo presentaria como primera opcion si:

```text
la plataforma sera Kubernetes
ya existe service mesh
ya existe cloud service discovery
la organizacion usa DNS interno + gateway
```

## 7. Produccion No Es Solo Discovery

Aunque tengas discovery perfecto, las llamadas remotas fallan.

Necesitas:

```text
timeouts
retries con backoff
circuit breaker
bulkheads si aplica
rate limiting
correlation id
distributed tracing
structured logs
metrics por dependencia
dashboards
alerts
contract tests
versionado de APIs
fallbacks si aplica
```

Pregunta senior:

```text
Que pasa si account-service esta lento?
Que pasa si responde 500?
Que pasa si timeout ocurre despues de ejecutar la operacion?
Que pasa si el cliente reintenta?
Como evito doble cargo?
```

## 8. Paso a Paso Enterprise

### Paso 1. Definir plataforma

```text
Kubernetes?
VMs?
ECS?
Cloud Run?
On-prem?
```

Si es Kubernetes:

```text
usar Services/DNS
```

Si son VMs/Spring Cloud legacy:

```text
evaluar Eureka o Consul
```

### Paso 2. Definir entrada externa

```text
API Gateway
Ingress Controller
WAF
TLS termination
OAuth2/OIDC integration
```

### Paso 3. Definir comunicacion interna

Sincronica:

```text
RestClient / Feign / WebClient
```

Asincronica:

```text
Kafka
outbox pattern
event-driven workflows
```

### Paso 4. Definir nombres internos

Ejemplo Kubernetes:

```text
account-service
payment-service
ledger-service
notification-service
```

### Paso 5. Configurar timeouts

No dejar defaults infinitos.

Ejemplo conceptual:

```text
connect timeout: 1s
read timeout: 2s
overall request timeout: 3s
```

Depende del caso de uso.

### Paso 6. Configurar resiliencia

```text
circuit breaker por dependencia
retry solo para operaciones idempotentes
bulkhead para aislar dependencias lentas
fallback solo si no rompe consistencia
```

### Paso 7. Configurar observabilidad

```text
logs estructurados
correlation id
trace id
metrics HTTP client
latencia p95/p99
error rate por downstream
dashboards
alerts
```

### Paso 8. Probar fallos

```text
downstream caido
downstream lento
timeout
respuesta 500
respuesta parcial
retry duplicado
red inestable
deploy rolling
```

## 9. Comparacion Rapida

| Opcion | Uso recomendado |
| --- | --- |
| Eureka | Spring Cloud clasico, VMs, Compose, legacy |
| Kubernetes Service DNS | Microservicios modernos en Kubernetes |
| Consul | Service discovery cross-platform, VMs, service mesh |
| Istio/Linkerd | mTLS, traffic policy, observabilidad de red |
| Cloud Map / cloud discovery | AWS/cloud-native sin Kubernetes |
| DNS interno simple | Sistemas pequenos o platform-managed |

## 10. Que Diria En Entrevista

Version corta:

> For a new banking microservice platform, I would not start with Eureka unless the runtime platform required it. If we deploy on Kubernetes, I would use Kubernetes Services and DNS for service discovery, with API Gateway/Ingress at the edge and HTTP clients like RestClient, Feign or WebClient for internal calls.

Version senior:

> I separate service discovery from HTTP client choice. Discovery answers where a service lives; Feign, RestClient or WebClient decide how to call it. In a cloud-native banking system, I would usually rely on Kubernetes Services/DNS or a service mesh instead of Eureka. Then I would focus on production concerns: strict timeouts, safe retries, circuit breakers, idempotency, observability, security propagation and contract testing.

## 11. Que No Diria

No diria:

```text
Eureka es obligatorio para microservicios.
WebClient reemplaza Eureka.
Feign reemplaza Eureka.
Kubernetes necesita Eureka para descubrir servicios.
Service discovery resuelve resiliencia.
```

Diria:

```text
Eureka es una implementacion de service discovery.
Kubernetes ya trae discovery con Services/DNS.
Feign/RestClient/WebClient son clientes HTTP.
Resiliencia y observabilidad son responsabilidades adicionales.
```
