# API Gateway Enterprise

Este documento es la version recomendada para pensar un sistema bancario nuevo.

No esta centrado en copiar Acme Shop ni en repetir una arquitectura de curso. Esta centrado en como decidir el borde de entrada en un sistema enterprise moderno.

## 1. Decision Principal

La pregunta no es:

```text
Uso Spring Cloud Gateway o no?
```

La pregunta correcta es:

```text
Que componente sera el borde oficial de entrada?
Que responsabilidades viviran en ese borde?
Que responsabilidades se quedan en cada microservicio?
```

En un sistema bancario, el gateway es una pieza critica porque queda entre clientes externos y servicios internos.

## 2. Recomendacion Para Banking

Si el sistema nuevo corre en Kubernetes, yo no empezaria automaticamente con Eureka ni con Spring Cloud Gateway como unica opcion.

Primero definiria la plataforma:

```text
Internet / Mobile / Web
  -> WAF / CDN si aplica
  -> API Gateway administrado o Ingress/Gateway API
  -> Kubernetes Services
  -> microservicios
```

Opciones comunes:

- Kubernetes Ingress Controller
- Kubernetes Gateway API
- Kong
- NGINX
- Traefik
- Apigee
- AWS API Gateway
- Azure API Management
- Spring Cloud Gateway
- service mesh ingress gateway

La opcion depende de:

- cloud provider
- estandar de la empresa
- necesidades de seguridad
- rate limiting
- monetizacion/API management
- soporte de OAuth2/OIDC
- observability
- costo operacional
- experiencia del equipo

## 3. Gateway No Es Solo Routing

En enterprise, el gateway suele cubrir:

- TLS termination
- CORS
- JWT validation / OAuth2 Resource Server
- OIDC integration
- rate limiting
- request size limits
- header normalization
- correlation id
- API versioning
- routing
- canary routing
- traffic splitting
- metrics
- access logs
- WAF integration
- threat protection
- request/response transformation limitada

Pero no deberia cubrir:

- reglas de negocio profundas
- calculo de saldos
- autorizacion fina de dominio
- transacciones de negocio
- workflows internos
- compensaciones

Frase senior:

> El gateway protege y ordena el borde; no reemplaza la logica ni la seguridad fina de los microservicios.

## 4. Kubernetes: Ingress vs Gateway API vs API Gateway

Antes de comparar herramientas, conviene tener esta foto mental:

```text
Usuario / Browser / Mobile
  -> CDN / WAF
  -> API Gateway / Ingress / Gateway API
  -> Kubernetes Service
  -> Pods del microservicio
```

No todas las empresas usan todas las capas, pero separarlas evita confundir responsabilidades.

### CDN

CDN significa Content Delivery Network.

Sirve para acercar contenido cacheable al usuario:

- imagenes
- CSS
- JavaScript
- archivos estaticos
- contenido publico cacheable

Ejemplos:

- Cloudflare
- AWS CloudFront
- Azure CDN
- Fastly

No necesariamente tienes que usar AWS. Por ejemplo, puedes usar Cloudflare delante de un dominio que apunta a un servidor en OVH, Hetzner o un VPS.

### WAF

WAF significa Web Application Firewall.

Sirve para bloquear trafico sospechoso antes de que llegue a tu aplicacion:

- patrones de SQL injection
- bots
- IPs maliciosas
- requests demasiado grandes
- reglas OWASP
- abuso basico

Ejemplos:

- Cloudflare WAF
- AWS WAF
- Azure WAF
- WAF incluido en una plataforma corporativa de API Gateway

Un WAF no reemplaza validaciones ni seguridad en backend. Es una defensa adicional en el borde.

### Kubernetes Service y Pods

Dentro de Kubernetes, un `Service` da un nombre estable para llegar a pods.

Ejemplo:

```text
payment-service
account-service
customer-service
```

Los pods son donde corre realmente la aplicacion:

```text
payment-service pod 1
payment-service pod 2
payment-service pod 3
```

Aunque los pods cambien, el nombre del Service se mantiene. Por eso Kubernetes Services + DNS reemplazan naturalmente a Eureka en muchos sistemas modernos.

### Ingress

Ingress es el recurso clasico de Kubernetes para exponer HTTP/HTTPS.

Sirve para:

- routing por host
- routing por path
- TLS
- reglas basicas de entrada

Ejemplo conceptual:

```text
bank.example.com/accounts -> account-service
bank.example.com/payments -> payment-service
```

Importante:

Ingress por si solo es una definicion. Necesitas un Ingress Controller que implemente esas reglas.

Ejemplos:

- NGINX Ingress Controller
- Traefik
- HAProxy Ingress

Si ya usaste NGINX como reverse proxy, la idea aqui es parecida:

```text
NGINX recibe trafico HTTP
lee reglas de Kubernetes Ingress
redirige al Service correcto
```

Problema:

Muchas funciones avanzadas terminaron dependiendo de anotaciones especificas del controller.

### Gateway API

Gateway API es la evolucion mas moderna del networking de Kubernetes.

Separa responsabilidades:

- plataforma/infra define `GatewayClass` y `Gateway`
- equipos de aplicaciones definen `HTTPRoute`

Es mas expresivo que Ingress para casos modernos:

- traffic splitting
- header matching
- ownership mas claro
- multi-team
- extensibilidad

Para un sistema bancario nuevo en Kubernetes, Gateway API es una buena tecnologia a estudiar.

### API Gateway dedicado

Herramientas como Kong, Apigee, AWS API Gateway o Azure API Management agregan capacidades de API management:

- API keys
- OAuth2/OIDC policies
- rate limits
- quotas por cliente
- analytics de consumo
- developer portal
- versionado de APIs
- suscripciones por aplicacion/partner
- transformaciones controladas
- monetizacion
- auditoria
- governance

En bancos, no es raro que ya exista una plataforma corporativa de API Gateway.

API Management es mas que "enrutar requests". Es tratar las APIs como una plataforma gobernada.

Ejemplo bancario:

```text
Un partner externo consume /api/payments
```

Con API Management puedes controlar:

```text
partner A tiene 1000 requests/min
partner B tiene 100 requests/min
partner C solo puede usar /api/accounts/read
todos deben enviar client_id
todos deben usar scope payments.write para crear pagos
```

Spring Cloud Gateway puede hacer routing, filtros y seguridad, pero plataformas como Apigee, Kong o Azure API Management suelen traer gobierno de APIs mas completo.

Frase senior:

> API Gateway routes and protects traffic at the edge. API Management adds governance: quotas, API keys, developer portal, analytics, policies and lifecycle management.

### Comparacion De Herramientas

| Herramienta | Que es | Donde encaja | Ventaja | Cuidado |
| --- | --- | --- | --- | --- |
| NGINX Ingress | Ingress Controller / reverse proxy en Kubernetes | Entrada HTTP al cluster | Muy comun, estable, barato | Muchas features avanzadas van por annotations |
| Traefik | Ingress Controller / proxy dinamico | Entrada HTTP al cluster | Simple, buena experiencia dev, automatic discovery | Menos corporativo en algunos bancos |
| Kubernetes Gateway API | API moderna de networking Kubernetes | Reemplazo/evolucion de Ingress | Mejor modelo para equipos/plataformas | Aun depende del controller elegido |
| Spring Cloud Gateway | Gateway construido en Spring/WebFlux | Edge o gateway interno Spring | Control en Java, filtros custom, buen match con Spring | No siempre es el estandar corporativo |
| Kong | API Gateway/API Management | Edge enterprise o platform gateway | Plugins, rate limit, auth, analytics | Operacion adicional |
| Apigee | API Management enterprise | APIs corporativas/partners | Gobierno fuerte, portal, monetizacion, analytics | Costo y dependencia de plataforma |
| AWS API Gateway | Gateway cloud administrado | AWS/serverless/cloud APIs | Managed, integra con AWS | Costo por request/features |
| Azure API Management | API Management administrado | Empresas en Azure | Muy usado en entornos Microsoft/enterprise | Costo y configuracion |
| Service mesh ingress | Entrada usando mesh | Plataformas con Istio/Linkerd/Consul | mTLS/policies/traffic avanzado | Complejidad alta |

## 5. Donde Encaja Spring Cloud Gateway

Spring Cloud Gateway sigue siendo valido.

Tiene sentido si:

- el equipo es fuerte en Spring
- quieres control en Java
- necesitas filtros custom controlados por la app
- estas en Spring Cloud tradicional
- tienes Eureka/Config Server
- quieres un gateway por dominio o BFF interno

Pero en una empresa grande, puede no ser la primera opcion si ya existe:

- API Gateway corporativo
- Kubernetes Gateway API
- service mesh
- cloud gateway administrado

Spring Cloud Gateway no es malo. Es una implementacion posible.

La decision senior es no asumir que siempre es el default.

## 6. Patron Recomendado

Para un sistema bancario nuevo:

```text
Cliente Web / Mobile
  -> CDN/WAF
  -> Enterprise API Gateway / Kubernetes Gateway API
  -> services internos via Kubernetes Service DNS
  -> microservicios
```

Dentro:

```text
account-service
payment-service
customer-service
ledger-service
notification-service
```

Para llamadas internas:

- RestClient / Feign / WebClient para HTTP sincronico
- Kafka para eventos asincronos
- outbox pattern para publicar eventos confiablemente
- circuit breaker/timeouts para dependencias remotas
- observability end-to-end

## 7. Seguridad En Gateway Enterprise

En banking, el gateway suele hacer seguridad gruesa:

- validar token
- validar issuer/audience
- exigir TLS
- aplicar rate limits
- bloquear requests malformadas
- controlar CORS
- integrar WAF

Los microservicios hacen seguridad fina:

- roles
- scopes
- ownership
- permisos de negocio
- limites por cliente/cuenta
- reglas regulatorias

Ejemplo:

```text
Gateway:
  token valido?
  cliente autenticado?
  request permitido por rate limit?

payment-service:
  este usuario puede pagar desde esta cuenta?
  la cuenta tiene fondos?
  el pago supera limites?
  se requiere MFA?
```

### Seguridad Gruesa Como En Acme

Si quieres usar Acme como ejemplo, si aplica.

En Acme Shop:

```text
api-gateway-server valida JWT como OAuth2 Resource Server
```

Eso es seguridad gruesa:

```text
el request tiene token valido?
puede pasar al sistema?
```

Pero no reemplaza seguridad fina:

```text
este usuario puede modificar esta orden?
este admin puede modificar inventario?
este cliente puede ver esta cuenta?
```

En entrevista:

> In Acme, the gateway performs coarse-grained security by validating JWTs at the edge. Services still need fine-grained authorization for roles, ownership and business rules.

## 8. Rate Limiting

Rate limiting en gateway es muy comun.

Casos:

- proteger login
- proteger password reset
- proteger endpoints caros
- evitar abuso de APIs publicas
- limitar por usuario, IP, tenant, client_id o API key

Ejemplo conceptual:

```text
POST /api/auth/login
  max 5 intentos por minuto por usuario/IP

POST /api/payments
  max N requests por minuto por customer/client_id
```

En produccion se suele usar Redis, gateway administrado o politicas del API Gateway.

## 9. Circuit Breaker, Retry y Timeout

El gateway puede aplicar resiliencia, pero con cuidado.

Timeout:

Siempre importante. Un gateway sin timeout puede acumular requests colgadas.

Retry:

Solo para operaciones seguras/idempotentes.

No haria retry automatico de:

```text
POST /payments
```

sin idempotency key y semantica clara.

Circuit breaker:

Sirve para cortar rapido cuando un downstream esta fallando y evitar cascadas.

Respuesta senior:

> I configure timeouts everywhere, retries only when safe, and circuit breakers for unstable dependencies. For payment operations, retries must be paired with idempotency.

## 10. BFF vs API Gateway

API Gateway general:

```text
expone APIs
enruta
aplica politicas transversales
```

BFF:

```text
compone datos para una experiencia especifica
web/mobile/admin
```

Ejemplo:

```text
mobile-bff
  -> account-service
  -> customer-service
  -> card-service
```

No mezclaria demasiada composicion de pantalla en el gateway principal.

Para eso usaria:

- BFF
- query service
- read model
- GraphQL gateway si aplica

## 11. API Gateway vs Service Mesh

API Gateway:

```text
north-south traffic
cliente externo hacia cluster
```

Service Mesh:

```text
east-west traffic
servicio a servicio dentro del cluster
```

El mesh puede manejar:

- mTLS interno
- retries/timeouts
- traffic splitting
- telemetry
- policies internas

Pero agrega complejidad operacional.

No lo pondria solo para verse "enterprise".

Lo propondria si hay una necesidad real:

- muchos servicios
- compliance fuerte
- mTLS obligatorio
- observability avanzada
- traffic control sofisticado

## 12. Recomendacion Practica Sin Cloud Caro

Para aprender y defender arquitectura enterprise sin pagar servicios cloud caros:

```text
Kubernetes local con kind/k3d/minikube
  -> NGINX Ingress o Traefik
  -> Kubernetes Services
  -> microservicios Spring Boot
```

Stack recomendado:

- `kind` o `k3d` para Kubernetes local
- NGINX Ingress Controller al inicio
- Kubernetes Services para discovery
- Keycloak local para OAuth2/OIDC
- Spring OAuth2 Resource Server en microservicios
- Redis para rate limiting/cache/idempotency si aplica
- Kafka para eventos
- Prometheus/Grafana para metricas
- OpenTelemetry + Jaeger/Tempo si quieres tracing

Primera decision concreta para tu sistema bancario:

```text
Usar NGINX Ingress + Kubernetes Services.
```

Luego, cuando eso este claro:

```text
evaluar Gateway API
evaluar Kong local si quieres practicar API Management
evaluar service mesh solo si hay necesidad real
```

Frase defendible:

> Para evitar costos innecesarios, implemente la arquitectura localmente con Kubernetes y herramientas open source. Use primitives estandar como Deployments, Services, ConfigMaps, Secrets, probes e Ingress, de modo que el diseno sea portable a EKS, AKS o GKE.

## 13. Paso a Paso Enterprise Para Banking

### Paso 1. Definir plataforma

Preguntas:

```text
Corre en Kubernetes?
Cloud provider?
Existe API Gateway corporativo?
Existe service mesh?
Existe IdP corporativo?
```

Si hay plataforma corporativa, seguirla.

### Paso 2. Definir borde externo

Opciones:

```text
API Gateway administrado
Kubernetes Gateway API
Ingress Controller
Kong/Apigee/NGINX/Traefik
Spring Cloud Gateway
```

Decision recomendada si estas creando portfolio banking en Kubernetes:

```text
NGINX Ingress Controller para primera version
Kubernetes Services para discovery interno
Spring services detras con OAuth2 Resource Server
```

Luego puedes evolucionar a:

```text
Kubernetes Gateway API
Kong local
API Gateway corporativo/cloud en una version real de empresa
```

Si quieres practicar Spring Cloud Gateway tambien:

```text
Ingress -> Spring Cloud Gateway -> microservicios
```

Pero debes poder justificar por que agregas esa capa.

### Paso 3. Definir rutas externas

Ejemplo:

```text
/api/customers/**
/api/accounts/**
/api/cards/**
/api/payments/**
/api/transfers/**
/api/auth/**
```

No exponer rutas internas innecesarias.

### Paso 4. Definir seguridad de borde

Minimo:

- OAuth2/OIDC
- issuer validation
- audience validation
- scopes
- TLS
- CORS controlado
- rate limiting
- logs sin datos sensibles

### Paso 5. Definir seguridad interna

Cada microservicio tambien debe validar token o identidad de servicio.

Opciones:

- OAuth2 Resource Server en cada servicio
- mTLS con service mesh
- token exchange para service-to-service
- service accounts para llamadas backend

### Paso 6. Definir resiliencia

Por ruta:

- timeout
- retry si es seguro
- circuit breaker
- fallback si aplica
- response mapping

Para pagos:

- idempotency key obligatoria
- no duplicar cargos
- audit trail
- outbox/eventos

### Paso 7. Definir observability

El gateway debe emitir:

- request count
- latency por ruta
- status codes
- 4xx/5xx
- rate limit hits
- circuit breaker state
- correlation id
- trace id

### Paso 8. Probar errores

Pruebas:

- token ausente -> 401
- token valido sin permisos -> 403
- ruta inexistente -> 404
- downstream caido -> 503/502 controlado
- CORS preflight -> 200/204
- rate limit -> 429
- timeout -> error controlado
- retry en GET idempotente
- no retry inseguro en payment POST sin idempotency

## 14. Que Implementaria En Tu Banking Portfolio

Primera version fuerte:

- Kubernetes manifests
- Ingress o Gateway API
- OAuth2 Resource Server en servicios
- API Gateway/BFF solo si justifica composicion o politicas extra
- Redis rate limiting para endpoints sensibles
- Resilience4j en llamadas internas
- correlation id
- OpenTelemetry
- tests de seguridad y rutas

Si quieres mostrar Spring Cloud Gateway:

- usarlo como edge gateway de la app
- documentar que en empresa podria reemplazarse por gateway corporativo
- no usar Eureka si vas por Kubernetes
- usar Kubernetes Service DNS para discovery

## 15. Comparacion Rapida

| Opcion | Uso tipico | Comentario |
| --- | --- | --- |
| Spring Cloud Gateway | Spring Cloud, control en Java | Bueno, pero no siempre default enterprise |
| Ingress Controller | Entrada HTTP clasica en Kubernetes | Muy comun |
| Kubernetes Gateway API | Evolucion moderna de Ingress | Buena apuesta para proyectos nuevos |
| Kong / Apigee | API management enterprise | Comun en empresas grandes |
| AWS API Gateway / Azure APIM | Cloud managed API gateway | Reduce operacion propia |
| Service Mesh Ingress | Organizaciones con mesh | Poderoso, mas complejo |

## 16. Que Diria En Entrevista

Version corta:

> An API Gateway is the external entry point. It centralizes routing and edge concerns such as authentication, CORS, rate limiting, observability and traffic policies. I keep business rules inside microservices.

Version senior:

> For a banking platform, I would not blindly choose Spring Cloud Gateway. I would first check the runtime platform and enterprise standards. In Kubernetes, I would likely use Ingress or Gateway API, or a corporate API gateway such as Kong, Apigee or a cloud-managed gateway. Services would still validate tokens and enforce domain authorization. The gateway would handle edge security, CORS, rate limiting, observability and controlled routing.

Version sin depender de cloud caro:

> I designed the system using Kubernetes-native primitives to avoid coupling the architecture to a specific cloud provider. The edge can start with NGINX Ingress and later evolve to Gateway API, Kong, Apigee or a managed API gateway depending on enterprise standards. Services remain behind stable Kubernetes Services, validate tokens as Resource Servers and expose clear operational signals.

## 17. Que No Diria

No diria:

Gateway reemplaza toda la seguridad.

Gateway reemplaza Kubernetes.

Gateway reemplaza service discovery.

Gateway debe tener logica de negocio.

Gateway debe hacer retry de todos los POST.

Spring Cloud Gateway es siempre la opcion mas enterprise.

Ingress y API Gateway son exactamente lo mismo.

NGINX Ingress reemplaza toda la seguridad.

API Management es solo routing.

Service mesh es obligatorio para ser enterprise.

## 18. Fuentes Utiles

- Spring Cloud Gateway official docs: https://docs.spring.io/spring-cloud-gateway/reference/index.html
- Kubernetes Gateway API: https://gateway-api.sigs.k8s.io/docs/introduction/
- Kubernetes Services: https://kubernetes.io/docs/concepts/services-networking/service/
