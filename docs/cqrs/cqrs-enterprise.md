# CQRS Enterprise

Este documento es la version recomendada para pensar CQRS en un sistema bancario o enterprise.

CQRS no es una decoracion de arquitectura. Es una herramienta para casos donde lectura y escritura tienen necesidades distintas.

## 1. Decision Principal

La pregunta no es:

```text
Uso CQRS?
```

La pregunta correcta es:

```text
mis lecturas y escrituras necesitan modelos, performance, escalabilidad o consistencia diferentes?
```

Si la respuesta es no, CRUD puede ser suficiente.

En una entrevista senior, esta es la parte mas importante:

```text
CQRS no es una meta.
CQRS es una respuesta a una diferencia real entre writes y reads.
```

Si dices "usaria CQRS en todo", puede sonar academico.

Si dices "lo usaria donde el write side requiere reglas fuertes y el read side requiere vistas optimizadas", suena mucho mas real.

## 2. Que Es CQRS

CQRS separa:

```text
Command side -> cambia estado
Query side   -> lee estado
```

Command side:

- valida intencion
- aplica reglas de negocio
- persiste cambios
- emite eventos si aplica

Query side:

- arma vistas
- optimiza lectura
- puede usar read models
- puede usar cache/search/document DB

## 3. Niveles De CQRS

### Nivel 1. Separacion En Codigo

Mismo servicio, misma DB/store, handlers separados.

```text
CommandHandler
QueryHandler
```

Bueno para claridad.

Acme Cart Service esta cerca de este nivel.

### Nivel 2. Modelos Separados

Mismo servicio, pero DTOs/modelos diferentes:

```text
CreatePaymentCommand
PaymentDetailsResponse
PaymentListView
```

Bueno cuando write model y read model no son iguales.

### Nivel 3. Stores Separados

Write side usa una DB transaccional.

Read side usa read model optimizado.

Ejemplo:

```text
payments write DB: PostgreSQL
payments read model: Elasticsearch / Redis / materialized view
```

### Nivel 4. Event-Driven CQRS

Commands generan eventos.

Consumers/projections actualizan read models.

```text
PaymentCreated
PaymentAuthorized
PaymentSettled
```

Este nivel introduce consistencia eventual.

### Nivel 5. Event Sourcing + CQRS

La fuente de verdad son eventos.

Estado actual se reconstruye/proyecta desde eventos.

Muy potente, pero complejo.

No lo usaria sin necesidad clara.

## 3.1. Decision Rapida Por Nivel

| Situacion | Nivel recomendado |
| --- | --- |
| Servicio CRUD simple | No usar CQRS o Nivel 1 muy ligero |
| Servicio con reglas de escritura y DTOs de lectura diferentes | Nivel 1 o 2 |
| Dashboard que une muchos datos | API composition o Nivel 3 |
| Lecturas muy frecuentes y caras | Nivel 3 |
| Necesitas actualizar vistas desde eventos | Nivel 4 |
| Auditoria completa basada en historial de eventos | Evaluar Nivel 5 |

Para un sistema bancario nuevo, mi recomendacion inicial seria:

```text
Nivel 1/2 para claridad en comandos de dominio.
Nivel 3/4 solo en pantallas o procesos donde el beneficio sea claro.
No arrancar todo con Event Sourcing salvo necesidad fuerte.
```

## 4. CQRS No Es Event Sourcing

CQRS:

```text
separa lectura/escritura
```

Event Sourcing:

```text
persistir eventos como fuente de verdad
```

Pueden combinarse, pero CQRS puede existir sin Event Sourcing.

En entrevista, esta diferencia importa.

## 5. Banking: Donde Tiene Sentido

### Payments

Commands:

- CreatePayment
- AuthorizePayment
- CancelPayment
- CapturePayment

Queries:

- GetPaymentStatus
- ListPaymentsByCustomer
- GetPaymentTimeline

Write side:

- consistencia fuerte
- idempotencia
- auditoria
- transacciones

Read side:

- vistas por cliente
- filtros
- historial
- timelines

### Ledger

Commands:

- PostLedgerEntry
- ReverseEntry

Queries:

- GetAccountBalance
- GetStatement
- GetLedgerEntries

Cuidado:

Ledger requiere consistencia fuerte. No usaria read model eventual para decisiones que requieren balance exacto sin entender el riesgo.

Una frase buena para entrevista:

> A read model can serve account history or dashboard views, but the source of truth for posting money movement should be the ledger/write model, not a stale projection.

### Customer Dashboard

Muy buen candidato a read model:

```text
customer profile
accounts
cards
recent transactions
payment status
alerts
```

El dashboard puede aceptar consistencia eventual controlada.

## 6. Donde No Lo Usaria

No forzaria CQRS para:

- CRUD simple admin
- catalogos chicos
- configuraciones internas
- auth/token issuing simple
- servicios sin diferencia real entre reads/writes

CQRS mal aplicado solo aumenta complejidad.

## 7. Arquitectura Banking Con CQRS

Ejemplo para payments:

```text
POST /payments
  -> CreatePaymentCommand
  -> PaymentCommandHandler
  -> payment DB transaction
  -> outbox PaymentCreated

Outbox Publisher
  -> Kafka PaymentCreated

PaymentProjectionConsumer
  -> payment_read_model

GET /payments/{id}
  -> PaymentQueryHandler
  -> payment_read_model
```

El command side protege consistencia.

El query side optimiza lectura.

## 8. Consistencia Eventual

Si read model se actualiza por eventos, puede haber delay.

Ejemplo:

```text
POST /payments -> 202 Accepted
GET /payments/{id} inmediatamente -> aun PENDING/UNKNOWN
```

Eso no necesariamente es bug.

Pero debe ser parte del contrato.

Para UX:

- mostrar `PENDING`
- polling
- websocket/SSE
- refresh automatico
- timeline de estado

Para decisiones criticas:

- leer write model
- usar transaccion
- no depender de read model eventual

En banking, esta distincion es clave:

| Caso | Puede usar read model eventual? |
| --- | --- |
| Mostrar historial reciente | Si, con indicador de frescura si aplica |
| Dashboard de cliente | Si, normalmente |
| Busqueda y filtros | Si |
| Decidir si hay saldo para debitar | No deberia depender solo de una proyeccion stale |
| Evitar doble cobro | No, requiere idempotencia/constraint/write model |
| Auditoria/regulacion | Fuente de verdad fuerte, no solo cache |

## 9. Read Model

Un read model es una estructura optimizada para lectura.

Puede vivir en:

- tabla SQL materializada
- Redis
- Elasticsearch/OpenSearch
- MongoDB/document DB
- Cassandra/DynamoDB

Ejemplo:

```text
payment_dashboard_view
  customer_id
  payment_id
  amount
  status
  created_at
  merchant_name
```

No necesariamente esta normalizado como el write model.

Esta disenado para responder rapido a una pantalla/query.

## 10. Outbox Y CQRS

Si el command side emite eventos para actualizar read models, necesitas publicacion confiable.

Patron recomendado:

```text
misma transaccion:
  guardar cambio de negocio
  guardar outbox event

poller:
  publica evento a Kafka
  marca outbox como publicado
```

Esto evita:

```text
DB se actualizo pero evento no se publico
```

En Acme ya usaste outbox en order/inventory, asi que esta pieza conecta muy bien con CQRS enterprise.

La combinacion enterprise mas comun seria:

```text
Command Handler
  -> transaccion DB
     -> actualiza aggregate/write model
     -> guarda outbox event

Outbox Publisher
  -> Kafka

Projection Consumer
  -> actualiza read model
```

Esto permite que el command side sea transaccional sin depender de publicar a Kafka dentro de la misma llamada HTTP.

## 11. CQRS Y API Composition

No confundir:

API composition:

```text
un servicio consulta varios servicios y compone respuesta
```

CQRS read model:

```text
vista precomputada/proyectada para lectura
```

`catalog-query-service` en Acme es API composition/query service.

Un read model enterprise seria:

```text
catalog_read_model actualizado por ProductUpdated + InventoryChanged events
```

Ambos son validos. La decision depende de:

- latencia
- volumen
- frescura requerida
- costo de llamadas downstream
- complejidad operativa

## 12. CQRS Y Redis/ElasticSearch

Redis:

- lectura rapida
- cache/read model temporal
- TTL
- datos derivados

Elasticsearch/OpenSearch:

- busqueda
- filtros
- ranking
- full-text search
- catalogos grandes

PostgreSQL materialized/read tables:

- read model relacional
- mas simple que meter otra tecnologia

Para banking, empezaria simple:

```text
PostgreSQL write DB
read tables/projections si hace falta
Redis para cache/control temporal
OpenSearch solo si hay busqueda real
```

## 13. Observabilidad En CQRS

Medir:

- commands received
- commands succeeded/failed
- command latency
- query latency
- projection lag
- read model freshness
- outbox pending
- consumer lag
- projection failures

Ejemplo:

```text
payment_projection_lag_seconds
payment_read_model_last_event_timestamp
outbox_events_pending
```

Sin observabilidad, CQRS event-driven se vuelve dificil de operar.

## 14. Testing

Command tests:

- valida reglas de negocio
- cambia estado correctamente
- emite outbox event
- idempotency funciona

Query tests:

- devuelve DTO correcto
- performance aceptable
- maneja read model vacio/stale

Projection tests:

- evento A actualiza read model
- evento duplicado no rompe
- evento fuera de orden se maneja
- replay funciona

## 15. Paso a Paso Enterprise Para Banking

### Paso 1. Elegir caso real

No empezar con CQRS en todo.

Elegir:

```text
payments dashboard
customer account overview
transaction history
```

### Paso 2. Definir command model

Ejemplo:

```java
public record CreatePaymentCommand(
    UUID customerId,
    UUID sourceAccountId,
    BigDecimal amount,
    String currency,
    String idempotencyKey
) {}
```

### Paso 3. Definir command handler

Responsabilidades:

- validar input
- validar ownership
- validar saldo/reglas
- persistir intento
- crear outbox event
- respetar idempotency

### Paso 4. Definir eventos

```text
PaymentCreated
PaymentAuthorized
PaymentFailed
PaymentSettled
```

### Paso 5. Crear read model

Ejemplo:

```text
payment_activity_view
```

Campos optimizados para pantalla.

### Paso 6. Crear projection consumer

Consume eventos y actualiza read model.

Debe manejar:

- duplicados
- reintentos
- orden
- errores
- replay si aplica

### Paso 7. Crear query handler

Lee read model.

No aplica reglas de negocio de escritura.

### Paso 8. Medir y alertar

Metricas:

- projection lag
- failed projections
- query latency
- outbox pending

### Paso 9. Definir Estrategia De Rebuild

Si el read model se corrompe o cambia el formato de la vista, necesitas reconstruirlo.

Opciones:

- replay desde eventos historicos
- job batch desde write DB
- backfill incremental
- versionar read models y migrar gradualmente

Esto es muy importante porque un read model es derivado.

Si no puedes reconstruirlo, en produccion se vuelve fragil.

### Paso 10. Definir Contrato De Frescura

No basta con decir "eventual consistency".

Hay que definir:

- cuanto lag es aceptable
- que ve el usuario mientras el read model no actualiza
- si se permite fallback al write model
- que alertas existen cuando el lag sube

Ejemplo:

```text
payment activity view puede tener segundos de retraso
payment authorization no puede depender de una vista retrasada
```

## 16. Que Implementaria En Tu Banking System

Primera version:

- CQRS ligero en Payments
- commands separados de queries
- idempotency en commands
- outbox events
- query handlers para vistas

Segunda version:

- read model para payment activity
- projection consumer desde Kafka
- metricas de lag/projection

Tercera version:

- read model para customer dashboard
- search/read model si hay necesidad real
- replay de proyecciones

No empezaria con Event Sourcing completo salvo que sea el foco explicito.

## 16.1. Propuesta Concreta Para Un Sistema Bancario

Para un proyecto banking serio pero controlado:

```text
payment-service
  command side:
    CreatePaymentCommand
    AuthorizePaymentCommand
    CancelPaymentCommand
    idempotency
    outbox events

  query side:
    GetPaymentStatusQuery
    ListCustomerPaymentsQuery

payment-read-service o modulo projection:
  consume PaymentCreated/Authorized/Failed/Settled
  actualiza payment_activity_view
```

Tecnologias razonables:

- PostgreSQL como write DB
- Kafka para eventos
- Outbox para publicacion confiable
- Redis solo para cache/locks/idempotency temporal si aplica
- OpenSearch solo si hay busqueda compleja real

No venderia Redis como fuente de verdad financiera.

## 17. Respuesta Para Entrevista

Version corta:

> CQRS separates commands that change state from queries that read state. It is useful when read and write paths have different models, scale or consistency requirements.

Version banking:

> In a banking system, I would use CQRS selectively. Payment commands must preserve consistency, idempotency and auditability, while payment queries can read from optimized read models for dashboards or history. If read models are event-driven, I would explicitly handle eventual consistency, projection lag, retries and observability.

Version senior:

> I would not apply CQRS everywhere. For simple CRUD it is overengineering. I use it when there is a real difference between write-side invariants and read-side performance/composition needs. For critical financial decisions, I would be careful not to rely on stale read models unless the business process explicitly allows it.

## 18. Que No Diria

No diria:

CQRS siempre mejora performance.

CQRS es lo mismo que Event Sourcing.

CQRS siempre requiere Kafka.

CQRS siempre requiere dos bases de datos.

Read model eventual sirve para cualquier decision financiera.

CQRS elimina transacciones.

## 19. Fuentes Utiles

- Martin Fowler CQRS: https://martinfowler.com/bliki/CQRS.html
- Microsoft CQRS pattern: https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs
