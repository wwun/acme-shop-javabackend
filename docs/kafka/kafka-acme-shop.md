# Kafka en Acme Shop

Este documento explica como Acme Shop usa Kafka, Outbox Pattern e idempotencia para coordinar el flujo entre `order-service` e `inventory-service`.

La idea principal:

```text
order-service crea una orden
  -> guarda ORDER_CREATED en outbox
  -> poller publica a Kafka
  -> inventory-service consume y reserva stock
  -> inventory-service guarda STOCK_RESERVED o STOCK_FAILED en su outbox
  -> poller publica respuesta a Kafka
  -> order-service consume la respuesta y actualiza la orden
```

## Flujo End To End

1. `POST /api/orders`
   - `OrderServiceImpl` crea la orden.
   - La orden queda en estado `PENDING_CONFIRMATION`.
   - Se guarda un evento `ORDER_CREATED` en `order_schema.outbox_events` con estado `PENDING`.

2. `OutboxPoller` de `order-service`
   - Corre cada cierto intervalo.
   - Lee eventos `PENDING`.
   - Si el evento es `ORDER_CREATED`, llama a `KafkaOrderProducer.publishOrderCreated(payload)`.
   - Publica en el topic `order.created`.
   - Si Kafka confirma/publica correctamente, marca el outbox event como `PROCESSED`.

3. `KafkaOrderEventConsumer` de `inventory-service`
   - Escucha el topic `order.created`.
   - Recibe un `String payload`.
   - Usa `ObjectMapper` para convertir JSON a `OrderCreatedEvent`.
   - Revisa idempotencia con `ProcessedEventRepository`.
   - Si no fue procesado, llama a `inventoryService.reserveStock(event)`.

4. Resultado en `inventory-service`
   - Si hay stock, crea `StockReservedEvent`.
   - Si no hay stock o no existe inventory, crea `StockFailedEvent`.
   - No publica directamente la respuesta a Kafka.
   - Guarda el resultado en el outbox de inventory.

5. `OutboxPoller` de `inventory-service`
   - Lee eventos `PENDING`.
   - Si el tipo es `STOCK_RESERVED`, publica en `inventory.stock.reserved`.
   - Si el tipo es `STOCK_FAILED`, publica en `inventory.stock.failed`.
   - Marca el evento como `PROCESSED`.

6. `KafkaStockEventConsumer` de `order-service`
   - Escucha `inventory.stock.reserved`.
   - Escucha `inventory.stock.failed`.
   - Si recibe `STOCK_RESERVED`, cambia la orden a `AWAITING_PAYMENT`.
   - Si recibe `STOCK_FAILED`, cambia la orden a `CANCELLED`.
   - Guarda `ProcessedEvent` para idempotencia.

Version para memorizar:

```text
OrderService guarda orden y ORDER_CREATED en outbox.
OutboxPoller(order) lee PENDING.
KafkaOrderProducer publica a order.created.
KafkaOrderEventConsumer(inventory) consume order.created.
Inventory reserva stock.
Inventory guarda STOCK_RESERVED o STOCK_FAILED en su outbox.
OutboxPoller(inventory) publica a inventory.stock.reserved/failed.
KafkaStockEventConsumer(order) actualiza la orden.
```

## Familias De Clases

No conviene pensar en diez clases sueltas. Conviene pensar en cuatro grupos.

### 1. Clases Que Guardan Eventos En DB

Estas no hablan con Kafka todavia.

- `OutboxEvent`
- `OutboxEventRepository`
- `OutboxEventPublisher`

Responsabilidad:

- recibir un evento de negocio
- serializarlo a JSON
- guardarlo en `outbox_events`

### 2. Clases Que Leen La Tabla Outbox

Estas tampoco toman decisiones de negocio.

- `OutboxPoller` de `order-service`
- `OutboxPoller` de `inventory-service`

Responsabilidad:

- buscar eventos `PENDING`
- decidir producer segun `type`
- publicar en Kafka
- marcar `PROCESSED` si salio bien
- marcar `FAILED` si fallo tecnicamente

### 3. Clases Que Publican A Kafka

Son puentes hacia Kafka.

- `KafkaOrderProducer`
- `KafkaInventoryProducer`

Responsabilidad:

- recibir un payload
- enviarlo al topic correcto
- no ejecutar reglas de negocio

### 4. Clases Que Consumen De Kafka

Estas si disparan logica de negocio.

- `KafkaOrderEventConsumer` en inventory
- `KafkaStockEventConsumer` en order

Responsabilidad:

- escuchar topics
- deserializar payload
- validar idempotencia
- llamar logica de negocio o actualizar estado

## KafkaTemplate

`KafkaTemplate` es la herramienta de Spring para enviar mensajes a Kafka.

Es comparable a:

- `JdbcTemplate` para DB
- `RestTemplate` para HTTP
- `RedisTemplate` para Redis

Ejemplo:

```java
kafkaTemplate.send(topic, payload);
```

Eso significa:

```text
Spring, usando el cliente Kafka, conectate al broker y manda este payload a este topic.
```

Si tienes:

```java
private final KafkaTemplate<String, Object> kafkaTemplate;
```

los genericos significan:

```text
KafkaTemplate<KeyType, ValueType>
KafkaTemplate<String, Object>
key   = String
value = Object
```

En Acme, muchas veces el payload ya es un JSON `String`. Para ese estilo, lo mas coherente seria:

```yaml
spring:
  kafka:
    producer:
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

Si se usan `JsonSerializer` y `JsonDeserializer`, lo ideal es enviar/recibir objetos Java y dejar que Spring haga la serializacion.

Leccion:

> No mezclar dos estilos sin querer: o mandas objetos y Spring serializa JSON, o mandas JSON String y tu haces `ObjectMapper.readValue`.

## Como Se Llama `consume(payload)`

Tu codigo no llama manualmente a `consume`.

El flujo real es:

```text
order-service
  kafkaTemplate.send(orderCreatedTopic, payload)

Kafka broker
  guarda el mensaje en topic order.created

inventory-service
  @KafkaListener(topics = "${kafka.topics.order-created}")
  public void consume(String payload)
```

Cuando Spring arranca `inventory-service`, ve `@KafkaListener` y crea por debajo un Kafka consumer.

Ese consumer se conecta a:

```text
kafka:9092
```

y se suscribe al topic. Cuando Kafka entrega un mensaje, Spring Kafka ejecuta el metodo:

```java
consume(payload)
```

## Topics, Producers, Consumers Y Broker

Un topic es un canal logico de mensajes.

Ejemplos:

- `order.created`
- `inventory.stock.reserved`
- `inventory.stock.failed`

Un producer envia mensajes.

Ejemplo:

```text
order-service produce ORDER_CREATED en order.created
```

Un consumer recibe mensajes.

Ejemplo:

```text
inventory-service consume order.created
```

El broker Kafka es el proceso externo donde viven los topics.

En Docker Compose:

```yaml
kafka:
  image: ...
```

Los servicios se conectan con:

```yaml
spring.kafka.bootstrap-servers: kafka:9092
```

Kafka no vive dentro de `order-service` ni `inventory-service`. Es un intermediario externo.

## Consumer Group Y Offset

Un consumer group identifica a un conjunto de consumidores que comparten el avance de lectura.

Ejemplo:

```yaml
spring.kafka.consumer.group-id: inventory-service-group
```

Kafka guarda el offset, que es la posicion leida.

```text
topic order.created
  offset 0
  offset 1
  offset 2
  offset 3
```

Si inventory ya proceso hasta el offset 2, Kafka recuerda ese avance para ese `group-id`.

Si el servicio se cae y vuelve, puede continuar desde donde quedo.

## Outbox `PROCESSED` No Significa Flujo Completo

`OutboxEventStatus.PROCESSED` significa:

```text
Este servicio ya publico este evento desde su outbox.
```

No significa:

- que el otro servicio ya lo proceso
- que la orden ya termino
- que el flujo completo fue exitoso
- que el cliente ya pago

Ejemplo:

```text
ORDER_CREATED PROCESSED en order outbox
```

solo significa:

```text
order-service ya publico ORDER_CREATED a Kafka.
```

Inventory todavia puede fallar despues.

## STOCK_FAILED Vs OutboxEventStatus.FAILED

Estos dos nombres son parecidos, pero significan cosas diferentes.

### `STOCK_FAILED`

Es un evento de negocio.

Significa:

```text
Kafka funciono, inventory recibio el pedido, pero no pudo reservar stock.
```

Ejemplo:

```java
outboxEventPublisher.publish(
    event.orderId(),
    OutboxEventType.STOCK_FAILED,
    stockFailedEvent
);
```

Ese evento si debe publicarse a Kafka para que order cancele la orden.

### `OutboxEventStatus.FAILED`

Es un fallo tecnico del outbox.

Significa:

```text
El poller intento publicar el evento a Kafka y fallo tecnicamente.
```

Ejemplo:

```java
try {
    kafkaProducer.publish(event.getPayload());
    event.markAsProcessed();
} catch (Exception ex) {
    event.markAsFailed();
}
```

Frase clave:

> `STOCK_FAILED` no significa que Kafka fallo. `OutboxEventStatus.FAILED` si significa que fallo la publicacion/procesamiento tecnico del outbox.

## Idempotencia

Kafka normalmente se diseña pensando en entrega `at-least-once`.

Eso significa:

```text
El mensaje puede llegar una o mas veces.
```

Por eso los consumidores deben ser idempotentes.

En Acme se usa:

- `ProcessedEvent`
- `ProcessedEventRepository`
- combinacion de `eventId` y `consumer`

La idea:

```text
Antes de procesar, reviso si ya procese ese evento.
Si ya existe, retorno.
Si no existe, proceso y guardo ProcessedEvent.
```

Regla importante:

```text
orderId = identifica la entidad de negocio
eventId = identifica el mensaje/evento
```

Para un flujo simple, `orderId` puede servir como event id temporal. Pero si luego existen varios eventos para la misma orden:

- `OrderCreatedEvent`
- `OrderCancelledEvent`
- `PaymentCompletedEvent`
- `OrderShippedEvent`

entonces `orderId` ya no alcanza. Se necesita un `eventId` real.

## Errores Reales Encontrados

### Enum Guardado Como String

Error:

```text
Parameter value [PENDING] did not match expected type
Could not convert OutboxEventStatus to String
```

Causa:

El campo era enum en Java, pero Hibernate no sabia como persistirlo correctamente.

Solucion:

```java
@Enumerated(EnumType.STRING)
private OutboxEventStatus status;
```

Leccion:

> En Java trabajas con enum. En DB puedes guardarlo como texto. No necesitas convertir manualmente con `.name()` si JPA conoce el mapping.

### Payload Muy Largo Para `varchar(255)`

Error:

```text
ERROR: value too long for type character varying(255)
insert into outbox_events (... payload ...)
```

Causa:

El JSON del evento puede superar 255 caracteres.

Solucion:

```sql
ALTER TABLE outbox_events
ALTER COLUMN payload TYPE TEXT;
```

Leccion:

> Los payloads de eventos no deben vivir en `varchar(255)`. Para outbox, usar `TEXT` o equivalente.

### Kafka No Estaba Listo

Errores:

```text
No resolvable bootstrap urls given in bootstrap.servers
Couldn't resolve server kafka:29092
DNS resolution failed for kafka
```

Causa:

`order-service` o `inventory-service` arrancaron antes de que Kafka estuviera realmente listo.

Solucion:

- healthcheck en Kafka
- `depends_on` con `condition: service_healthy`
- reiniciar servicios consumidores/producers despues de Kafka si hace falta

### Header `Idempotency-Key` Bloqueado

Problema:

`POST /api/orders` usaba el header custom `Idempotency-Key`, pero CORS/gateway no lo aceptaba.

Solucion:

```java
config.setAllowedHeaders(List.of(
    "Authorization",
    "Content-Type",
    "Idempotency-Key"
));
```

Leccion:

> Si agregas headers funcionales, gateway/security/CORS deben permitirlos.

### Null En Request Hash

Error:

```text
Cannot invoke "String.equals(Object)" because Order.getRequestHash() is null
```

Causa:

Se comparaba:

```java
duplicatedOrder.get().getRequestHash().equals(orderHashed)
```

pero `getRequestHash()` podia venir `null`.

Mejor:

```java
Objects.equals(duplicatedOrder.get().getRequestHash(), orderHashed)
```

o validar que la data historica tenga request hash.

## Mejoras Pendientes Para Acme

- Usar `eventId` real, no solo `orderId`.
- Publicar a Kafka con key de negocio:

```java
kafkaTemplate.send(topic, orderId.toString(), payload);
```

- Preservar orden por aggregate usando particiones.
- Marcar outbox como `PROCESSED` solo cuando Kafka confirme publish.
- Agregar retries antes de marcar `FAILED`.
- Agregar DLQ despues de varios retries.
- Implementar handlers por tipo de evento.
- Crear topics por IaC/pipeline, no por la app.
- Agregar metricas:
  - eventos pendientes
  - eventos fallidos
  - edad del evento mas antiguo pendiente
  - lag de consumidores
  - errores por topic

