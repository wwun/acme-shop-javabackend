# Observability Enterprise

Este documento es la version recomendada para pensar observabilidad en un sistema bancario o enterprise.

No se trata de instalar Prometheus y Grafana solamente. Se trata de disenar el sistema para poder operarlo, diagnosticarlo y auditarlo.

## 1. Decision Principal

La pregunta no es:

```text
Tengo logs?
```

La pregunta correcta es:

```text
puedo detectar, diagnosticar y explicar una falla en minutos?
```

En sistemas bancarios, observabilidad ayuda a responder:

- se proceso el pago?
- donde se trabo?
- cuanto tardo?
- que dependencia fallo?
- afecto a un usuario o a todos?
- hay duplicados?
- hay perdida de eventos?
- hay degradacion antes de una caida?

## 2. Tres Pilares

### Metrics

Datos numericos agregados en el tiempo.

Ejemplos:

- request rate
- error rate
- latency p95/p99
- CPU/memoria
- JVM GC
- pagos creados
- pagos fallidos
- outbox pending
- kafka lag

Metricas responden:

```text
que tan grande es el problema?
desde cuando?
esta empeorando?
```

### Logs

Eventos detallados.

Ejemplos:

```text
payment failed because provider timeout
order created
stock reservation failed
JWT validation failed
```

Logs responden:

```text
que paso exactamente?
con que ids?
que exception?
```

### Traces

Camino completo de una request/transaccion entre servicios.

Ejemplo:

```text
api-gateway
  -> payment-service
  -> fraud-service
  -> account-service
  -> ledger-service
  -> kafka
```

Traces responden:

```text
donde se fue el tiempo?
que servicio fallo?
que llamada remota fue lenta?
```

## 3. Stack Recomendado Sin Cloud Caro

Para un sistema bancario local/portable:

```text
Spring Boot Actuator
Micrometer
Prometheus
Grafana
OpenTelemetry
Tempo o Jaeger
Loki o ELK/OpenSearch
```

Stack Grafana OSS:

```text
Prometheus -> metrics
Loki       -> logs
Tempo      -> traces
Grafana    -> dashboards/visualizacion
```

OpenTelemetry funciona como estandar para instrumentar y exportar telemetria.

## 4. Arquitectura Objetivo

```text
microservicios Spring Boot
  -> metrics: /actuator/prometheus
  -> logs: stdout JSON con trace_id
  -> traces: OpenTelemetry

Prometheus scrapea metrics
Loki/ELK recolecta logs
Tempo/Jaeger recibe traces
Grafana visualiza todo
Alertmanager/Grafana Alerting envia alertas
```

## 5. Actuator Y Micrometer

En Spring Boot:

- Actuator expone endpoints operativos
- Micrometer instrumenta metricas
- Prometheus registry expone `/actuator/prometheus`

Endpoints recomendados:

```text
/actuator/health
/actuator/info
/actuator/prometheus
```

No exponer publicamente:

```text
/actuator/env
/actuator/beans
/actuator/heapdump
/actuator/threaddump
```

En production, Actuator debe estar:

- en red interna
- protegido por auth
- o expuesto solo al sistema de monitoreo

## 6. SLIs, SLOs Y Alertas

No alertar por cualquier metrica.

Definir SLIs:

- disponibilidad
- latencia
- error rate
- throughput
- freshness de eventos
- consumer lag
- duracion de procesamiento

Definir SLOs:

```text
99.9% de requests de login responden < 500ms
99% de pagos se aceptan o rechazan en < 3s
0 pagos duplicados
outbox pending no debe superar X por mas de Y minutos
```

Alertas deben apuntar a sintomas accionables:

- high error rate
- high latency
- service down
- DB connection pool exhausted
- Kafka consumer lag alto
- outbox stuck
- payment provider failures
- circuit breaker open
- fraud-service timeout rate alto

## 7. Metricas De Negocio Bancario

Metricas tecnicas no bastan.

Para banking, agregaria:

Payments:

- `payments_created_total{status="..."}`
- `payments_completed_total`
- `payments_failed_total{reason="..."}`
- `payments_pending_current`
- `payment_provider_latency_seconds`
- `payment_idempotency_conflicts_total`

Ledger:

- `ledger_entries_created_total`
- `ledger_posting_failures_total`
- `ledger_reconciliation_mismatches_total`

Outbox/Kafka:

- `outbox_events_pending`
- `outbox_events_failed_total`
- `outbox_publish_latency_seconds`
- `kafka_consumer_lag`

Security:

- `auth_login_failures_total{reason="..."}`
- `jwt_validation_failures_total`
- `mfa_challenges_total`

Resilience:

- circuit breaker state
- retry attempts
- fallback count
- timeout count

## 8. Cardinalidad

Cardinalidad significa cuantos valores distintos tienen los labels/tags.

Malo:

```text
payments_total{user_id="123"}
payments_total{email="a@b.com"}
payments_total{payment_id="uuid"}
```

Eso explota la cantidad de series y puede matar Prometheus.

Bueno:

```text
payments_total{status="completed"}
payments_total{provider="internal"}
payments_total{currency="CAD"}
```

Regla:

```text
No usar IDs unicos como labels de metricas.
```

IDs van en logs/traces, no en metric labels.

## 9. Logs Enterprise

Logs deben ser estructurados.

Ejemplo JSON:

```json
{
  "timestamp": "2026-05-31T12:00:00Z",
  "level": "ERROR",
  "service": "payment-service",
  "trace_id": "abc",
  "span_id": "def",
  "payment_id": "pmt_123",
  "message": "Payment provider timeout"
}
```

Buenas practicas:

- no loggear secrets
- no loggear PAN/tarjeta completa
- enmascarar PII
- incluir trace id
- incluir ids de negocio cuando sea seguro
- logs a stdout
- centralizar en Loki/ELK/OpenSearch

## 10. Tracing Enterprise

OpenTelemetry permite propagar contexto entre servicios.

Flujo:

```text
request entra con traceparent
gateway crea/propaga trace
payment-service crea span
fraud-service crea span
ledger-service crea span
logs incluyen trace_id
```

Esto permite abrir una traza y ver:

- duracion total
- duracion por servicio
- errores
- llamada mas lenta
- dependencias involucradas

En Kafka/eventos:

- propagar trace context en headers
- crear spans para producer/consumer

## 11. Correlation ID vs Trace ID

Correlation ID:

```text
id de negocio/request usado por humanos y logs
```

Trace ID:

```text
id tecnico de tracing distribuido
```

Pueden coexistir.

Ejemplo:

```text
X-Correlation-Id: generado por gateway o cliente
traceparent: generado/propagado por OpenTelemetry
```

En entrevista:

> I use trace IDs for distributed tracing and correlation IDs for business/request-level log correlation when needed. Both should be propagated across HTTP and messaging boundaries.

## 12. Dashboards Recomendados

### Executive / Service Overview

- availability
- request rate
- error rate
- p95/p99 latency
- top failing endpoints

### JVM

- heap
- non-heap
- GC pause
- threads
- CPU

### Database

- connection pool usage
- slow queries
- error rate
- latency

### Kafka

- consumer lag
- messages consumed
- publish failures
- DLQ size

### Payments

- created/completed/failed
- pending
- provider latency
- idempotency conflicts
- reconciliation mismatches

### Outbox

- pending
- failed
- publish latency
- retries

## 13. Alerting

Alertas deben tener:

- condicion
- duracion
- severidad
- owner
- runbook
- dashboard link

Ejemplo:

```text
Payment provider timeout rate > 5% for 5 minutes
Severity: high
Owner: payments
Runbook: docs/runbooks/payment-provider-timeouts.md
```

Evitar:

- alertas ruidosas
- alertas sin accion
- alertar por causa interna antes de sintoma de usuario

## 14. Runbooks

Un runbook responde:

```text
que significa esta alerta?
como valido impacto?
que comandos miro?
que dashboard abro?
como mitigo?
cuando escalo?
```

Para banking, runbooks son clave.

Ejemplos:

- payment provider down
- Kafka lag high
- outbox stuck
- DB connection pool exhausted
- login failures spike
- Redis unavailable

## 15. Observabilidad En Kubernetes

Agregar:

- pod restart count
- container CPU/memory
- OOMKilled
- readiness/liveness failures
- deployment rollout status
- ingress latency/errors
- node pressure

Herramientas:

- kube-state-metrics
- cAdvisor
- Prometheus Operator
- Grafana dashboards Kubernetes

## 16. Seguridad Y Privacidad

En banking, observabilidad no debe filtrar datos sensibles.

No loggear:

- password
- token completo
- JWT completo
- tarjeta completa
- CVV
- secrets
- PII innecesaria

Enmascarar:

```text
card: **** **** **** 1234
email: w***@domain.com
```

Controlar acceso a dashboards/logs/traces.

## 17. Paso a Paso Enterprise Para Banking

### Paso 1. Activar Actuator/Micrometer

En cada microservicio:

```text
health
info
prometheus
```

### Paso 2. Prometheus + Grafana

Scrapear todos los servicios.

Crear dashboards:

- infra
- HTTP
- business

### Paso 3. Metricas custom de negocio

Agregar metrics para:

- payments
- ledger
- outbox
- kafka consumers
- idempotency

### Paso 4. Logs estructurados

JSON logs con:

- service
- trace_id
- correlation_id
- business id seguro

### Paso 5. OpenTelemetry

Agregar tracing HTTP y Kafka.

Exportar a Tempo/Jaeger.

### Paso 6. Alertas

Crear alertas basadas en SLO y sintomas.

### Paso 7. Runbooks

Cada alerta importante debe tener runbook.

### Paso 8. Pruebas De Observabilidad

Probar:

- servicio caido
- endpoint lento
- payment provider falla
- Kafka lag
- Redis caido
- DB caida
- error 500
- endpoint con alta latencia

Validar:

- metrica aparece
- alerta dispara
- log tiene correlation/trace id
- trace muestra el camino

## 18. Que Implementaria En Tu Banking System

Primera fase:

- Actuator + Micrometer + Prometheus
- Grafana
- metricas custom de payments/outbox
- logs claros con ids de negocio seguros

Segunda fase:

- OpenTelemetry
- Tempo/Jaeger
- Loki o ELK/OpenSearch
- correlation id
- dashboards versionados

Tercera fase:

- SLOs
- alertas con runbooks
- Prometheus Operator en Kubernetes
- tracing HTTP + Kafka
- security/audit observability

## 19. Respuesta Para Entrevista

Version corta:

> Observability is built from metrics, logs and traces. Metrics tell me something is wrong, logs explain what happened, and traces show where time was spent across services.

Version banking:

> For a banking system, I would instrument both technical and business signals. I care about HTTP latency and JVM metrics, but also payment failures, pending payments, idempotency conflicts, outbox backlog, Kafka lag and reconciliation mismatches. I would use OpenTelemetry for traces, structured logs with trace IDs, Prometheus/Grafana for metrics and alerts with runbooks.

Version senior:

> I do not consider dashboards alone to be observability. A production system needs actionable alerts, SLOs, correlation between logs/metrics/traces, safe handling of sensitive data, and runbooks that let engineers diagnose incidents quickly.

## 20. Que No Diria

No diria:

Actuator es suficiente para observabilidad enterprise.

Logs son suficientes.

Grafana guarda las metricas.

Prometheus recibe push de todos los servicios por defecto.

Puedo usar user_id/payment_id como label de Prometheus.

Esta bien loggear JWTs para debug.

Dashboard verde significa que no hay problema.

## 21. Fuentes Utiles

- Spring Boot Actuator endpoints: https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
- Spring Boot Actuator metrics: https://docs.spring.io/spring-boot/reference/actuator/metrics.html
- Micrometer concepts: https://docs.micrometer.io/micrometer/reference/concepts.html
- OpenTelemetry docs: https://opentelemetry.io/docs/
