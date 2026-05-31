# MapStruct En Acme Shop

Este documento consolida las notas de MapStruct y las conecta con la implementacion real de Acme Shop.

MapStruct es una libreria para convertir objetos Java en compile-time.

En Acme se usa principalmente para:

```text
Request DTO -> Entity
Entity      -> Response DTO
Entity      -> payload/hash DTO
```

La regla mental:

```text
MapStruct transforma datos.
El service decide reglas de negocio.
```

## 1. Donde Se Usa En Acme

Ejemplos reales:

- [ProductMapper.java](/home/william/Documents/portfolio/acme-shop/product-service/src/main/java/com/wwun/acme/product/mapper/ProductMapper.java)
- [CategoryMapper.java](/home/william/Documents/portfolio/acme-shop/product-service/src/main/java/com/wwun/acme/product/mapper/CategoryMapper.java)
- [OrderMapper.java](/home/william/Documents/portfolio/acme-shop/order-service/src/main/java/com/wwun/acme/order/mapper/OrderMapper.java)
- [OrderItemMapper.java](/home/william/Documents/portfolio/acme-shop/order-service/src/main/java/com/wwun/acme/order/mapper/OrderItemMapper.java)
- [UserMapper.java](/home/william/Documents/portfolio/acme-shop/user-service/src/main/java/com/wwun/acme/user/mapper/UserMapper.java)
- [InventoryMapper.java](/home/william/Documents/portfolio/acme-shop/inventory-service/src/main/java/com/wwun/acme/inventory/mapper/InventoryMapper.java)
- [StockMovementMapper.java](/home/william/Documents/portfolio/acme-shop/inventory-service/src/main/java/com/wwun/acme/inventory/mapper/StockMovementMapper.java)

Todos usan la forma correcta para Spring:

```java
@Mapper(componentModel = "spring")
public interface InventoryMapper {
    InventoryResponseDTO toResponseDTO(Inventory inventory);
}
```

Eso hace que MapStruct genere una implementacion como bean de Spring.

Por eso luego puedes inyectar el mapper en controllers o services.

## 2. Por Que No Usar INSTANCE En Spring

En ejemplos simples de internet aparece esto:

```java
ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);
```

En proyectos Spring Boot no conviene.

En Acme se usa:

```java
@Mapper(componentModel = "spring")
```

Ventajas:

- Spring administra el mapper como bean
- se puede inyectar por constructor
- funciona con otros mappers en `uses`
- es mas consistente con testing y arquitectura Spring

## 3. Configuracion Maven

En el parent `pom.xml` se centralizan versiones:

```xml
<dependency>
  <groupId>org.mapstruct</groupId>
  <artifactId>mapstruct</artifactId>
  <version>1.6.3</version>
</dependency>

<dependency>
  <groupId>org.mapstruct</groupId>
  <artifactId>mapstruct-processor</artifactId>
  <version>1.6.3</version>
  <scope>provided</scope>
</dependency>
```

Cuando se usa Lombok junto con MapStruct, tambien aparece:

```xml
<artifactId>lombok-mapstruct-binding</artifactId>
```

Esto ayuda a que MapStruct entienda mejor codigo generado por Lombok durante annotation processing.

## 4. Modelo Mental

MapStruct:

- genera codigo en compile-time
- no usa reflection en runtime
- no consulta la base de datos
- no valida reglas de negocio
- no resuelve relaciones por si solo
- no deberia crear ids, fechas ni decisiones de dominio

Si un campo existe en la entity pero no existe en el DTO:

```text
MapStruct no falla.
Ese campo queda con su valor por defecto.
```

Ejemplo:

```text
DTO:    items
Entity: id, userId, orderDate, total, items
```

Si el mapper solo recibe el DTO, campos como `id`, `userId`, `orderDate` y `total` no deberian venir del cliente.

En Acme se completan en el service.

## 5. `source` Y `target`

En MapStruct:

```java
@Mapping(source = "a", target = "b")
```

Significa:

```text
origen.a -> destino.b
```

Si los nombres coinciden, no necesitas `@Mapping`.

Ejemplo:

```text
product.name -> productResponseDTO.name
```

MapStruct lo resuelve solo.

## 6. `ignore = true`

`ignore = true` significa exactamente:

```text
MapStruct: no generes codigo para asignar este campo.
```

Ejemplo real:

[OrderMapper.java](/home/william/Documents/portfolio/acme-shop/order-service/src/main/java/com/wwun/acme/order/mapper/OrderMapper.java)

```java
@Mappings({
    @Mapping(target = "id", ignore = true),
    @Mapping(target = "userId", ignore = true),
    @Mapping(target = "orderDate", ignore = true),
    @Mapping(target = "total", ignore = true),
    @Mapping(target = "requestHash", ignore = true),
    @Mapping(target = "idempotencyKey", ignore = true)
})
Order toEntity(OrderCreateRequestDTO orderCreateRequestDTO);
```

Esto NO significa:

- que JPA llene el campo en ese momento
- que la base de datos actue ahi
- que el campo se autogenere en el mapper
- que MapStruct valide algo

Solo significa:

```text
no hagas entity.setId(...)
no hagas entity.setUserId(...)
no hagas entity.setOrderDate(...)
```

## 7. Por Que `ignore = true` Es Buena Practica

A veces no es estrictamente obligatorio.

Si el DTO no tiene `id`, MapStruct no lo va a copiar.

Pero `ignore = true` documenta y protege la regla:

```text
este campo no debe venir del cliente
```

Sirve para evitar que manana alguien agregue `id` al DTO y MapStruct lo copie automaticamente.

Campos tipicos para ignorar:

- `id`
- `createdAt`
- `updatedAt`
- `orderDate`
- `userId`
- `total`
- `idempotencyKey`
- relaciones resueltas por service

## 8. Service Completa El Dominio

Ejemplo real:

[OrderServiceImpl.java](/home/william/Documents/portfolio/acme-shop/order-service/src/main/java/com/wwun/acme/order/service/OrderServiceImpl.java)

```java
Order order = orderMapper.toEntity(orderCreateRequestDTO);

order.setUserId(userId);
order.setIdempotencyKey(idempotencyKey);
order.setOrderDate(Instant.now());
```

Esto esta bien.

El mapper convierte datos del request.

El service completa:

- usuario autenticado
- idempotency key
- fecha
- total
- items con precio de compra
- hash
- status inicial
- evento outbox

Esa separacion es sana.

## 9. Relaciones Y `uses`

En Product:

[ProductMapper.java](/home/william/Documents/portfolio/acme-shop/product-service/src/main/java/com/wwun/acme/product/mapper/ProductMapper.java)

```java
@Mapper(componentModel = "spring", uses = CategoryMapper.class)
public interface ProductMapper {
    ProductResponseDTO toResponseDTO(Product product);
}
```

`uses = CategoryMapper.class` dice:

```text
si necesitas convertir Category, usa CategoryMapper
```

Eso sirve para mapping entre objetos ya cargados.

Pero MapStruct no busca la categoria en DB.

Por eso en ProductService:

```java
Category category = categoryService.findById(categoryId);
Product product = productMapper.toEntity(productCreateRequestDTO);
product.setCategory(category);
```

La relacion se resuelve en service, no en mapper.

## 10. Create Vs Update

Create:

```text
DTO -> entity nueva
```

Ejemplo:

```java
Product product = productMapper.toEntity(productCreateRequestDTO);
```

Update:

```text
buscar entity existente
actualizar campos permitidos
guardar
```

Para update, una buena practica con MapStruct es usar `@MappingTarget`:

```java
void updateEntityFromDTO(ProductUpdateRequestDTO dto, @MappingTarget Product product);
```

Eso evita crear una entity nueva y perder campos existentes.

En Acme hay una pista de esto en `ProductMapper`:

```java
// void updateEntityFromDTO(ProductUpdateRequestDTO productUpdateRequestDTO, @MappingTarget Product product);
```

Actualmente algunos updates se hacen manualmente en service, por ejemplo Product:

```java
existing.setName(productUpdateRequestDTO.getName());
existing.setPrice(productUpdateRequestDTO.getPrice());
existing.setCategory(categoryService.findById(productUpdateRequestDTO.getCategoryId()));
```

Eso tambien es valido si son pocos campos y hay reglas claras.

Pero para updates grandes, `@MappingTarget` reduce boilerplate.

## 11. Partial Update Y Nulls

Si haces PATCH o update parcial, necesitas decidir que hacer con nulls.

MapStruct permite:

```java
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
void update(@MappingTarget Product entity, ProductUpdateRequestDTO dto);
```

Eso evita que un null del DTO borre un valor existente.

Esto es distinto de PUT completo, donde null puede ser invalido o significar reemplazo total.

## 12. Mappers No Deben Tener Repositorios

No meter esto en un mapper:

```java
@Autowired ProductRepository repository;
```

Red flag.

El mapper no debe:

- consultar DB
- validar ownership
- calcular stock
- calcular total
- generar idempotency key
- publicar eventos

Eso pertenece al service, domain service o handler.

## 13. Testing

Los mappers normalmente no se mockean si quieres probar mapping real.

Opciones:

### Test Unitario Sin Spring

```java
OrderMapper mapper = Mappers.getMapper(OrderMapper.class);
```

Util para probar mapping puro.

### Test Con Spring

```java
@SpringBootTest
class OrderMapperTest {
    @Autowired OrderMapper mapper;
}
```

Mas cercano a tu configuracion real.

No necesitas testear cada campo trivial si no aporta.

Si el mapper tiene:

- `@Mapping` especial
- `ignore`
- nested DTOs
- `@MappingTarget`
- custom methods

ahi si vale la pena testear.

## 14. Lecciones Aprendidas En Acme

Buenas decisiones:

- usar `@Mapper(componentModel = "spring")`
- no usar `INSTANCE`
- ignorar campos sensibles/generados en `OrderMapper`
- resolver relaciones en services
- usar `uses` para mappers secundarios
- mapear entities a response DTOs en vez de devolver entities

Mejoras posibles:

- implementar `@MappingTarget` en updates grandes
- evitar comments largos en mappers y mover explicacion a docs
- agregar tests enfocados para mappers con reglas especiales
- usar `unmappedTargetPolicy = ReportingPolicy.ERROR` en mappers criticos si se quiere forzar explicitud
- revisar updates donde `toEntity(updateDto)` crea una entity nueva

## 15. Version Corta Para Entrevista

> I use MapStruct for DTO/entity mapping because it generates type-safe code at compile time and avoids reflection. I keep it limited to data transformation; business rules, relationships, ids and timestamps are handled in services.

## 16. Version Senior

> In Acme, MapStruct maps request DTOs to entities and entities to response DTOs. For sensitive fields like `id`, `userId`, `total`, `orderDate` or `idempotencyKey`, I ignore them in the mapper and set them in the service, because those values belong to the domain/application layer, not to the client payload. For updates, I prefer `@MappingTarget` when the entity already exists.

## 17. Que No Decir

No diria:

MapStruct valida reglas de negocio.

MapStruct sabe buscar relaciones en la base de datos.

`ignore = true` hace que JPA llene el campo.

El mapper puede usar repositories sin problema.

Da igual devolver entities en controllers.

Para update siempre creo una entity nueva.

