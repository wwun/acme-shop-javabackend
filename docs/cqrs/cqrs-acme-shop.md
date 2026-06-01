# CQRS en Acme Shop

Este documento consolida las notas de CQRS y las alinea con la implementacion real de Acme Shop.

CQRS significa:

```text
Command Query Responsibility Segregation
```

La idea es separar operaciones que cambian estado de operaciones que solo leen.

## 1. Idea Principal

Command:

```text
intencion de modificar estado
```

Ejemplos:

- agregar item al carrito
- cambiar cantidad
- eliminar item
- crear orden
- reservar stock

Query:

```text
intencion de leer datos sin modificar estado
```

Ejemplos:

- obtener carrito
- obtener resumen del carrito
- listar productos
- obtener catalogo con disponibilidad

Regla mental:

```text
Command cambia algo.
Query pregunta algo.
```

## 2. Command Query Separation

La idea base viene de Command-Query Separation:

```text
Una operacion no deberia modificar estado y comportarse como una consulta al mismo tiempo.
```

En APIs reales a veces un command devuelve algo minimo:

- `201 Created` con id
- `202 Accepted`
- `204 No Content`

Pero conceptualmente no deberia ser el camino principal para construir una vista compleja.

## 3. CQRS No Es Obligatoriamente Event Sourcing

CQRS:

```text
separa writes y reads
```

Event Sourcing:

```text
guarda eventos como fuente de verdad
```

Pueden ir juntos, pero no son lo mismo.

Puedes tener CQRS simple sin Event Sourcing.

Acme usa CQRS-style en `cart-service`, pero no usa Event Sourcing para el carrito.

## 4. CQRS En Acme Shop

El uso real de CQRS esta principalmente en:

[cart-service](/home/william/Documents/portfolio/acme-shop/cart-service)

Estructura:

```text
cart-service
  cqrs/
    command/
      Commands.java
      CartCommandHandler.java
    query/
      Queries.java
      CartQueryHandler.java
```

Controller:

[CartController.java](/home/william/Documents/portfolio/acme-shop/cart-service/src/main/java/com/wwun/acme/cart/controller/CartController.java)

```java
private final CartCommandHandler command;
private final CartQueryHandler query;
```

El controller no hace la logica directamente.

Delega:

```text
POST/PUT/DELETE -> CommandHandler
GET             -> QueryHandler
```

## 5. Commands En Cart Service

Archivo:

[Commands.java](/home/william/Documents/portfolio/acme-shop/cart-service/src/main/java/com/wwun/acme/cart/cqrs/command/Commands.java)

```java
public record AddItemToCartCommand(UUID userId, UUID productId, int quantity){}
public record SetItemQuantityCommand(UUID userId, UUID productId, int quantity){}
public record RemoveItemFromCartCommand(UUID userId, UUID productId){}
public record ClearCartCommand(UUID userId){}
```

Los records son buena opcion para commands porque:

- son inmutables
- expresan intencion
- reducen boilerplate
- funcionan bien como DTOs internos

## 6. Command Handler

Archivo:

[CartCommandHandler.java](/home/william/Documents/portfolio/acme-shop/cart-service/src/main/java/com/wwun/acme/cart/cqrs/command/CartCommandHandler.java)

Ejemplo:

```java
public void handle(AddItemToCartCommand command){
    redisTemplate.opsForHash().increment(
        key(command.userId()),
        command.productId().toString(),
        command.quantity()
    );
    meterRegistry.counter("cart_commands_total", "type", "add").increment();
}
```

Responsabilidades:

- modificar estado del carrito en Redis
- aplicar regla simple de cantidad
- emitir metricas de comandos

El command handler no construye una vista de carrito.

Solo modifica estado.

## 7. Queries En Cart Service

Archivo:

[Queries.java](/home/william/Documents/portfolio/acme-shop/cart-service/src/main/java/com/wwun/acme/cart/cqrs/query/Queries.java)

```java
public record GetCartQuery(UUID userId){}
public record GetCartSummaryQuery(UUID userId){}
```

Las queries representan preguntas.

## 8. Query Handler

Archivo:

[CartQueryHandler.java](/home/william/Documents/portfolio/acme-shop/cart-service/src/main/java/com/wwun/acme/cart/cqrs/query/CartQueryHandler.java)

Ejemplo:

```java
public CartResponseDTO handle(GetCartQuery query){
    Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(query.userId()));
    ...
    return new CartResponseDTO(query.userId(), items);
}
```

Para resumen:

```java
ProductResponseDTO product = productGatewayService.getById(productId);
```

El query handler:

- lee Redis
- compone DTOs de respuesta
- llama product-service para enriquecer resumen
- mide latencia de query

## 9. Redis Y CQRS En Cart

En Acme, Redis funciona como store principal del carrito.

Commands:

```text
escriben en Redis Hash
```

Queries:

```text
leen desde Redis Hash
```

Esto es CQRS ligero:

- separa handlers
- separa intenciones
- usa DTOs distintos
- agrega metricas separadas

Pero no es CQRS avanzado con:

- write DB separada
- read model proyectado
- event sourcing
- proyecciones asincronas

Eso esta bien. No hay que inflarlo.

## 10. Catalog Query Service

`catalog-query-service` no es CQRS completo.

Es mas bien un query/aggregation service.

Archivo:

[CatalogQueryServiceImpl.java](/home/william/Documents/portfolio/acme-shop/catalog-query-service/src/main/java/com/wwun/acme/catalog/service/CatalogQueryServiceImpl.java)

Hace:

```text
product-service + inventory-service -> CatalogProductResponseDTO
```

Esto se parece a:

- BFF
- query service
- read-side composition
- API composition

Pero no hay commands equivalentes en ese servicio.

Por eso, una forma precisa de decirlo:

> Cart Service uses CQRS-style command/query handlers. Catalog Query Service is a read-side aggregation service, not full CQRS.

Eso suena mas senior que decir que todo es CQRS.

## 10.1. Diferencia Importante: CQRS, Query Service Y Read Model

Hay tres ideas que se parecen, pero no son iguales.

| Concepto | Que hace | Ejemplo en Acme |
| --- | --- | --- |
| CQRS ligero | Separa command handlers y query handlers dentro del servicio | `cart-service` |
| API composition / query service | Llama varios servicios y arma una respuesta | `catalog-query-service` |
| Read model proyectado | Guarda una vista precomputada para leer rapido | No esta implementado aun |

`catalog-query-service` hoy hace esto:

```text
request
  -> product-service batch
  -> inventory-service batch
  -> combina datos
  -> devuelve CatalogProductResponseDTO
```

Eso es muy util y realista, pero no es lo mismo que tener una tabla o indice `catalog_read_model` actualizado por eventos.

Una version mas avanzada seria:

```text
ProductUpdated event
InventoryChanged event
  -> CatalogProjectionConsumer
  -> catalog_read_model

GET /catalog
  -> lee catalog_read_model
```

La version actual es buena para aprender API composition y evitar que el frontend tenga que llamar dos servicios.

La version con read model sirve cuando hay mucho trafico, mucha latencia downstream o pantallas muy caras de construir en tiempo real.

## 11. Beneficios En Acme

Claridad:

```text
CartCommandHandler modifica
CartQueryHandler lee
```

Testing:

```text
puedes testear commands y queries por separado
```

Metricas:

```text
cart_commands_total
cart_queries_total
cart_query_latency
```

Evolucion:

Mas adelante, commands podrian:

- publicar eventos
- persistir en DB
- emitir outbox events

Queries podrian:

- leer read model optimizado
- usar cache
- leer ElasticSearch

## 12. Desventajas

CQRS agrega:

- mas clases
- mas estructura
- mas decisiones
- mas testing
- posible consistencia eventual si read/write se separan fisicamente

Para CRUD simple puede ser overengineering.

Ejemplo donde no lo forzaria:

- `category-service` simple
- CRUD basico de roles
- endpoints admin de baja complejidad

## 13. Records En CQRS

`record` en Java es ideal para Commands y Queries simples.

Ejemplo:

```java
public record AddItemToCartCommand(UUID userId, UUID productId, int quantity){}
```

Ventajas:

- inmutable
- constructor automatico
- accessors automaticos: `userId()`, `productId()`
- `equals`, `hashCode`, `toString`
- menos boilerplate

No usar `record` para entidades JPA mutables.

## 14. Observaciones De Mejora En Acme

### Request DTO vs Command

En `CartController`, `addItem` recibe `AddItemRequestDTO`, pero `setQty` recibe directamente `SetItemQuantityCommand`.

```java
public ResponseEntity<Void> setQty(@Valid @RequestBody SetItemQuantityCommand setItemQuantityCommand)
```

Para separar API externa de modelo interno, seria mas limpio:

```text
SetItemQuantityRequestDTO
  -> SetItemQuantityCommand
```

Asi el command queda como intencion interna, no como contrato HTTP publico.

### Query N+1 En Summary

`GetCartSummaryQuery` llama product-service por cada item:

```java
productGatewayService.getById(productId)
```

Si el carrito tiene 20 productos:

```text
20 llamadas a product-service
```

Mejora:

```text
batch endpoint
getProductsByIds(List<UUID>)
```

Esto conecta con la leccion de `catalog-query-service`.

### TTL Del Carrito

Commands escriben en Redis, pero conviene definir TTL del carrito.

Ejemplo:

```text
cart:{userId} expira en 7/30 dias
```

Depende del producto.

### Persistencia

Si Redis es store principal del carrito, decidir:

- persistencia Redis habilitada?
- que pasa si Redis se pierde?
- se acepta perder carrito?
- se sincroniza a DB?

Para ecommerce, muchos carritos pueden ser temporales.

Para banking, no aplicaria esta ligereza a datos financieros.

### Idempotencia En Commands

En carrito, repetir un `AddItemToCartCommand` normalmente incrementa cantidad otra vez.

Eso puede ser aceptable porque el usuario esta agregando items.

Pero en dominios como pagos, ordenes o reservas, un command repetido puede ser peligroso.

Ejemplo:

```text
CreatePaymentCommand repetido -> no debe cobrar dos veces
ReserveStockCommand repetido   -> no debe reservar dos veces
```

Para esos casos se usa:

- `Idempotency-Key`
- tabla de processed requests/events
- constraint unico
- respuesta cacheada por key
- control transaccional

Esta diferencia es buena para entrevista:

> CQRS separa comandos y consultas, pero no resuelve idempotencia automaticamente. La idempotencia es una regla adicional del command side.

## 15. Paso a Paso Para Implementar CQRS Ligero

### Paso 1. Separar paquetes

```text
cqrs/
  command/
  query/
```

### Paso 2. Crear commands

```java
public record AddItemToCartCommand(UUID userId, UUID productId, int quantity){}
```

### Paso 3. Crear command handler

```java
@Service
public class CartCommandHandler {
    public void handle(AddItemToCartCommand command) {
        ...
    }
}
```

### Paso 4. Crear queries

```java
public record GetCartQuery(UUID userId){}
```

### Paso 5. Crear query handler

```java
@Service
public class CartQueryHandler {
    public CartResponseDTO handle(GetCartQuery query) {
        ...
    }
}
```

### Paso 6. Controller delega

```java
@PostMapping("/items")
public ResponseEntity<Void> add(...) {
    command.handle(new AddItemToCartCommand(...));
    return ResponseEntity.accepted().build();
}

@GetMapping
public CartResponseDTO getCart() {
    return query.handle(new GetCartQuery(...));
}
```

### Paso 7. Agregar metricas

```text
cart_commands_total
cart_queries_total
cart_query_latency
```

### Paso 8. Decidir Si Necesita Read Model

No todos los CQRS necesitan read model separado.

Preguntas:

- La query llama muchos servicios?
- La query es lenta?
- El volumen de lectura es mucho mayor que escritura?
- La pantalla necesita datos denormalizados?
- Se acepta consistencia eventual?

Si la respuesta es si, puedes evolucionar hacia read model.

Si la respuesta es no, CQRS ligero puede ser suficiente.

## 16. Version Corta Para Entrevista

> CQRS separates write operations from read operations. In Acme Shop, Cart Service uses command handlers for cart mutations and query handlers for cart reads, which makes the intent clearer and allows independent metrics and evolution of reads and writes.

## 17. Version Senior

> I use CQRS when read and write paths have different requirements. In Acme, cart commands modify Redis while queries build cart views and summaries. I would not force CQRS everywhere; for simple CRUD it adds unnecessary complexity. For more advanced systems, commands can emit events and queries can read from optimized read models.

## 18. Que No Decir

No diria:

CQRS es obligatorio en microservicios.

CQRS y Event Sourcing son lo mismo.

CQRS siempre necesita dos bases de datos.

Todo endpoint GET/POST ya es CQRS enterprise.

Un query puede modificar estado sin problema.

CQRS elimina la necesidad de transacciones.

## 19. Fuentes Utiles

- Martin Fowler CQRS: https://martinfowler.com/bliki/CQRS.html
- Microsoft CQRS pattern: https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs
