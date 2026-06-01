# Kafka Enterprise

Este documento resume como usaria Kafka en un sistema bancario o enterprise, especialmente para pagos, ordenes, reservas, auditoria y comunicacion asincrona entre microservicios.

La postura recomendada:

```text
Kafka no es solo "una cola".
Kafka es un log distribuido para eventos durables, re-procesables y observables.
```

## Cuando Usar Kafka

Usaria Kafka cuando necesito:

- desacoplar servicios
- procesar eventos asincronos
- tolerar caidas temporales de consumidores
- mantener historial temporal de eventos
- re-procesar eventos si hubo un bug
- coordinar workflows distribuidos
- alimentar read models, analytics o auditoria

Ejemplos:

- `PaymentRequested`
- `PaymentAuthorized`
- `PaymentCaptured`
- `PaymentFailed`
- `OrderCreated`
- `StockReserved`
- `FraudCheckRequested`
- `NotificationRequested`

## Cuando No Usar Kafka

No usaria Kafka para todo.

No es la mejor opcion si:

- necesito respuesta sincrona inmediata
- necesito una consulta simple request/response
- el flujo es muy pequeno y no requiere durabilidad de eventos
- el equipo no tiene experiencia operando Kafka
- el costo operativo no se justifica

Para request/response directo, usar HTTP/gRPC puede ser mas simple.

Para trabajos puntuales simples, RabbitMQ o una queue administrada pueden ser suficientes.

## Kafka Vs RabbitMQ

RabbitMQ y Kafka pueden resolver problemas parecidos, pero su modelo mental es diferente.

### RabbitMQ

RabbitMQ funciona muy bien como broker de colas.

Modelo:

```text
Producer -> queue -> consumer -> ACK -> mensaje se elimina
```

Ventajas:

- mas simple para work queues
- routing flexible
- bueno para tareas donde cada mensaje se consume una vez
- menor complejidad inicial

Limitacion:

Si un consumidor confirma (`ACK`) y despues descubres un bug, el mensaje normalmente ya no esta.

### Kafka

Kafka guarda eventos en un log.

Modelo:

```text
Producer -> topic/partition -> consumer group lee por offset
```

Ventajas:

- mensajes persisten durante la retencion configurada
- consumidores independientes pueden leer el mismo topic
- se puede re-procesar moviendo offsets
- alto throughput
- buen soporte para event-driven architecture

Frase para entrevista:

> RabbitMQ es excelente para work queues. Kafka es mejor cuando necesito un log durable de eventos, multiples consumidores independientes y capacidad de re-procesamiento.

## Transactional Outbox Pattern

Problema:

```text
Guardo una orden en DB.
Luego publico evento a Kafka.
```

Si la DB guarda bien pero Kafka falla, queda estado inconsistente.

Si Kafka publica bien pero la DB falla, tambien queda inconsistente.

Solucion:

```text
Dentro de la misma transaccion de negocio:
  1. guardar entidad
  2. guardar evento en tabla outbox

Luego un poller o relay publica el outbox a Kafka.
```

Flujo:

```text
Business transaction
  -> update local DB
  -> insert outbox event

Outbox relay
  -> read pending event
  -> publish to Kafka
  -> mark as processed
```

Ventaja:

- la DB local y el evento quedan atomicamente registrados
- si Kafka cae, el evento sigue en outbox
- el poller puede reintentar

## Estados Del Outbox

Estados tipicos:

- `PENDING`
- `PROCESSING`
- `PROCESSED`
- `FAILED`
- `DEAD_LETTERED`

En produccion evitaria marcar `FAILED` definitivo al primer error.

Mejor:

```text
PENDING -> PROCESSING -> PROCESSED
                  |
                  -> RETRYABLE_FAILED -> PENDING despues de backoff
                  |
                  -> DEAD_LETTERED despues de N intentos
```

Campos utiles:

- `id`
- `event_id`
- `aggregate_id`
- `aggregate_type`
- `event_type`
- `payload`
- `headers`
- `status`
- `attempt_count`
- `next_retry_at`
- `created_at`
- `processed_at`
- `last_error`

## Idempotencia

En Kafka se debe asumir `at-least-once`.

Eso significa:

```text
Un mensaje puede llegar mas de una vez.
```

Por eso el consumidor debe ser idempotente.

Patron:

```text
Antes de procesar:
  buscar event_id + consumer_name en processed_events

Si existe:
  ignorar

Si no existe:
  procesar
  guardar processed_event
```

Tabla:

```sql
processed_events(
  event_id uuid,
  consumer varchar,
  processed_at timestamp,
  primary key(event_id, consumer)
)
```

Regla:

```text
event_id identifica el mensaje.
aggregate_id identifica la entidad de negocio.
```

Ejemplo:

```text
aggregate_id = orderId
event_id     = uuid unico del evento PaymentAuthorized
```

## Kafka Key Y Orden

Kafka solo garantiza orden dentro de una particion.

Para preservar orden por entidad, publicar con key de negocio:

```java
kafkaTemplate.send(topic, orderId.toString(), payload);
```

Con eso todos los eventos de la misma orden tienden a ir a la misma particion.

Ejemplo:

```text
OrderCreated(order-123)
StockReserved(order-123)
PaymentAuthorized(order-123)
OrderConfirmed(order-123)
```

La key debe ser:

```text
order-123
```

No conviene usar key aleatoria si necesitas orden por aggregate.

## Retries Y DLQ

En produccion separaria:

- errores transitorios
- errores permanentes

Errores transitorios:

- Kafka temporalmente no disponible
- downstream caido
- timeout
- lock temporal

Estrategia:

- retry con backoff
- limite de intentos
- metricas

Errores permanentes:

- payload invalido
- schema incompatible
- entidad inexistente no recuperable

Estrategia:

- DLQ
- alerta
- investigacion manual o replay controlado

Topics posibles:

```text
payment.events
payment.events.retry
payment.events.dlq
```

## Schema Registry

En sistemas grandes no conviene que cada servicio invente JSON libremente sin contrato.

Opciones:

- Avro + Schema Registry
- Protobuf
- JSON Schema

Beneficios:

- versionado de contratos
- validacion de compatibilidad
- consumidores mas seguros
- menos errores por campos renombrados

Estrategia recomendada:

```text
event type estable
event version explicita
schema compatible hacia atras cuando sea posible
```

Ejemplo:

```json
{
  "eventId": "...",
  "eventType": "PaymentAuthorized",
  "eventVersion": 1,
  "occurredAt": "...",
  "aggregateId": "...",
  "payload": {}
}
```

## Topic Provisioning

En desarrollo se pueden crear topics desde la app.

En produccion preferiria:

- Terraform
- Helm chart
- pipeline CI/CD
- scripts operacionales aprobados
- tooling del proveedor cloud

No dependeria de que cada app cree topics al arrancar.

Razones:

- control de particiones
- control de replication factor
- permisos
- auditoria
- evitar cambios accidentales

## Configuracion De Topics

Decisiones importantes:

- numero de particiones
- replication factor
- retention time
- cleanup policy
- compaction
- max message size

Ejemplo conceptual:

```text
topic: payment.events
partitions: 12
replication.factor: 3
retention: 7 days
cleanup.policy: delete
```

Para read models por ultimo estado, puede aplicar compaction:

```text
topic: account.balance.snapshot
cleanup.policy: compact
```

## Observabilidad Kafka

Metricas minimas:

- consumer lag por consumer group
- mensajes por segundo por topic
- errores de publish
- errores de consume
- retry count
- DLQ count
- edad del evento mas antiguo en outbox
- cantidad de eventos `PENDING`
- cantidad de eventos `FAILED`
- tiempo promedio desde outbox hasta publish

Logs recomendados:

- `eventId`
- `aggregateId`
- `eventType`
- `topic`
- `partition`
- `offset`
- `consumerGroup`
- `correlationId`

Trazabilidad:

```text
HTTP request
  -> create event
  -> publish Kafka
  -> consume Kafka
  -> update DB
```

Debe poder seguirse con trace/correlation id.

## Payments Con Kafka

Para un payment service, modelaria eventos como:

- `PaymentRequested`
- `PaymentAuthorizationStarted`
- `PaymentAuthorized`
- `PaymentDeclined`
- `PaymentCaptured`
- `PaymentFailed`
- `PaymentRefunded`

Flujo ejemplo:

```text
order-service
  -> OrderAwaitingPayment

payment-service
  -> consume OrderAwaitingPayment
  -> crea Payment en estado PENDING
  -> llama banco/procesador
  -> publica PaymentAuthorized o PaymentFailed

order-service
  -> consume PaymentAuthorized
  -> cambia orden a CONFIRMED
```

Para evitar doble cobro:

- idempotency key en API
- unique constraint en payment request
- idempotencia al consumir eventos
- idempotency key hacia proveedor externo si existe
- estado de payment controlado

Estados posibles:

```text
PENDING
AUTHORIZED
CAPTURED
DECLINED
FAILED
REFUNDED
```

## Exactly Once

Kafka tiene features de exactly-once para ciertos escenarios, pero en sistemas de microservicios con DB externa no conviene venderlo como magia.

Respuesta senior:

> En la practica diseno consumidores idempotentes y uso outbox/inbox. Kafka puede ayudar con transacciones internas, pero cuando hay bases de datos y servicios externos, la proteccion real viene de idempotencia, constraints, estados y reintentos controlados.

## Checklist Enterprise

- Eventos con `eventId` real.
- `aggregateId` separado de `eventId`.
- Kafka key por aggregate.
- Outbox con retries y DLQ.
- Consumers idempotentes.
- Schema versionado.
- Topics provisionados por IaC/pipeline.
- Observabilidad de lag, DLQ, outbox pending y publish failures.
- Security:
  - TLS
  - SASL/OAuth si aplica
  - ACLs por producer/consumer
- Pruebas con Testcontainers.
- Documentar contratos de eventos.
- Plan de replay.
- Plan de manejo de poison messages.

## Como Lo Diria En Entrevista

> Use Kafka para desacoplar el flujo de ordenes, inventario y pagos. Para evitar inconsistencias entre la base de datos y Kafka, usaria Transactional Outbox. Cada consumidor seria idempotente usando `eventId` y una tabla de processed events. Publicaria con key por aggregate para preservar orden por entidad. En produccion no crearia topics desde la app, los provisionaria con IaC o pipeline. Tambien agregaria retries, DLQ, consumer lag metrics, trazabilidad por correlation id y versionado de schemas.

