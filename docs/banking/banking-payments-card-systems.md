# Banking, Payments Y Tarjetas

Este documento resume lo que un backend engineer deberia entender para hablar de sistemas bancarios, pagos y tarjetas con criterio de produccion.

No es una guia legal ni de cumplimiento formal. Es una guia tecnica para arquitectura, entrevistas y diseno del futuro `bank-system`.

## Idea Central

Un sistema bancario no es solo CRUD sobre cuentas.

Un sistema bancario serio mezcla:

- identidad de cliente
- productos financieros
- cuentas
- tarjetas
- autorizaciones
- ledger contable
- riesgos/fraude
- auditoria
- seguridad
- integraciones legacy
- regulacion
- observabilidad

La diferencia con una app normal es que aqui:

```text
el estado financiero debe ser correcto,
auditable,
reconciliable,
seguro,
y resistente a reintentos/fallas parciales.
```

## Comercios Vs Bancos

No todas las empresas manejan tarjetas igual.

### Ecommerce / Comercio

Un comercio como Acme Shop normalmente intenta reducir su exposicion.

Idealmente no guarda:

- numero completo de tarjeta
- CVV/CVC
- track data
- PIN

Guarda:

- `cardToken`
- `last4`
- `brand`
- referencia del customer en proveedor de pagos
- billing address si aplica

Modelo:

```text
frontend / payment provider
  -> tokeniza tarjeta
  -> devuelve token
  -> ecommerce usa token para cobrar
```

### Banco / Issuer / Processor

Un banco o procesador puede manejar datos mas sensibles, pero en zonas fuertemente controladas.

Ejemplos:

- card management system
- core banking
- authorization system
- fraud/AML systems
- ledger/accounting
- customer information file

Estos sistemas tienen controles mas estrictos:

- PCI DSS
- cifrado en reposo y transito
- masking
- tokenizacion
- HSM / key management
- controles de acceso
- auditoria
- segregacion de funciones
- monitoreo
- retencion

Frase de entrevista:

> Un comercio reduce PCI scope usando tokenizacion. Un banco puede manejar datos sensibles, pero dentro de un cardholder data environment controlado, cifrado, auditado y segmentado.

## PCI DSS En Palabras Practicas

PCI DSS significa:

```text
Payment Card Industry Data Security Standard
```

Aplica cuando una organizacion almacena, procesa o transmite datos de tarjetas.

Terminos importantes:

### PAN

`PAN` es el numero principal de la tarjeta.

Ejemplo:

```text
4111111111111111
```

Si almacenas PAN, el alcance PCI aumenta mucho.

### Cardholder Data

Datos del titular/tarjeta.

Incluye principalmente:

- PAN
- cardholder name
- expiration date
- service code

El PAN es el dato central que vuelve el entorno sensible.

### Sensitive Authentication Data

Datos usados para autenticar/autorizacion de la transaccion.

Incluye:

- CVV/CVC/CID
- full track data
- PIN/PIN block

Regla fuerte:

```text
Sensitive Authentication Data no debe almacenarse despues de la autorizacion, incluso si esta cifrada.
```

Esto incluye CVV/CVC.

Frase de entrevista:

> El CVV se puede usar durante la autorizacion, pero no se persiste despues. Ni siquiera cifrado.

## Tokenizacion

Tokenizacion significa reemplazar el dato sensible por un valor sustituto.

Ejemplo:

```text
PAN real: 4111111111111111
Token:    tok_visa_4242_x8as91
```

El sistema de ecommerce trabaja con:

```json
{
  "cardToken": "tok_visa_4242_x8as91",
  "cardLast4": "1111",
  "cardBrand": "VISA"
}
```

No trabaja con:

```json
{
  "cardNumber": "4111111111111111",
  "cvv": "123"
}
```

Beneficios:

- reduce el dano si hay brecha
- reduce alcance PCI del comercio
- evita PAN/CVV en logs, backups, traces y dumps
- centraliza proteccion en un vault/procesador

## Masking

Masking es mostrar solo parte del dato.

Ejemplo:

```text
**** **** **** 1111
```

Masking no es lo mismo que tokenizacion.

- tokenizacion reemplaza el dato para uso tecnico
- masking oculta el dato para visualizacion

Una pantalla de soporte podria mostrar:

```text
VISA ending in 1111
```

pero no el PAN completo.

## Encriptacion Vs Tokenizacion

Encriptacion:

```text
PAN -> ciphertext
```

El dato original puede recuperarse si tienes la llave.

Tokenizacion:

```text
PAN -> token
```

El token no deberia revelar el PAN. La relacion vive en un vault/token service.

En sistemas bancarios reales se pueden usar ambos:

- tokenizacion para reducir exposicion en aplicaciones
- cifrado fuerte donde el PAN debe persistirse
- HSM/KMS para llaves

## HSM Y Key Management

HSM significa:

```text
Hardware Security Module
```

Es hardware/servicio especializado para proteger llaves criptograficas y operaciones sensibles.

No tienes que implementarlo en Acme, pero debes saber por que existe.

Usos:

- proteger llaves maestras
- operaciones criptograficas sensibles
- PIN block
- datos de tarjeta
- firmas

Respuesta de entrevista:

> No pondria llaves maestras en properties ni en variables simples. En produccion usaria KMS/HSM o secret manager, con rotacion, control de acceso y auditoria.

## Payment Lifecycle

Un pago no es solo `success` o `fail`.

Estados comunes:

```text
PENDING
PROCESSING
AUTHORIZED
CAPTURED
DECLINED
FAILED
REVERSED
REFUNDED
```

### Authorization

El comercio pregunta:

```text
Puedo cobrar este monto?
```

El banco/procesador responde:

```text
AUTHORIZED
DECLINED
```

Authorization puede crear una retencion/hold.

### Capture

El comercio confirma:

```text
Ahora captura/cobra realmente.
```

En ecommerce simple, authorization y capture pueden ocurrir juntos.

En otros modelos, pueden separarse.

### Reversal

Si algo falla despues de autorizar, se puede revertir la autorizacion.

### Refund

Devolver dinero despues de una captura.

No es lo mismo que reversal.

Frase de entrevista:

> Modelaria pagos como una maquina de estados. Evitaria booleanos tipo `paid=true` porque no capturan autorizaciones, reversiones, fallas parciales ni refunds.

## Idempotencia En Pagos

Idempotencia significa que repetir la misma operacion no produce efectos duplicados.

En pagos es critica.

Problema:

```text
Usuario toca Pay.
El backend cobra.
La red falla antes de responder.
Usuario toca Pay otra vez.
```

Sin idempotencia puedes cobrar dos veces.

Solucion:

- `Idempotency-Key` en el request
- unique constraint por usuario/orden/key
- payment state machine
- almacenar resultado de la primera operacion
- devolver el mismo resultado si llega el mismo request

Ejemplo:

```http
POST /api/payments
Idempotency-Key: 11111111-2222-3333-4444-555555555555
```

Tabla:

```sql
unique(user_id, order_id, idempotency_key)
```

Logica:

```text
si existe payment con esa key:
  devolver resultado existente
si no existe:
  crear payment y procesar
```

## Ledger

Un sistema bancario no deberia actualizar dinero como:

```java
balance = balance - amount;
```

sin dejar rastro.

Debe registrar movimientos.

Un ledger es un registro contable de debitos y creditos.

Ejemplo:

```text
Account A debit  100
Account B credit 100
```

Principios:

- append-only cuando sea posible
- auditable
- reconciliable
- no borrar movimientos financieros
- reversar con nuevos movimientos, no editar historia

Modelo simplificado:

```text
ledger_entry
  id
  transaction_id
  account_id
  direction: DEBIT/CREDIT
  amount
  currency
  created_at
```

Un pago autorizado puede generar:

```text
hold / authorization
```

Una captura puede generar:

```text
ledger movement real
```

## Reconciliacion

Reconciliacion significa comparar registros entre sistemas para asegurar que coinciden.

Ejemplos:

- payment-service dice `CAPTURED`
- bank-system dice `AUTHORIZED`
- ledger dice que no hay movimiento

Eso es inconsistencia.

En bancos existen procesos batch o jobs de reconciliacion:

```text
comparar transacciones internas
comparar contra archivos externos
marcar discrepancias
generar reportes
corregir con ajustes controlados
```

Para tu proyecto, una version simple podria ser:

```text
GET /api/reconciliation/payments?date=...
```

que compare:

- payments
- bank authorizations
- ledger entries

## Arquitectura Recomendada Para Tu Bank System

Version inicial pero seria:

```text
api-gateway
auth-service
customer-service
account-service
card-service
payment-service
ledger-service
notification-service opcional
fraud-service opcional
```

### `customer-service`

Responsable de:

- cliente
- datos personales
- KYC basico simulado
- relacion cliente-productos

### `account-service`

Responsable de:

- cuentas
- estado de cuenta
- balance disponible
- balance contable

### `card-service`

Responsable de:

- tarjetas
- tokenizacion simulada
- last4
- brand
- estado de tarjeta
- limite si es credito

No guardar CVV persistido.

### `payment-service`

Responsable de:

- recibir solicitud de pago
- idempotencia
- llamar card/account/bank logic
- crear autorizacion
- publicar eventos

### `ledger-service`

Responsable de:

- movimientos contables
- debitos/creditos
- reversals
- auditoria financiera

## Flujo Acme Shop -> Bank System

Acme no debe conocer tarjeta real.

Request desde Acme:

```json
{
  "paymentId": "uuid",
  "merchantId": "acme-shop",
  "orderId": "uuid",
  "userId": "uuid",
  "amount": 699.99,
  "currency": "CAD",
  "cardToken": "tok_visa_4242",
  "idempotencyKey": "uuid"
}
```

Bank system:

```text
1. valida idempotencyKey
2. valida merchant
3. resuelve cardToken
4. revisa estado de tarjeta
5. revisa fondos/limite
6. crea authorization
7. registra hold o ledger entry segun diseno
8. responde
```

Respuesta autorizada:

```json
{
  "bankTransactionId": "uuid",
  "status": "AUTHORIZED",
  "authorizationCode": "AUTH123456",
  "message": "Payment authorized"
}
```

Respuesta rechazada:

```json
{
  "bankTransactionId": "uuid",
  "status": "DECLINED",
  "declineCode": "INSUFFICIENT_FUNDS",
  "message": "Insufficient funds"
}
```

## Seguridad De APIs Bancarias

Controles esperados:

- OAuth2/OIDC
- Resource Server en microservicios
- scopes/roles
- mTLS para service-to-service en entornos mas fuertes
- secrets fuera del repo
- audit logs
- rate limiting
- input validation
- no datos sensibles en logs
- correlation id

Ejemplo de scopes:

```text
payments:write
payments:read
accounts:read
cards:tokenize
ledger:write
```

## Logs Y Datos Sensibles

No loguear:

- PAN
- CVV
- PIN
- full card token si se considera sensible
- authorization secrets
- access tokens completos

Loguear:

- `paymentId`
- `orderId`
- `bankTransactionId`
- `merchantId`
- status
- error code
- correlation id

Ejemplo:

```text
paymentId=... orderId=... status=AUTHORIZED correlationId=...
```

No:

```text
cardNumber=4111111111111111 cvv=123
```

## Legacy En Bancos

Bancos suelen tener:

- mainframe
- DB2
- AS/400
- SOAP
- batch
- archivos
- colas antiguas
- aplicaciones internas viejas

Una arquitectura real no reemplaza todo de golpe.

Patron comun:

```text
modern API layer
  -> integration service
  -> legacy system
```

O:

```text
microservice
  -> anti-corruption layer
  -> SOAP/mainframe/DB2
```

Frase de entrevista:

> En banco no asumiria greenfield puro. Disenaria una capa de integracion/anti-corruption para aislar legacy y evitar que los modelos antiguos contaminen los dominios nuevos.

## Resiliencia

En pagos no basta con retry agresivo.

Riesgo:

```text
retry mal diseñado = doble cobro
```

Buenas practicas:

- timeout corto
- circuit breaker
- retry solo si la operacion es idempotente
- idempotency key hacia el proveedor/banco
- estado `PROCESSING` para incertidumbre
- reconciliacion posterior

Caso dificil:

```text
payment-service llama al banco
banco autoriza
respuesta se pierde por timeout
```

No puedes simplemente asumir `FAILED`.

Opciones:

- consultar estado por `paymentId`
- dejar `PROCESSING`
- reconciliar
- usar idempotency key para reintentar seguro

## Eventos

Eventos utiles:

- `PaymentRequested`
- `PaymentProcessingStarted`
- `PaymentAuthorized`
- `PaymentDeclined`
- `PaymentCaptured`
- `PaymentFailed`
- `PaymentReversed`
- `PaymentRefunded`

Cada evento debe tener:

```json
{
  "eventId": "uuid",
  "eventType": "PaymentAuthorized",
  "eventVersion": 1,
  "occurredAt": "timestamp",
  "aggregateId": "paymentId",
  "correlationId": "uuid",
  "payload": {}
}
```

Usar outbox para publicar eventos desde payment/ledger.

## Preguntas De Entrevista

### Guardarias Numero De Tarjeta En Tu Payment Service?

Respuesta:

> No en el ecommerce/payment orchestration. Reduciria PCI scope usando tokenizacion. Guardaria `cardToken`, `last4` y `brand`. El CVV nunca lo almacenaria despues de autorizacion. Si soy un banco/card processor y hay una razon de negocio para manejar PAN, lo haria dentro de un entorno PCI controlado con cifrado, HSM/KMS, masking, auditoria y acceso minimo.

### Como Evitas Cobrar Dos Veces?

Respuesta:

> Idempotency key, unique constraints, estado transaccional de payment, y si llamo a un banco/proveedor externo, envio tambien una referencia/idempotency key externa. Si hay timeout, no asumo automaticamente failed; puedo dejar processing y reconciliar o consultar estado.

### Como Modelas Dinero?

Respuesta:

> No actualizaria balances sin historial. Usaria ledger entries de debito/credito, idealmente append-only, con reversals para corregir. El balance se deriva o se actualiza bajo controles estrictos y reconciliacion.

### Que Haces Si El Banco Responde Lento?

Respuesta:

> Timeout, circuit breaker, bulkhead si aplica, estado `PROCESSING`, retry solo idempotente, y reconciliacion. No haria retries ciegos en operaciones que pueden cobrar.

### Como Modernizas Un Sistema Bancario Legacy?

Respuesta:

> No reemplazo todo. Creo APIs modernas, anti-corruption layers, integraciones controladas con legacy, eventos para desacoplar, y migro por dominios. Mantengo auditoria, seguridad y reconciliacion.

## Checklist Para Tu Proyecto

Para que el bank-system se vea serio:

- `card-service` con tokenizacion simulada.
- No persistir CVV.
- Mostrar solo `last4`.
- `payment-service` con idempotency key.
- Estados de payment.
- `ledger-service` con debitos/creditos.
- Eventos Kafka con outbox.
- Circuit breaker al llamar bank/card/ledger.
- Auditoria basica.
- Logs sin datos sensibles.
- Correlation id.
- Resource Server/OAuth2.
- Tests de idempotencia.
- Tests de doble click/retry.
- Tests de timeout del banco.

## Fuentes Oficiales Y Referencias

- PCI Security Standards Council FAQ: Sensitive Authentication Data must not be stored after authorization, even if encrypted.
  https://www.pcisecuritystandards.org/faq/articles/Frequently_Asked_Question/For-PCI-DSS-why-is-storage-of-sensitive-authentication-data-SAD-after-authorization-not-permitted-even-when-there-are-no-primary-account-numbers-PANs-in-an-environment/

- PCI Portal: Sensitive data according to PCI DSS.
  https://helpguide.pciportal.info/hc/en-gb/articles/29421365372946-What-is-sensitive-data-according-to-the-PCI-DSS

- PCI Portal: Alternatives such as tokenization to avoid storing actual card data.
  https://helpguide.pciportal.info/hc/en-gb/articles/25543087053202-Can-I-store-cardholder-data-or-payment-card-numbers

- Microsoft Learn: PCI DSS 4.0.1 AKS regulated cluster guidance. Util para entender controles cloud/Kubernetes como secrets, acceso, monitoreo y proteccion de datos.
  https://learn.microsoft.com/en-us/azure/aks/pci-summary

