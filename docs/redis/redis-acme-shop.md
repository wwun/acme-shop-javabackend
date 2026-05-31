# Redis en Acme Shop

Este documento consolida las notas de Redis y las alinea con la implementacion real de Acme Shop.

La idea importante: Redis no tiene un solo uso. En este proyecto aparece en dos roles distintos:

- `product-service` y `catalog-query-service`: Redis como cache de lectura.
- `cart-service`: Redis como store temporal de estado del carrito.

Esa diferencia es clave para entrevistas.

## Como Usar Este Documento

Este documento tiene tres niveles:

```text
Secciones 1-20:
  Entender Redis, revisar como se usa en Acme Shop y preparar respuestas de entrevista.

Secciones 21-23:
  Replicar rapido Redis en otro servicio con paso a paso.

Secciones 24-26:
  Conversacion senior/enterprise: usos avanzados, riesgos y decisiones de produccion.
```

No todo lo descrito aqui debe implementarse en cada servicio.

Regla practica:

```text
Si solo quieres acelerar lecturas:
  usa RedisCacheManager.

Si Redis guarda estado temporal mutable:
  usa RedisTemplate.

Si el flujo es financiero, de inventario definitivo o ledger:
  Redis puede ayudar, pero no debe ser la fuente de verdad.
```

## 1. Que es Redis

Redis es un data store en memoria, key-value, de muy baja latencia.

Se usa mucho para:

| Caso | Ejemplo |
| --- | --- |
| Cache | productos, catalogo, resultados de queries |
| Estado temporal | carritos, sesiones, drafts |
| Rate limiting | limite por usuario/IP |
| Contadores | views, likes, metrics temporales |
| Pub/Sub | notificaciones simples |
| Locks | coordinacion distribuida |
| Rankings | leaderboards con sorted sets |

Redis vive principalmente en RAM. Por eso es rapido, pero tambien obliga a pensar en TTL, memoria, eviction y disponibilidad.

## 2. Redis No Siempre Significa Cache

En Acme Shop hay dos usos diferentes.

### Product/Catalog: Redis como cache

Fuente de verdad:

```text
PostgreSQL / Supabase
```

Redis guarda copias temporales para acelerar lecturas.

Si Redis se borra:

```text
No se pierde data de negocio.
El siguiente request vuelve a consultar la BD o servicios fuente.
Redis se repuebla.
```

### Cart: Redis como store temporal

Fuente activa del carrito:

```text
Redis
```

El carrito es estado temporal y mutable.

Si Redis se borra:

```text
Se pierden carritos activos, salvo que exista persistencia o backup.
```

Esto cambia el diseno. Para cache usas abstracciones de cache. Para carrito necesitas operar estructuras Redis directamente.

Frase de entrevista:

> In product-service Redis is a disposable read cache. In cart-service Redis is the backing store for active cart state, so I use RedisTemplate and hashes for granular updates.

## 3. RedisTemplate vs RedisCacheManager

### RedisTemplate

`RedisTemplate` es una API de bajo nivel.

Permite trabajar directamente con estructuras Redis:

```java
redisTemplate.opsForValue()
redisTemplate.opsForHash()
redisTemplate.opsForList()
redisTemplate.opsForSet()
redisTemplate.opsForZSet()
```

Ventajas:

```text
Mas control.
Puedes usar hashes, increments, deletes, TTL por key, operaciones atomicas.
```

Desventajas:

```text
Mas codigo repetitivo.
Debes manejar keys, get, set, delete, serializers y expiracion manualmente.
```

Uso en Acme Shop:

```text
cart-service
```

Porque el carrito necesita operaciones granulares:

```java
redisTemplate.opsForHash().increment("cart:" + userId, productId, quantity);
redisTemplate.opsForHash().put("cart:" + userId, productId, quantity);
redisTemplate.opsForHash().delete("cart:" + userId, productId);
redisTemplate.opsForHash().entries("cart:" + userId);
```

### RedisCacheManager

`RedisCacheManager` integra Redis con Spring Cache.

Permite usar:

```java
@Cacheable
@CacheEvict
@CachePut
@Caching
```

Ventajas:

```text
Menos codigo repetitivo.
Spring maneja cache hit, cache miss, set, keys basicas, TTL e invalidacion declarativa.
```

Desventajas:

```text
Menos control fino.
No es ideal cuando Redis es el store principal o necesitas estructuras especificas.
```

Uso en Acme Shop:

```text
product-service
catalog-query-service
```

Porque esos servicios cachean resultados de lectura.

## 4. Product Service

`product-service` usa Redis como cache de lectura.

Archivo:

[RedisConfig.java](/home/william/Documents/portfolio/acme-shop/product-service/src/main/java/com/wwun/acme/product/redis/RedisConfig.java)

Caches:

| Cache | Key | TTL | Uso |
| --- | --- | --- | --- |
| `productById` | `productId` | 300s | Producto individual |
| `productsAll` | `ALL` | 60s | Lista completa |

Configuracion:

```java
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConf = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );

        Map<String, RedisCacheConfiguration> config = new HashMap<>();
        config.put("productById", defaultConf.entryTtl(Duration.ofSeconds(300)));
        config.put("productsAll", defaultConf.entryTtl(Duration.ofSeconds(60)));

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConf)
            .withInitialCacheConfigurations(config)
            .build();
    }
}
```

### Cache individual

```java
@Cacheable(cacheNames = "productById", key = "#productId")
public ProductResponseDTO findById(UUID productId) {
    ...
}
```

Flujo:

```text
1. Spring busca productById::<productId> en Redis.
2. Si existe, devuelve cache.
3. Si no existe, ejecuta el metodo.
4. Guarda resultado en Redis.
5. Devuelve respuesta.
```

### Cache de lista

```java
@Cacheable(cacheNames = "productsAll", key = "'ALL'")
public List<ProductResponseDTO> findAll() {
    ...
}
```

Como `findAll` no recibe parametros, se usa key fija:

```text
productsAll::ALL
```

### Invalidacion

Cuando se crea, actualiza o elimina un producto, hay que invalidar caches.

Ejemplo:

```java
@Caching(evict = {
    @CacheEvict(cacheNames = "productsAll", allEntries = true),
    @CacheEvict(cacheNames = "productById", key = "#productId")
})
public void delete(UUID productId) {
    ...
}
```

Por que dos caches:

```text
productsAll:
  La lista completa puede cambiar.

productById:
  Solo se borra el producto afectado.
```

Esto evita borrar todos los productos individuales y reduce impacto.

## 5. Catalog Query Service

`catalog-query-service` compone:

```text
product-service + inventory-service
```

Y devuelve una vista lista para frontend:

```text
producto + categoria + availability
```

Archivo:

[RedisConfig.java](/home/william/Documents/portfolio/acme-shop/catalog-query-service/src/main/java/com/wwun/acme/catalog/redis/RedisConfig.java)

Cache:

| Cache | TTL | Uso |
| --- | --- | --- |
| `catalogProductsByIds` | 30s | Resultado compuesto por batch de productIds |

El TTL es corto porque incluye disponibilidad de inventario, y esa informacion puede cambiar mas rapido que datos de catalogo.

### KeyGenerator para batch

Cuando un metodo recibe una lista de IDs, hay un detalle importante:

```text
[A, B] y [B, A] representan el mismo conjunto de productos.
```

Sin normalizacion, Redis podria crear dos claves diferentes.

Por eso se usa un `KeyGenerator` que:

```text
1. elimina nulls
2. convierte ids a string
3. elimina duplicados
4. ordena
5. une con coma
```

Asi:

```text
[B, A, A] -> A,B
[A, B]    -> A,B
```

### Nota de consistencia

Catalog cache no decide reservas.

```text
Catalog:
  muestra disponibilidad aproximada/reciente.

Inventory:
  decide reservas reales.
```

Frase de entrevista:

> I cache composed catalog responses with a short TTL because they include availability. Redis improves read latency, but inventory remains the source of truth for stock reservation.

## 6. Cart Service

`cart-service` usa Redis como store temporal del carrito.

Archivo:

[RedisConfig.java](/home/william/Documents/portfolio/acme-shop/cart-service/src/main/java/com/wwun/acme/cart/redis/RedisConfig.java)

Configuracion:

```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(redisConnectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    return template;
}
```

Modelo:

```text
key:
  cart:<userId>

hash fields:
  productId -> quantity
```

Ejemplo conceptual:

```text
cart:b3a5...
  aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa -> 2
  bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb -> 1
```

### Por que Hash

Con `opsForValue`, tendrias que guardar todo el carrito como un JSON:

```text
leer carrito completo
modificar item
reescribir carrito completo
```

Con `opsForHash`, modificas un producto puntual:

```java
redisTemplate.opsForHash().increment(cartKey, productId, quantity);
```

Esto es mas granular y natural para carrito.

### Commands

Archivo:

[CartCommandHandler.java](/home/william/Documents/portfolio/acme-shop/cart-service/src/main/java/com/wwun/acme/cart/cqrs/command/CartCommandHandler.java)

Operaciones:

```java
increment(...)
put(...)
delete(...)
redisTemplate.delete(...)
```

Esto muta Redis directamente.

### Queries

Archivo:

[CartQueryHandler.java](/home/william/Documents/portfolio/acme-shop/cart-service/src/main/java/com/wwun/acme/cart/cqrs/query/CartQueryHandler.java)

`getCart` lee el hash:

```java
redisTemplate.opsForHash().entries(key(userId));
```

`getSummary` lee el carrito y consulta productos para precio/nombre.

Nota: si el carrito crece mucho, `getSummary` puede caer en N+1 calls contra product-service. Una mejora seria usar batch product lookup.

## 7. Estructuras Redis

| Estructura | Uso | Ejemplo |
| --- | --- | --- |
| String | valor simple | cache simple, token, contador |
| Hash | objeto/campos | carrito `productId -> quantity` |
| List | secuencia ordenada | colas simples, historial |
| Set | unicos sin orden | favoritos, tags |
| Sorted Set | ranking por score | leaderboard, top products |

Regla practica:

```text
Cache de resultado completo -> Spring Cache / RedisCacheManager
Estado mutable con operaciones finas -> RedisTemplate + estructura adecuada
```

## 8. TTL e Invalidacion

TTL significa que una key expira automaticamente.

Se usa para:

```text
evitar datos viejos eternos
controlar memoria
permitir recuperacion natural de cache
```

Invalidacion se usa cuando una escritura cambia la fuente de verdad.

Ejemplo:

```text
update product
  -> guardar en DB
  -> evict productById
  -> evict productsAll
```

Eleccion de TTL:

| Dato | TTL sugerido |
| --- | --- |
| Catalogo estable | minutos |
| Lista general | 30-60s |
| Disponibilidad/stock visible | corto, 5-30s |
| Carrito | depende de negocio, horas/dias |

## 9. Fallos Reales y Lecciones

### SerializationException en productsAll

Problema observado:

```text
Primer request funcionaba.
Segundo request fallaba.
```

Lectura:

```text
Cache miss funcionaba.
Cache hit fallaba.
```

Causa:

```text
Stream.toList() puede devolver ImmutableCollections$ListN.
Ese tipo interno genero problemas al deserializar con GenericJackson2JsonRedisSerializer.
```

Solucion aplicada:

```java
List<ProductResponseDTO> dtos = new ArrayList<>(
    productRepository.findAll()
        .stream()
        .map(productMapper::toResponseDTO)
        .toList()
);
```

Leccion:

> En cache no solo importa el dato logico. Tambien importa el tipo concreto que serializas.

### GenericJackson2JsonRedisSerializer

Explicacion precisa:

```text
Conceptualmente convierte objetos Java a JSON.
Dependiendo del serializer/configuracion, el JSON puede incluir metadata de tipo para poder deserializar objetos genericos.
```

No siempre esperes JSON plano como:

```json
{"name":"Smartphone X"}
```

Puede incluir metadata extra.

## 10. Cache Problems: Stampede, Avalanche y Penetration

### Cache Stampede

Muchos requests piden la misma key justo cuando expira.

Resultado:

```text
todos van a la DB al mismo tiempo
```

Mitigaciones:

```text
TTL con jitter
locking por key
single-flight
pre-warming
background refresh
```

### Cache Avalanche

Muchas keys expiran al mismo tiempo.

Mitigaciones:

```text
TTL escalonado/jitter
no usar el mismo TTL exacto para todo
pre-warming
limites de concurrencia
```

### Cache Penetration

Requests repetidos por datos que no existen.

Ejemplo:

```text
GET productId inexistente muchas veces
```

Mitigaciones:

```text
cachear negativos con TTL corto
Bloom filters
rate limiting
validacion temprana
```

## 11. Que Pasa Si Redis Cae

Depende del rol.

### Si Redis es cache

Ejemplo:

```text
product-service
catalog-query-service
```

Comportamiento deseable:

```text
fallback a DB o servicios fuente
mayor latencia
mas carga en dependencias
no se pierde informacion de negocio
```

### Si Redis es store temporal

Ejemplo:

```text
cart-service
```

Comportamiento:

```text
puede perderse o quedar inaccesible el carrito activo
```

Opciones production:

```text
Redis persistence
replication
managed Redis
backup/restore
degradacion clara al usuario
```

## 12. Persistence: RDB y AOF

Redis puede persistir datos, pero no siempre hace falta.

| Modo | Que hace | Uso |
| --- | --- | --- |
| RDB | snapshots cada cierto tiempo | cache, datos reconstruibles |
| AOF | append-only log de escrituras | menos perdida, estado importante |
| Hybrid | combina ambos | produccion con mas seguridad |

Para cache pura, perder Redis suele ser aceptable.

Para carritos, depende del negocio:

```text
si perder carritos es aceptable -> Redis temporal
si no es aceptable -> persistencia, replica o backing store adicional
```

## 13. Eviction y Memoria

Redis tiene memoria finita.

Debes decidir:

```text
maxmemory
maxmemory-policy
```

Politicas comunes:

| Politica | Idea |
| --- | --- |
| noeviction | devuelve error si no hay memoria |
| allkeys-lru | elimina menos recientemente usadas |
| allkeys-lfu | elimina menos frecuentemente usadas |
| volatile-ttl | elimina keys con TTL |

Para cache, LRU/LFU suelen tener sentido.

Para datos que no se pueden perder, `noeviction` puede ser mejor, pero debes monitorear memoria.

## 14. Observabilidad Redis

Metricas importantes:

```text
cache hit ratio
cache misses
latency
memory used
evictions
expired keys
connected clients
command rate
errors/timeouts
```

En Acme Shop:

```text
Micrometer + Actuator + Prometheus
```

Spring Boot puede exponer metricas de Redis/cache si esta configurado con Actuator/Micrometer.

Frase de entrevista:

> I would monitor Redis hit ratio, latency, evictions, memory usage and command errors. A cache with low hit ratio or high eviction rate may be adding complexity without enough value.

## 15. Redis y Stock

Para ecommerce, hay una pregunta delicada:

```text
Se cachea stock?
```

Respuesta senior:

```text
Se puede cachear disponibilidad para lectura, pero no usar cache como autoridad final para reservar stock.
```

En Acme Shop:

```text
Catalog puede mostrar availability cacheada por pocos segundos.
Inventory decide la reserva real.
```

No venderia Redis cache de stock como decision final de compra, salvo que implementes un modelo especifico con operaciones atomicas, locks/Lua, reconciliacion y fuente de verdad clara.

Frase de entrevista:

> I can cache stock availability for read UX, but reservation must be validated against the inventory source of truth using transactional or atomic mechanisms.

## 16. Distributed Locks

Redis puede usarse para locks distribuidos.

Ejemplo:

```text
lock:stock:<productId>
```

Pero hay que tener cuidado:

```text
TTL del lock
release seguro
timeouts
fallos de red
Redlock si hay multiples nodos
```

Para un sistema bancario o pagos, no usaria locks Redis como primera defensa para consistencia contable. Preferiria:

```text
transacciones DB
idempotency keys
unique constraints
ledger append-only
outbox
```

Redis lock puede ayudar, pero no reemplaza garantias del modelo de datos.

## 17. Checklist Para Agregar Redis a un Servicio

### Si es cache de lectura

```text
1. Agregar spring-boot-starter-data-redis.
2. Agregar @EnableCaching.
3. Configurar RedisCacheManager.
4. Definir cache names y TTL por cache.
5. Usar @Cacheable en queries.
6. Usar @CacheEvict en writes.
7. Definir comportamiento si Redis cae.
8. Medir hit ratio, latency y evictions.
```

### Si es store temporal

```text
1. Agregar RedisTemplate.
2. Elegir estructura: Hash, String, Set, SortedSet.
3. Definir naming de keys.
4. Definir TTL si aplica.
5. Definir persistencia o perdida aceptable.
6. Agregar metricas.
7. Agregar estrategia de fallback.
```

## 18. Comandos Utiles

Entrar al CLI:

```bash
docker exec -it redis redis-cli
```

Ping:

```bash
ping
```

Listar keys:

```bash
keys *
```

Mejor para produccion/dev con muchas keys:

```bash
scan 0
```

Buscar productos:

```bash
--scan --pattern '*products*'
```

Ver tipo:

```bash
type key
```

Ver TTL:

```bash
ttl key
```

Leer string:

```bash
get key
```

Leer hash:

```bash
hgetall key
```

## 19. Que Decir En Entrevista

Version corta:

> I use Redis in two different ways. For products and catalog, Redis is a disposable read cache through Spring Cache and RedisCacheManager, with TTL and explicit eviction on writes. For carts, Redis is the temporary state store, so I use RedisTemplate with hashes for granular productId-to-quantity updates.

Version senior:

> I do not treat Redis as a generic magic performance layer. I first define whether Redis is a cache or a source of temporary state. For cache, I care about TTL, invalidation, stale data, hit ratio and fallback when Redis is unavailable. For mutable state like carts, I choose Redis data structures intentionally, define key naming, TTL/persistence expectations, and monitor memory, latency and evictions. For stock or payments, Redis can help with reads or coordination, but the source of truth and consistency guarantees must live in inventory/ledger transactions and idempotent workflows.

## 20. Mejoras Pendientes En Acme Shop

- Agregar TTL al carrito si el negocio lo requiere.
- Revisar `cart-service` para evitar N+1 calls en summary usando product batch endpoint.
- Revisar si `catalog-query-service` debe cachear availability por 30s o menos.
- Agregar metricas especificas de cache hit/miss si Actuator no da suficiente detalle.
- Definir comportamiento explicito si Redis esta caido.
- Configurar maxmemory/eviction policy en entornos reales.
- Para banking/payment system, usar Redis con mucho cuidado: rate limiting, idempotency helper o cache, pero no como ledger.

## 21. Paso a Paso: Redis Como Cache Con RedisCacheManager

Usa este flujo cuando:

```text
El dato vive en una base de datos o servicio fuente.
Redis solo acelera lecturas.
Si Redis se borra, no pierdes informacion de negocio.
```

Ejemplos:

```text
product-service
catalog-query-service
category-service
read-only views
```

### Paso 1. Agregar dependencia

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

Si el proyecto no trae cache starter transitivamente, agregar:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

### Paso 2. Configurar conexion

En config del servicio:

```yaml
spring:
  data:
    redis:
      host: redis
      port: 6379
      timeout: 6000
```

En Docker Compose, el host es el nombre del servicio:

```yaml
redis:
  image: redis:7
  container_name: redis
  ports:
    - "6379:6379"
```

### Paso 3. Crear RedisConfig

```java
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConf = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );

        Map<String, RedisCacheConfiguration> config = new HashMap<>();
        config.put("entityById", defaultConf.entryTtl(Duration.ofMinutes(5)));
        config.put("entitiesAll", defaultConf.entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConf)
            .withInitialCacheConfigurations(config)
            .build();
    }
}
```

### Paso 4. Cachear queries

Por ID:

```java
@Cacheable(cacheNames = "entityById", key = "#id")
public EntityResponseDTO findById(UUID id) {
    ...
}
```

Lista:

```java
@Cacheable(cacheNames = "entitiesAll", key = "'ALL'")
public List<EntityResponseDTO> findAll() {
    ...
}
```

### Paso 5. Invalidar en escrituras

Create:

```java
@CacheEvict(cacheNames = "entitiesAll", allEntries = true)
public Entity save(CreateRequestDTO dto) {
    ...
}
```

Update/delete:

```java
@Caching(evict = {
    @CacheEvict(cacheNames = "entitiesAll", allEntries = true),
    @CacheEvict(cacheNames = "entityById", key = "#id")
})
public void delete(UUID id) {
    ...
}
```

### Paso 6. Elegir TTL

Regla practica:

```text
Dato estable:
  TTL mas largo.

Dato que cambia seguido:
  TTL corto.

Dato con impacto fuerte si esta stale:
  TTL muy corto o no cachear.
```

Ejemplos:

```text
productById:
  5 min

productsAll:
  1 min

catalog availability:
  5-30s
```

### Paso 7. Probar

```bash
docker exec -it redis redis-cli
```

Buscar keys:

```bash
--scan --pattern '*product*'
```

Ver TTL:

```bash
ttl key
```

Prueba funcional:

```text
1. Primera request -> cache miss.
2. Segunda request -> cache hit.
3. Update/delete -> evict.
4. Siguiente request -> cache miss y repopula.
```

## 22. Paso a Paso: Redis Como Store Temporal Con RedisTemplate

Usa este flujo cuando:

```text
Redis no solo acelera lecturas.
Redis guarda estado temporal activo.
Necesitas modificar estructuras de forma granular.
```

Ejemplos:

```text
cart-service
drafts
temporary sessions
rate limiting counters
short-lived workflow state
```

### Paso 1. Agregar dependencia

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Paso 2. Configurar RedisTemplate

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

### Paso 3. Definir modelo de keys

Ejemplo carrito:

```text
cart:<userId>
```

Hash:

```text
productId -> quantity
```

Ejemplo:

```text
cart:b3a5
  product-a -> 2
  product-b -> 1
```

### Paso 4. Escribir commands

Agregar:

```java
redisTemplate.opsForHash()
    .increment(key(userId), productId.toString(), quantity);
```

Setear cantidad:

```java
redisTemplate.opsForHash()
    .put(key(userId), productId.toString(), quantity);
```

Eliminar item:

```java
redisTemplate.opsForHash()
    .delete(key(userId), productId.toString());
```

Limpiar carrito:

```java
redisTemplate.delete(key(userId));
```

### Paso 5. Leer queries

```java
Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(userId));
```

Convertir:

```java
entries.entrySet().stream()
    .map(entry -> new CartItemResponseDTO(
        UUID.fromString((String) entry.getKey()),
        ((Number) entry.getValue()).intValue()
    ))
    .toList();
```

### Paso 6. Agregar TTL si aplica

Para carritos puedes definir expiracion:

```java
redisTemplate.expire(key(userId), Duration.ofDays(7));
```

Decision de negocio:

```text
Carrito anonimo:
  TTL corto/medio.

Carrito usuario logueado:
  TTL mas largo.

Carrito critico:
  considerar persistencia/backing store.
```

### Paso 7. Medir

Metricas utiles:

```text
cart_commands_total
cart_queries_total
cart_query_latency
redis command latency
redis errors
memory usage
```

## 23. Decision Guide: Que Opcion Elegir

| Necesidad | Opcion |
| --- | --- |
| Cachear una consulta de BD | `RedisCacheManager` + `@Cacheable` |
| Invalidar despues de update/delete | `@CacheEvict` |
| Guardar carrito mutable | `RedisTemplate` + Hash |
| Contador con expiracion | `RedisTemplate` + String/increment + TTL |
| Rate limiting | Redis counter + TTL o bucket algorithm |
| Ranking/top items | Sorted Set |
| IDs unicos temporales | Set |
| Lock distribuido | SET NX PX / libreria, con cuidado |
| Search/facets | Mejor OpenSearch/Elasticsearch, no Redis basico |
| Ledger/pagos | No usar Redis como fuente de verdad |

Regla rapida:

```text
Si Redis es una copia descartable:
  RedisCacheManager.

Si Redis es el lugar donde mutas estado temporal:
  RedisTemplate.

Si necesitas consistencia financiera:
  DB transaccional / ledger / idempotency / outbox.
```

## 24. Usos Senior/Enterprise de Redis

Estos usos son buenos para conocer y mencionar con criterio, pero no todos deben implementarse en Acme.

### Rate limiting

Redis es comun para limitar requests por usuario/IP/token.

Ejemplo:

```text
rate:user:<userId>:minute
increment + TTL 60s
```

Uso real:

```text
proteger login
proteger checkout
proteger APIs publicas
```

Para entrevistas:

> I would use Redis for distributed rate limiting because counters with TTL are fast and shared across service instances.

### Idempotency helper

Redis puede ayudar a guardar respuestas temporales por idempotency key.

Ejemplo:

```text
idempotency:<userId>:<key>
```

Pero en pagos/banca, la garantia fuerte debe estar en DB:

```text
unique constraint
transaction
payment/order state machine
```

Redis puede ser una capa rapida, no la unica garantia.

### Distributed locks

Redis puede coordinar acceso a recursos.

Pero en sistemas criticos:

```text
usar con cuidado
TTL obligatorio
release seguro
evitar que reemplace transacciones DB
```

Para stock puede ser util en algunos disenos.

Para ledger bancario, no lo pondria como garantia principal.

### Session/token blacklist

Redis puede guardar:

```text
revoked tokens
refresh token ids
session ids
```

Uso:

```text
logout
revocacion
rotacion de refresh token
```

### Pub/Sub y Streams

Redis Pub/Sub sirve para notificaciones simples.

Redis Streams puede modelar eventos, pero si ya tienes Kafka:

```text
Kafka:
  eventos durables, replay, consumer groups robustos

Redis Pub/Sub:
  notificacion liviana, no ideal como event backbone principal
```

En Acme ya usas Kafka para eventos de negocio, asi que no usaria Redis para reemplazar Kafka.

### Leaderboards / rankings

Sorted Sets son muy buenos para:

```text
top products
top sellers
ranking de busquedas
scoreboards
```

No es core para Acme, pero es una estructura clasica de Redis.

## 25. Version Corta Para Entrevista

> I use Redis differently depending on the responsibility. For read-heavy catalog data, I use Redis through Spring Cache and RedisCacheManager, with TTL and eviction on writes. For active cart state, I use RedisTemplate with hashes because I need granular updates like productId-to-quantity increments. I avoid using Redis as the final source of truth for inventory reservation, payments, or ledger data. In production I would monitor hit ratio, latency, memory, evictions, and define fallback behavior when Redis is unavailable.

## 26. Version Production-Level

Para empresas grandes, Redis debe venir con decisiones explicitas:

```text
1. Que problema resuelve?
2. Es cache o fuente de estado temporal?
3. Que pasa si Redis cae?
4. Que TTL tiene?
5. Que eviction policy aplica?
6. Que metricas se monitorean?
7. Como se invalida?
8. Puede haber stale data?
9. Cual es la fuente de verdad?
10. Hay riesgo financiero/consistencia fuerte?
```

Respuesta senior:

> I would not introduce Redis just because it is fast. I would define the consistency model first. If Redis is only a cache, the system must tolerate misses and rebuild from the source of truth. If Redis stores temporary state, I need TTL, persistence expectations and failure handling. For financial workflows, Redis can support rate limiting or idempotency acceleration, but the durable guarantee belongs in the transactional database and event/outbox flow.
