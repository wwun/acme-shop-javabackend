# MapStruct Enterprise

Este documento resume como usaria MapStruct en un proyecto enterprise/banking.

MapStruct no es arquitectura de dominio.

Es una herramienta de mapping.

La arquitectura correcta viene de separar:

```text
API contract
application/domain rules
persistence model
event contracts
```

MapStruct ayuda en las conversiones entre esas capas, pero no debe reemplazar las reglas del sistema.

## 1. Decision Principal

Usaria MapStruct cuando:

- hay DTOs y entities con campos repetidos
- hay muchos mappings repetitivos
- se quiere compile-time safety
- se quiere evitar reflection runtime
- se quiere reducir boilerplate controladamente

No lo usaria para:

- reglas de negocio
- validaciones de dominio
- ownership/security
- generar ids/fechas
- consultar repositorios
- publicar eventos
- resolver workflows

Frase senior:

```text
MapStruct is for deterministic object mapping, not for business decisions.
```

## 2. Por Que MapStruct En Enterprise

Alternativas:

- mapping manual
- ModelMapper
- BeanUtils
- MapStruct

Para sistemas grandes, MapStruct suele ser mejor que reflection-based mappers porque:

- falla antes, durante compilacion
- genera codigo Java legible
- es rapido en runtime
- no depende de reflection magica
- hace los mappings mas explicitos

Mapping manual tambien es valido en partes criticas.

No todo tiene que usar MapStruct.

## 3. Regla De Capas

En banking separaria modelos asi:

```text
Request DTO       -> lo que llega por HTTP
Command           -> intencion interna
Domain/Entity     -> modelo de negocio/persistencia
Response DTO      -> lo que sale por HTTP
Event DTO         -> contrato publicado a Kafka
Read Model DTO    -> vista optimizada para queries
```

MapStruct puede ayudar en algunas conversiones:

```text
Request DTO -> Command
Entity      -> Response DTO
Entity      -> Event DTO
Entity      -> Read DTO
```

Pero no todas deben ser automaticas.

En procesos financieros, a veces prefiero mapping manual para payloads/eventos criticos si hay reglas delicadas.

## 4. Configuracion Recomendada

Mapper Spring:

```java
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface PaymentMapper {
}
```

`componentModel = "spring"`:

```text
Spring crea el mapper como bean.
```

`unmappedTargetPolicy = ReportingPolicy.ERROR`:

```text
si agregas un campo nuevo y no lo mapeas, falla compilacion.
```

Esto puede ser demasiado estricto para todos los mappers, pero es muy bueno en mappers criticos.

Alternativa intermedia:

```text
ERROR en payment/ledger/security/event contracts
WARN o default en DTOs simples
```

## 5. `ignore = true` En Produccion

`ignore = true` no es magia.

Significa:

```text
no generes asignacion para este campo
```

En banking lo usaria para campos que nunca deben venir del cliente:

- `id`
- `customerId` si viene del token/contexto
- `status`
- `createdAt`
- `approvedAt`
- `riskScore`
- `fee`
- `exchangeRate`
- `idempotencyKey`
- `ledgerTransactionId`

Ejemplo:

```java
@Mapping(target = "id", ignore = true)
@Mapping(target = "customerId", ignore = true)
@Mapping(target = "status", ignore = true)
@Mapping(target = "createdAt", ignore = true)
Payment toEntity(CreatePaymentRequest request);
```

Luego el service/handler completa:

```java
payment.setCustomerId(SecurityUtils.getCurrentUserId());
payment.setStatus(PaymentStatus.PENDING);
payment.setCreatedAt(clock.instant());
payment.setIdempotencyKey(idempotencyKey);
```

## 6. Create Vs Update

Create:

```text
crear entity nueva desde request/command
```

Update:

```text
cargar entity existente y modificar campos permitidos
```

Para update enterprise:

```java
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
void updatePayment(@MappingTarget Payment payment, UpdatePaymentRequest request);
```

Importante:

- no crear entity nueva para update si ya existe una fila
- no pisar campos controlados por el sistema
- no permitir cambiar campos inmutables

Ejemplos de campos inmutables:

- payment id
- customer id
- original amount si ya fue autorizado
- idempotency key
- createdAt

## 7. Relaciones

MapStruct no debe resolver relaciones desde DB.

Incorrecto:

```java
class PaymentMapper {
    AccountRepository accountRepository;
}
```

Correcto:

```text
service/handler valida y carga Account
mapper transforma datos simples
service asigna relacion
```

Ejemplo:

```java
Account account = accountRepository.findById(command.sourceAccountId())
    .orElseThrow(...);

Payment payment = mapper.toEntity(command);
payment.setSourceAccount(account);
```

## 8. Eventos Y Contratos Publicos

En sistemas con Kafka, no conviene publicar entities JPA.

Se publican eventos estables:

```java
public record PaymentCreatedEvent(
    UUID paymentId,
    UUID customerId,
    BigDecimal amount,
    String currency,
    Instant createdAt
) {}
```

MapStruct puede convertir:

```text
Payment entity -> PaymentCreatedEvent
```

Pero cuidado:

- no filtrar datos sensibles accidentalmente
- no publicar campos internos
- versionar eventos si cambia contrato
- probar mappings de eventos

Para eventos criticos, el mapping manual tambien es aceptable.

## 9. Seguridad Y Datos Sensibles

Un mapper mal hecho puede filtrar informacion.

Ejemplo de campos que normalmente NO deberian salir:

- password hash
- refresh token
- card PAN completo
- CVV
- internal risk flags
- internal audit notes
- provider tokens

Buenas practicas:

- response DTOs explicitos
- no devolver entities
- testear DTOs sensibles
- usar `ignore = true` o DTOs separados
- revisar mappings en PR

En banking, un mapper no es solo comodidad: tambien es frontera de datos.

## 10. Null Handling

En update parcial:

```java
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
```

Sirve para no pisar valores existentes con null.

Pero no siempre es correcto.

Para PUT completo, un null podria ser invalido.

Para PATCH, un null podria significar:

- ignorar campo
- limpiar campo
- error de validacion

Eso debe definirse como contrato de API.

MapStruct solo ejecuta la decision.

## 11. Testing Enterprise

Testear mappers cuando:

- hay `@Mapping` no trivial
- hay campos ignorados criticos
- hay nested objects
- hay `@MappingTarget`
- hay eventos publicados
- hay datos sensibles
- hay transformaciones custom

No siempre hace falta testear mappings triviales.

Ejemplo de test util:

```text
CreatePaymentRequest -> Payment
  id = null
  status = null
  customerId = null
  amount mapped
  currency mapped
```

Luego otro test del service:

```text
PaymentCommandHandler completa customerId, status, createdAt e idempotencyKey.
```

## 12. Observabilidad

Normalmente no se agregan metricas al mapper.

Si un mapping es lento, suele significar que:

- estas haciendo demasiado en el mapper
- hay relaciones enormes
- estas serializando objetos pesados
- estas metiendo logica que no corresponde

Medir mejor en:

- service
- controller
- Feign client
- query handler
- Kafka consumer

## 13. Propuesta Para Tu Sistema Bancario

Usaria MapStruct asi:

```text
account-service
  AccountMapper
  AccountResponseDTO
  AccountCreatedEvent

payment-service
  PaymentMapper
  CreatePaymentRequest -> CreatePaymentCommand si aplica
  Payment -> PaymentResponseDTO
  Payment -> PaymentCreatedEvent

customer-service
  CustomerMapper
  CustomerResponseDTO sin datos sensibles
```

Reglas:

- mappers sin repositorios
- mappers como beans Spring
- `ReportingPolicy.ERROR` en payment/event mappers
- `@MappingTarget` para updates
- entities nunca salen directo por controller
- eventos nunca usan entities directamente

## 14. Checklist Para PR

Antes de aprobar un mapper:

- El mapper tiene `componentModel = "spring"`?
- Hay campos sensibles expuestos?
- Los campos generados estan ignorados?
- Relaciones se resuelven en service?
- Updates usan entity existente?
- Null handling esta definido?
- Hay tests para mappings criticos?
- Eventos tienen contrato estable?

## 15. Respuesta Para Entrevista

Version corta:

> I use MapStruct for compile-time DTO mapping. It avoids reflection and boilerplate, but I keep business rules out of mappers.

Version banking:

> In a banking system, I would use MapStruct to map between request/response DTOs, entities and event payloads, but I would keep sensitive fields, ids, timestamps, ownership and status transitions in the service/domain layer. For critical mappers I would use explicit mappings and fail the build on unmapped fields.

Version senior:

> I do not treat MapStruct as a domain layer. It is a deterministic transformation tool. I use it where it improves clarity, but for critical financial events or sensitive data boundaries, I make mappings explicit, test them, and avoid leaking internal fields.

## 16. Que No Diria

No diria:

MapStruct reemplaza DTO design.

MapStruct valida negocio.

MapStruct puede consultar repositorios para resolver relaciones.

Da igual usar entities como response.

`ignore = true` hace que la DB llene el campo.

Todos los mappings deben ser automaticos.

No hace falta testear mappings de eventos o datos sensibles.

