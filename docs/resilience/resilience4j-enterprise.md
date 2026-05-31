# Resilience Enterprise

Este documento es la version recomendada para pensar resiliencia en un sistema bancario o enterprise.

No se trata de "poner `@Retry` y `@CircuitBreaker` en todo". Se trata de definir politicas de falla por tipo de operacion.

## 1. Principio Principal

En sistemas distribuidos, toda llamada remota puede fallar.

Por eso, una llamada a otro servicio debe tener una politica explicita:

```text
timeout
retry?
circuit breaker?
fallback?
bulkhead?
rate limit?
metricas?
alertas?
```

La pregunta senior no es:

```text
Uso Resilience4j?
```

La pregunta correcta es:

```text
Que comportamiento debe tener el sistema cuando esta dependencia falla?
```

## 2. Resiliencia No Es Solo Disponibilidad

En banking, resiliencia tambien significa:

- no duplicar pagos
- no perder eventos
- no corromper saldos
- no aprobar operaciones sin confirmacion
- no ocultar fallas criticas
- no generar retry storms
- degradar sin mentir
- mantener auditabilidad

Una respuesta disponible pero incorrecta puede ser peor que un error controlado.

Ejemplo:

```text
payment provider no responde
```

Mala respuesta:

```text
asumir pago aprobado
```

Mejor respuesta:

```text
marcar pago como PENDING
confirmar asincronicamente
usar idempotency key
emitir evento/auditoria
```

## 3. Patrones Y Cuando Usarlos

### Timeout

Debe existir en casi toda llamada remota.

Sin timeout, puedes agotar threads/conexiones esperando una respuesta que ya no sirve.

Ejemplo:

```text
connect timeout: 500ms - 1s
read timeout: 1s - 3s segun endpoint
overall timeout: controlado por operacion
```

### Retry

Usar solo cuando:

- la falla es transitoria
- la operacion es idempotente
- el downstream puede soportarlo
- hay backoff y jitter
- hay limite maximo

No usar retry agresivo para:

- pagos sin idempotencia
- transferencias
- operaciones que crean efectos secundarios
- servicios ya saturados

### Circuit Breaker

Usar cuando una dependencia puede fallar repetidamente y quieres evitar cascadas.

Sirve para:

- fallar rapido
- proteger recursos
- dar tiempo de recuperacion
- emitir metricas claras

No reemplaza timeout.

Normalmente se usan juntos.

### Bulkhead

Usar cuando quieres aislar recursos.

Ejemplo:

```text
fraud-service lento no debe consumir todos los threads de payment-service
```

### Rate Limiter

Usar para proteger:

- endpoints publicos
- login
- password reset
- APIs caras
- providers con cuota

En un cluster con multiples instancias, rate limiting local puede no ser suficiente.

Para limites globales puedes necesitar:

- Redis
- API Gateway
- service mesh/policy engine
- provider administrado

### Fallback

Fallback no significa "inventar success".

Tipos:

- fail-fast controlado
- cache si es seguro
- respuesta parcial
- modo degradado
- encolar para procesar despues
- estado `PENDING`

## 4. Matriz Por Tipo De Operacion

| Operacion | Retry | Fallback | Comentario |
| --- | --- | --- | --- |
| `GET /catalog` | Si, corto | cache/stale si aplica | Read-heavy, degradable |
| `GET /account/balance` | Con cuidado | tal vez error controlado | Dato sensible/preciso |
| `POST /payments` | Solo con idempotency key | `PENDING` o error controlado | Nunca duplicar cargo |
| `POST /transfers` | Solo con idempotency + ledger seguro | `PENDING` | Consistencia primero |
| `POST /login` | Muy limitado | fail closed | Seguridad primero |
| `POST /notifications` | Si/async | cola/evento | Puede ser eventual |
| `GET /recommendations` | Si | respuesta vacia/cache | No critico |

## 5. Banking Payment Flow

Para pagos, la resiliencia no deberia depender solo de retries HTTP.

Patron recomendado:

```text
Cliente
  -> POST /payments con Idempotency-Key
  -> payment-service crea intento de pago PENDING
  -> outbox event PaymentRequested
  -> worker/procesador llama provider/bank
  -> resultado PaymentSucceeded/PaymentFailed
  -> ledger/audit se actualiza
```

Si el provider no responde:

```text
payment queda PENDING
se reintenta con politica controlada
idempotency evita duplicados
auditoria registra intentos
```

Esto es mucho mas seguro que:

```text
controller hace POST directo al provider y retry automatico sin estado
```

## 6. Donde Poner Resilience

Capas posibles:

```text
API Gateway
Service-to-service clients
Workers/event consumers
Database clients
External provider clients
```

### API Gateway

Puede aplicar:

- timeout
- rate limit
- circuit breaker por ruta
- fallback generico

Pero no deberia decidir reglas de negocio.

### Client Adapter / Gateway Service

Muy buena ubicacion.

Ejemplo:

```text
payment-service
  -> FraudGatewayService
  -> FraudClient
```

Alli pones:

- circuit breaker
- retry
- timeout
- mapping de errores
- metricas

### Event Consumers

Tambien necesitan resiliencia.

Ejemplo:

```text
PaymentRequestedConsumer
  -> bank-provider
```

Necesita:

- retry con backoff
- DLQ
- idempotencia
- circuit breaker si llama HTTP
- no perder mensajes

## 7. Resilience4j vs Kubernetes vs Service Mesh

No son lo mismo.

Kubernetes:

- reinicia pods
- readiness/liveness probes
- Services/DNS
- scaling

Service Mesh:

- mTLS
- retries/timeouts a nivel red
- traffic splitting
- observability

Resilience4j:

- politicas dentro de la aplicacion
- entiende excepciones de negocio/HTTP
- fallbacks en codigo
- circuit breaker por metodo/cliente

En enterprise pueden convivir.

Frase senior:

> Kubernetes keeps workloads running; Resilience4j defines how my application behaves when a dependency is slow or failing.

## 8. Configuracion Recomendada

Principios:

- configurar por dependencia, no un default ciego para todo
- timeouts siempre
- retry limitado
- circuit breaker con metricas
- fallback por caso de negocio
- alertas por circuit open y fallback rate

Ejemplo:

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 20
        minimum-number-of-calls: 10
        failure-rate-threshold: 50
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 2s
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 5
    instances:
      fraudProvider:
        base-config: default
      productCatalog:
        base-config: default

  retry:
    instances:
      productCatalog:
        max-attempts: 3
        wait-duration: 200ms
      paymentProvider:
        max-attempts: 1
```

Para pagos, preferiria retries controlados en worker con estado e idempotencia, no retry ciego en request sin persistencia.

## 9. Observability

Resiliencia sin observability es medio ciega.

Medir:

- circuit breaker state
- failure rate
- slow call rate
- fallback count
- retry attempts
- timeout count
- latency
- downstream error rate
- rejected calls
- bulkhead saturation

Alertas:

- circuit open por mucho tiempo
- fallback rate alto
- retries subiendo
- timeouts subiendo
- latencia p95/p99 alta

## 10. Errores Comunes

### Retry storm

Un servicio falla y todos los clientes empiezan a reintentar.

Resultado:

```text
mas trafico contra un sistema ya caido
```

Mitigacion:

- backoff
- jitter
- circuit breaker
- limites
- bulkhead

### Fallback que miente

Ejemplo:

```text
si inventory falla, devolver stock=999
```

Eso puede vender productos que no existen.

### Circuit breaker sin timeout

Si las llamadas quedan colgadas, tardas demasiado en detectar falla.

### Retry en operaciones no idempotentes

Puede duplicar efectos.

### Config unica para todo

`products`, `payments`, `fraud`, `ledger` no tienen el mismo riesgo.

## 11. Paso a Paso Enterprise Para Banking

### Paso 1. Clasificar dependencias

Ejemplo:

```text
payment-service -> ledger-service
payment-service -> fraud-service
payment-service -> bank-provider
payment-service -> notification-service
```

Clasificar:

- critica/sensible
- lectura/degradacion posible
- escritura con efectos
- externa/interna
- idempotente/no idempotente

### Paso 2. Definir politica por dependencia

Ejemplo:

```text
fraud-service:
  timeout corto
  circuit breaker
  fallback = payment review/manual/PENDING

notification-service:
  async event
  retry worker
  DLQ

bank-provider:
  idempotency
  persistent attempt
  retry controlled worker
  no duplicate charge
```

### Paso 3. Implementar adapter

```java
@Service
public class FraudGatewayService {
    private final FraudClient fraudClient;

    @CircuitBreaker(name = "fraud", fallbackMethod = "fallback")
    @Retry(name = "fraud")
    public FraudDecision evaluate(FraudRequest request) {
        return fraudClient.evaluate(request);
    }

    private FraudDecision fallback(FraudRequest request, Throwable ex) {
        return FraudDecision.reviewRequired();
    }
}
```

### Paso 4. Configurar YAML por instancia

```yaml
resilience4j:
  circuitbreaker:
    instances:
      fraud:
        sliding-window-size: 20
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
  retry:
    instances:
      fraud:
        max-attempts: 2
        wait-duration: 200ms
```

### Paso 5. Agregar metricas

Exponer:

```text
/actuator/prometheus
```

Monitorear en Prometheus/Grafana.

### Paso 6. Probar fallas

Probar:

- dependencia lenta
- dependencia caida
- 500
- 429
- timeout
- recuperacion
- circuit open
- fallback
- retries
- no duplicacion

### Paso 7. Documentar decisiones

Para cada dependencia:

```text
por que hay retry
por que no hay retry
que hace fallback
que metricas importan
que pasa si falla durante 5 minutos
que pasa si falla durante 1 hora
```

## 12. Respuesta Para Entrevista

Version corta:

> I use Resilience4j to protect remote calls with timeouts, circuit breakers, retries and fallbacks. I choose the policy based on the business operation, not as a generic annotation everywhere.

Version banking:

> For banking systems, resilience must preserve correctness. I would not blindly retry payments or transfers. I would use idempotency keys, persistent payment attempts, outbox/event-driven processing and controlled retries. Circuit breakers and timeouts protect the caller, while observability tells us when downstream dependencies are unhealthy.

Version senior:

> My resilience strategy depends on the dependency and operation semantics. Read-only catalog calls can use retries and cached fallbacks. Payment and ledger operations require idempotency, durable state and controlled recovery. I combine application-level Resilience4j with platform-level Kubernetes probes, autoscaling, API gateway rate limits and observability.

## 13. Que No Diria

No diria:

Retry siempre mejora el sistema.

Circuit breaker hace que el servicio no falle.

Fallback debe devolver cualquier cosa para que el usuario no vea error.

Kubernetes reemplaza Resilience4j.

Service mesh elimina la necesidad de pensar fallbacks.

Puedo reintentar pagos sin idempotencia.

## 14. Fuentes Utiles

- Resilience4j official docs: https://resilience4j.readme.io/docs/getting-started
- Spring Cloud CircuitBreaker docs: https://docs.spring.io/spring-cloud-circuitbreaker/docs/current/reference/html/
