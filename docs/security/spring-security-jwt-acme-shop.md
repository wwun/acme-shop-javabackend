# Spring Security + JWT en Acme Shop

Este documento consolida las notas originales de Spring Security/JWT y las alinea con la implementacion actual de Acme Shop.

La idea importante: las notas antiguas explicaban varias formas de implementar JWT. El proyecto actual ya evoluciono. Hoy Acme Shop usa:

- `auth-service` para autenticar y emitir JWT.
- `user-service` para usuarios, roles, password hashing y validacion interna de credenciales.
- `acme-commons` para codigo compartido de JWT.
- `api-gateway-server` como borde de entrada y Resource Server.
- Microservicios de negocio con `JwtAuthFilter` propio y `@PreAuthorize`.
- Feign con propagacion del token cuando un servicio llama a otro.

## 1. Conceptos Base

**Autenticacion** responde: quien eres.

Ejemplo: enviar username/password a `auth-service` y recibir un JWT.

**Autorizacion** responde: que puedes hacer.

Ejemplo: un usuario con `ROLE_USER` puede consultar productos, pero solo `ROLE_ADMIN` puede crear categorias o modificar inventario.

**JWT** es un token firmado que transporta claims. En este proyecto el JWT contiene:

- `sub`: username.
- `userId`: id del usuario.
- `email`: email del usuario.
- `roles`: lista de roles, por ejemplo `ROLE_ADMIN` o `ROLE_USER`.
- `iat`: fecha de emision.
- `exp`: fecha de expiracion.

El token no se "desencripta". Se parsea y se valida su firma con la misma secret key. Si alguien modifica el payload, la firma deja de coincidir.

## 2. Flujo Actual de Login

Flujo real del proyecto:

```text
Cliente
  -> POST /api/auth/login
  -> API Gateway
  -> auth-service
  -> user-service /internal/... para verificar credenciales
  -> auth-service genera JWT con JwtService
  -> Cliente recibe token
```

En [AuthServiceImpl](/home/william/Documents/portfolio/acme-shop/auth-service/src/main/java/com/wwun/acme/auth/service/AuthServiceImpl.java), el login no autentica directamente contra la tabla de usuarios. Llama a `UserGatewayService`, recibe un `UserAuthResponseDTO`, toma sus roles y genera el JWT:

```java
UserAuthResponseDTO user = userGatewayService.verify(authRequestDTO);
List<String> roles = user.roles().stream().map(RoleResponseDTO::name).toList();
String token = jwtService.generateToken(user.id(), user.username(), user.email(), roles);
```

Esto separa responsabilidades:

- `auth-service`: orquesta login, OAuth2 y emision de tokens.
- `user-service`: conoce usuarios, passwords y roles.

## 3. Registro y Passwords

El password nunca se guarda plano.

`user-service` define:

```java
@Bean
PasswordEncoder passwordEncoder(){
    return new BCryptPasswordEncoder();
}
```

BCrypt no "desencripta" passwords. Para validar hace:

```text
passwordEncoder.matches(rawPassword, encodedPasswordFromDatabase)
```

BCrypt usa salt y factor de costo. El hash almacenado contiene la informacion necesaria para comparar el password ingresado con el hash guardado.

Explicacion de entrevista:

> Nunca guardaria passwords en texto plano. Uso BCrypt porque incorpora salt y costo configurable. En login, Spring compara el password enviado contra el hash almacenado usando `matches`, no desencripta nada.

## 4. acme-commons

`acme-commons` es una libreria interna, no una app.

Contiene codigo reutilizable:

- [JwtService](/home/william/Documents/portfolio/acme-shop/acme-commons/src/main/java/com/wwun/acme/security/JwtService.java)
- [JwtAuthConverter](/home/william/Documents/portfolio/acme-shop/acme-commons/src/main/java/com/wwun/acme/security/JwtAuthConverter.java)
- [JwtFeignInterceptor](/home/william/Documents/portfolio/acme-shop/acme-commons/src/main/java/com/wwun/acme/security/JwtFeignInterceptor.java)
- [SecurityUtils](/home/william/Documents/portfolio/acme-shop/acme-commons/src/main/java/com/wwun/acme/security/SecurityUtils.java)
- [TokenJwtConfig](/home/william/Documents/portfolio/acme-shop/acme-commons/src/main/java/com/wwun/acme/security/TokenJwtConfig.java)

Esto evita duplicar la logica JWT en cada servicio.

### JwtService

Responsabilidades:

- Generar token.
- Validar firma y expiracion.
- Extraer username.
- Extraer roles.
- Extraer userId.
- Extraer claims genericos.

Metodo clave:

```java
public <T> T extractClaim(String token, Function<Claims, T> resolver){
    final Claims claims = getAllClaims(token);
    return resolver.apply(claims);
}
```

`Function<Claims, T>` permite reutilizar el mismo metodo para extraer distintos claims:

```java
extractClaim(token, Claims::getSubject)
extractClaim(token, Claims::getExpiration)
```

Explicacion corta:

> `extractClaim` recibe una funcion que sabe que dato sacar del payload. Asi evito repetir el parseo del token para cada claim.

### JwtAuthConverter

Convierte un token valido en un `Authentication` de Spring Security.

Hace:

```text
JWT -> username, roles, userId -> AuthUserPrincipal -> UsernamePasswordAuthenticationToken
```

El principal del proyecto no es solo un string. Es `AuthUserPrincipal`, que permite obtener `userId` y `username` desde `SecurityUtils`.

## 5. SecurityConfig en Microservicios Servlet

Servicios como product, order, cart, inventory, user y catalog usan Spring MVC/Servlet.

El patron esperado:

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception{
        return http.authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .csrf(csrf -> csrf.disable())
            .build();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtService jwtService) {
        return new JwtAuthFilter(jwtService);
    }
}
```

Puntos clave:

- `@EnableMethodSecurity(prePostEnabled = true)` habilita `@PreAuthorize`.
- `csrf().disable()` es normal en APIs REST stateless con JWT.
- `addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)` ejecuta tu filtro JWT antes del filtro tradicional de username/password.
- `anyRequest().authenticated()` bloquea todo lo que no fue explicitamente publico.

### Por que `filterChain` recibe `JwtAuthFilter`

Este metodo:

```java
@Bean
SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception{
    return http
        .authorizeHttpRequests(authz -> authz
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .csrf(csrf -> csrf.disable())
        .build();
}
```

recibe `JwtAuthFilter` porque necesita agregarlo a la cadena de filtros de Spring Security:

```java
.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
```

Pero Spring solo puede inyectar `JwtAuthFilter` si ese filtro existe como bean:

```java
@Bean
public JwtAuthFilter jwtAuthFilter(JwtService jwtService) {
    return new JwtAuthFilter(jwtService);
}
```

Flujo mental:

```text
filterChain necesita JwtAuthFilter
JwtAuthFilter debe existir como bean
JwtAuthFilter necesita JwtService
JwtService existe como bean en acme-commons
```

El bean de `JwtService` no esta en cada microservicio. Esta centralizado en `acme-commons`:

```java
@Configuration
public class CommonsConfig {

    @Bean
    public JwtService jwtService(
        @Value("${jwt.secret}") String secretKey,
        @Value("${jwt.expiration}") long jwtExpiration
    ){
        return new JwtService(secretKey, jwtExpiration);
    }
}
```

Entonces Spring puede construir todo:

```text
1. Lee CommonsConfig.
2. Crea JwtService usando jwt.secret y jwt.expiration.
3. Crea JwtAuthFilter usando JwtService.
4. Crea SecurityFilterChain usando JwtAuthFilter.
5. Mete JwtAuthFilter en la cadena de seguridad.
```

### Que es un bean

Un bean es un objeto administrado por el contenedor de Spring.

Eso significa que Spring:

```text
1. Lo crea.
2. Guarda una instancia en su ApplicationContext.
3. Inyecta ese objeto donde alguien lo necesite.
4. Maneja su ciclo de vida.
```

Ejemplo:

```java
@Bean
public JwtAuthFilter jwtAuthFilter(JwtService jwtService) {
    return new JwtAuthFilter(jwtService);
}
```

Aqui no estas llamando manualmente este metodo desde tu codigo. Spring lo llama al arrancar la aplicacion, crea el objeto y lo registra.

Despues, cuando otro bean pide:

```java
SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter)
```

Spring dice:

```text
"Necesitas un JwtAuthFilter. Yo ya tengo uno registrado. Te lo paso."
```

Por eso "se ve" desde otras clases: no porque sea global estatico, sino porque Spring lo tiene registrado en su contenedor.

Formas comunes de crear beans:

```java
@Component
@Service
@Repository
@Controller
@RestController
@Configuration + @Bean
```

Regla practica:

```text
Si una clase necesita ser inyectada por Spring, debe ser un bean.
Si una clase necesita construccion especial o parametros de configuracion, suele declararse con @Bean.
```

## 6. JwtAuthFilter en Microservicios

El filtro actual en los servicios extiende `OncePerRequestFilter`.

Flujo:

```text
1. Leer header Authorization.
2. Verificar que empiece con Bearer.
3. Extraer token.
4. Convertir token a Authentication con JwtAuthConverter.
5. Guardar Authentication en SecurityContextHolder.
6. Continuar la cadena de filtros.
```

Codigo conceptual:

```java
String authHeader = request.getHeader(HEADER_AUTHORIZATION);

if(authHeader != null && authHeader.startsWith(PREFIX_HEADER)){
    String token = authHeader.substring(PREFIX_HEADER.length()).trim();
    Authentication auth = jwtAuthConverter.convert(token);
    SecurityContextHolder.getContext().setAuthentication(auth);
}

filterChain.doFilter(request, response);
```

`SecurityContextHolder` guarda la autenticacion de la request actual. Luego `@PreAuthorize`, controllers y services pueden consultar quien es el usuario.

## 7. API Gateway

El gateway es WebFlux, no Spring MVC. Por eso usa:

- `SecurityWebFilterChain`
- `ServerHttpSecurity`
- `ReactiveJwtDecoder`
- `CorsWebFilter`

En [SecurityConfig del gateway](/home/william/Documents/portfolio/acme-shop/api-gateway-server/src/main/java/com/wwun/acme/api_gateway_server/security/SecurityConfig.java), el gateway usa OAuth2 Resource Server:

```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
```

Y define el decoder:

```java
@Bean
public ReactiveJwtDecoder jwtDecoder(){
    return NimbusReactiveJwtDecoder
        .withSecretKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
        .build();
}
```

Esto significa que el gateway valida Bearer tokens de forma estandar:

- Extrae el token.
- Valida firma.
- Valida expiracion.
- Rechaza requests no autenticadas.

Rutas publicas actuales:

- `/api/auth/**`
- `/actuator/**`
- `/api/health`
- Swagger/OpenAPI
- OAuth2 endpoints

Explicacion de entrevista:

> Uso seguridad gruesa en el gateway para bloquear requests sin token antes de llegar a los microservicios. Aun asi, mantengo seguridad fina en los servicios con `@PreAuthorize`, porque no quiero depender solamente del gateway.

## 8. Gateway Resource Server vs Filtro JWT Custom

Tus notas antiguas tenian `JwtAuthenticationFilter` y `JwtValidationFilter`.

Eso era una implementacion custom clasica:

- `JwtAuthenticationFilter`: login con username/password y generacion del token.
- `JwtValidationFilter`: validacion del token en cada request.

El proyecto actual ya no usa esa forma como camino principal.

Comparacion:

| Enfoque | Que hace | Ventaja | Desventaja |
| --- | --- | --- | --- |
| Filtro JWT custom | Tu lees header, validas firma, parseas claims y creas Authentication | Control total | Mas codigo, mas riesgo de errores |
| OAuth2 Resource Server | Spring valida Bearer token con `oauth2ResourceServer().jwt()` | Mas estandar y seguro | Requiere configurar decoder/claims |

En Acme Shop:

- Gateway: usa Resource Server.
- Microservicios: usan filtro propio simple con `JwtAuthConverter`.

Esto es valido para un proyecto de portfolio, pero en produccion real muchas empresas tienden a estandarizar mas con Resource Server en todos los servicios o con un proveedor como Keycloak/Auth0/Okta.

### Que es OAuth2 Resource Server

OAuth2 no es exactamente lo mismo que Resource Server.

OAuth2 es un framework/protocolo de autorizacion. Dentro de OAuth2 existen varios roles:

```text
Resource Owner:
  El usuario dueno de los datos.

Client:
  La app que pide acceso. Ejemplo: frontend, mobile app, Bruno/Postman.

Authorization Server:
  El componente que autentica y emite tokens.
  Ejemplos reales: Keycloak, Auth0, Okta, Azure AD, Cognito.
  En Acme Shop: auth-service cumple una version propia de este rol porque emite JWT.

Resource Server:
  La API protegida que recibe un access token y valida si puede entregar el recurso.
  Ejemplos en Acme Shop: product-service, order-service, inventory-service, cart-service, catalog-query-service.
```

Entonces, cuando Spring Security dice `oauth2ResourceServer`, significa:

> Esta aplicacion es una API protegida. Va a recibir Bearer tokens y Spring se encargara de validarlos.

Ejemplo:

```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
```

Con eso, Spring Security hace automaticamente gran parte de lo que antes hacia un filtro custom:

```text
1. Lee el header Authorization.
2. Extrae el Bearer token.
3. Valida firma.
4. Valida expiracion.
5. Construye un Authentication.
6. Lo guarda en el SecurityContext.
7. Permite usar @PreAuthorize.
```

### Que cambia si implemento Resource Server en los microservicios

Con el filtro custom actual:

```text
JwtAuthFilter
  -> lee Authorization
  -> extrae token
  -> JwtAuthConverter
  -> JwtService valida firma/claims
  -> crea UsernamePasswordAuthenticationToken
  -> SecurityContextHolder.setAuthentication(...)
```

Con Resource Server:

```text
Spring Security
  -> lee Authorization
  -> extrae token
  -> JwtDecoder valida firma/expiracion
  -> crea Authentication
  -> SecurityContextHolder queda poblado
```

Por eso, si un microservicio usa Resource Server bien configurado, ya no necesita:

```text
JwtAuthFilter
JwtAuthConverter
```

Pero todavia puedes necesitar:

```text
JwtService
```

en `auth-service`, porque alguien debe generar los tokens.

Tambien puedes necesitar una forma de propagar el token con Feign. Si migras a Resource Server, revisa `SecurityUtils.getCurrentToken()`, porque el token puede quedar representado distinto dentro del `Authentication`.

Resumen practico:

```text
Con filtro custom:
  SecurityConfig
  JwtAuthFilter
  JwtAuthConverter
  JwtService para validar
  @PreAuthorize

Con Resource Server:
  SecurityConfig
  JwtDecoder / issuer-uri / jwk-set-uri
  @PreAuthorize
```

Si usas Resource Server bien, en los microservicios ya no necesitas:

```text
JwtAuthFilter
JwtAuthConverter
```

Spring Security hace esa parte.

### Resource Server no reemplaza la propagacion con Feign

`Resource Server` y `FeignSecurityConfig` resuelven problemas distintos:

```text
Resource Server:
  Valida el token que entra al servicio.

FeignSecurityConfig / RequestInterceptor:
  Propaga el token cuando este servicio llama a otro servicio.
```

Ejemplo:

```text
Cliente -> catalog-query-service
  Authorization: Bearer abc

catalog-query-service:
  valida token con Resource Server

catalog-query-service -> product-service
  necesita reenviar Authorization: Bearer abc
```

Para eso todavia necesitas algo como un interceptor de Feign:

```text
FeignSecurityConfig
FeignClientConfig
AuthForwardingConfig
RequestInterceptor
```

El nombre puede cambiar, pero la idea es la misma: antes de enviar una request Feign, agregar el header `Authorization`.

Lo que cambia al migrar a Resource Server es como obtienes el token actual.

Con el flujo custom actual, esto funciona:

```java
SecurityUtils.getCurrentToken()
```

porque `JwtAuthFilter` guarda el token como credentials:

```java
new UsernamePasswordAuthenticationToken(principal, token, authorities)
```

Con Resource Server, Spring crea otro tipo de `Authentication`, normalmente `JwtAuthenticationToken`. Por eso el interceptor tendria que leer el token desde ese objeto, o leer el header original de la request actual.

Frase importante:

> Lo profesional no es "no usar interceptor". Lo profesional es usar Resource Server para validar y un RequestInterceptor/WebClient filter para propagar el contexto o token cuando una llamada downstream representa al usuario.

### Flujo profesional tipico

Para microservicios propios con JWT:

```text
1. auth-service / IdP emite token.
2. gateway valida token como Resource Server.
3. microservicio valida token como Resource Server tambien.
4. @PreAuthorize aplica roles/scopes.
5. Feign/WebClient propaga token a servicios downstream si la llamada representa al usuario.
```

### Opciones reales

#### Opcion A: filtro JWT custom

Esta es la opcion mas academica y sirve muy bien para aprender los internals.

```text
JwtAuthFilter
JwtAuthConverter
JwtService
SecurityContextHolder
```

Ventaja:

```text
Entiendes exactamente como se lee el header, se valida el token y se crea el Authentication.
```

Desventaja:

```text
Mas codigo propio de seguridad, mas posibilidad de errores y menos estandarizacion.
```

Uso recomendado:

```text
Aprendizaje, portfolio, proyectos controlados o casos donde necesitas control muy especifico.
```

#### Opcion B: Resource Server con secret compartida

Esta opcion es mas estandar que el filtro custom y sirve cuando tus JWT estan firmados con una secret compartida, como HS256.

Ejemplo:

```java
@Bean
JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secretKey) {
    return NimbusJwtDecoder
        .withSecretKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
        .build();
}
```

Y en security:

```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
```

Ventaja:

```text
Spring Security valida Bearer token de forma estandar.
```

Desventaja:

```text
Todos los Resource Servers necesitan conocer la misma secret. La rotacion de llaves y el control de secretos se vuelve delicado.
```

Uso recomendado:

```text
Proyecto propio, ambiente interno, MVP serio o transicion hacia un IdP real.
```

#### Opcion C: Resource Server con issuer-uri o jwk-set-uri

Esta es la opcion mas comun en ambientes enterprise/cloud.

El Authorization Server o IdP publica sus llaves publicas:

```text
Keycloak
Okta
Auth0
Azure AD / Entra ID
Cognito
Ping Identity
ForgeRock
```

Configuracion tipica:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://idp.example.com/realms/acme
```

O:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: https://idp.example.com/.well-known/jwks.json
```

Ventaja:

```text
Los microservicios no conocen la secret privada.
Validan con llaves publicas.
Soporta rotacion de llaves.
Se integra mejor con IdPs corporativos.
```

Desventaja:

```text
Requiere un Authorization Server/IdP real y configuracion de issuer, audience, scopes/claims.
```

Uso recomendado:

```text
Produccion real, bancos, empresas grandes, cloud, sistemas con varios equipos.
```

Conclusion:

```text
Opcion A:
  Buena para aprender.

Opcion B:
  Mejor estandarizacion manteniendo tu auth-service propio y secret compartida.

Opcion C:
  Mas profesional/enterprise si usas IdP real con JWKS.
```

### Servicios del proyecto que usan Resource Server hoy

Estado actual:

```text
api-gateway-server:
  Si usa OAuth2 Resource Server.

auth-service:
  No es Resource Server principalmente. Es el servicio que emite tokens y maneja login/OAuth2.

user-service:
  Usa filtro custom JwtAuthFilter.

product-service:
  Usa filtro custom JwtAuthFilter.

order-service:
  Usa filtro custom JwtAuthFilter.

inventory-service:
  Usa filtro custom JwtAuthFilter.

cart-service:
  Usa filtro custom JwtAuthFilter.

catalog-query-service:
  Pendiente de implementar seguridad.
```

Arquitectura actual:

```text
Gateway: Resource Server
Microservicios: filtro custom
```

Arquitectura mas estandarizada:

```text
Gateway: Resource Server
Microservicios: Resource Server
Auth: emisor de tokens o delegado a Keycloak/Auth0/Okta/Azure AD
```

### Que suelen hacer bancos y empresas grandes

En bancos y empresas grandes normalmente se evita tener mucha logica de seguridad hecha a mano en cada microservicio.

Lo comun es usar un proveedor de identidad o authorization server:

```text
Keycloak
Okta
Auth0
Azure AD / Entra ID
Ping Identity
ForgeRock
Cognito
```

Y los microservicios se configuran como Resource Servers:

```text
Request con Bearer token
  -> Resource Server valida token con llaves publicas/JWKS o secret compartida
  -> aplica roles/scopes/claims
  -> @PreAuthorize protege endpoints
```

Eso no significa que nunca exista un filtro custom. Si existe, suele ser para casos especificos:

- correlation id
- tenant id
- headers internos
- auditoria
- validaciones corporativas extra
- mTLS/header validation entre capas internas

Pero para validar JWT, roles y expiracion, lo preferible suele ser usar el mecanismo estandar de Spring Security Resource Server.

Frase de entrevista:

> Para aprender implemente un filtro JWT custom y entiendo el flujo interno: leer header, validar token, crear Authentication y poblar el SecurityContext. Para una version production-level tenderia a estandarizar los microservicios como OAuth2 Resource Servers y delegar la emision de tokens a un IdP como Keycloak, Okta o Azure AD.

## 9. Roles vs Authorities

En Spring Security, roles y authorities terminan siendo `GrantedAuthority`.

Convencion:

- Rol almacenado/token: `ROLE_ADMIN`
- En anotacion: `hasRole('ADMIN')`

Spring agrega automaticamente el prefijo `ROLE_` cuando usas `hasRole`.

Ejemplos:

```java
@PreAuthorize("hasRole('ADMIN')")
```

Valida que exista:

```text
ROLE_ADMIN
```

```java
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
```

Valida que exista `ROLE_ADMIN` o `ROLE_USER`.

Si usas permissions mas finos:

```java
@PreAuthorize("hasAuthority('DELETE_PRODUCT')")
```

Eso valida literalmente `DELETE_PRODUCT`.

Frase de entrevista:

> Uso roles para permisos gruesos como ADMIN o USER. Si el dominio crece, evolucionaria a authorities mas granulares como READ_ORDER, CANCEL_ORDER o APPROVE_PAYMENT.

## 10. Feign y Propagacion del Token

Cuando un microservicio llama a otro con Feign, el servicio destino tambien puede exigir JWT.

Para eso existe [JwtFeignInterceptor](/home/william/Documents/portfolio/acme-shop/acme-commons/src/main/java/com/wwun/acme/security/JwtFeignInterceptor.java).

Flujo:

```text
Request entra con Authorization: Bearer token
JwtAuthFilter valida y guarda el token como credentials
FeignSecurityConfig registra JwtFeignInterceptor
JwtFeignInterceptor toma el token actual desde SecurityUtils
Feign agrega Authorization: Bearer token en la request saliente
```

Codigo clave:

```java
String token = SecurityUtils.getCurrentToken();

if(token == null || token.isBlank()){
    return;
}

template.header(TokenJwtConfig.HEADER_AUTHORIZATION, TokenJwtConfig.PREFIX_HEADER + " " + token);
```

Servicios que ya tienen esta idea implementada:

- `order-service`
- `cart-service`

Nota importante de estado actual:

- `inventory-service` tiene `FeignSecurityConfig`, pero actualmente esta vacio.
- `catalog-query-service` todavia tiene `SecurityConfig` y `JwtAuthFilter` vacios.

Checklist para `catalog-query-service`:

```text
1. Copiar/adaptar SecurityConfig de product-service.
2. Copiar/adaptar JwtAuthFilter de product-service.
3. Crear FeignSecurityConfig con JwtFeignInterceptor.
4. Agregar configuration = FeignSecurityConfig.class a ProductClient e InventoryClient.
5. Probar request por gateway con Bearer token.
```

## 11. Seguridad Gruesa y Seguridad Fina

**Seguridad gruesa**:

- Lugar: Gateway.
- Pregunta: trae token valido?
- Ejemplos: bloquear `/api/**`, permitir `/api/auth/**`, CORS, rate limiting.

**Seguridad fina**:

- Lugar: microservicio.
- Pregunta: este usuario puede ejecutar esta accion concreta?
- Ejemplos: `@PreAuthorize("hasRole('ADMIN')")`, validar que el usuario sea owner de una orden.

Ejemplo en ecommerce:

```text
Gateway:
  "Solo requests autenticadas pueden entrar a /api/orders/**"

order-service:
  "Este usuario solo puede ver sus propias ordenes, a menos que sea ADMIN"
```

Frase senior:

> El gateway reduce trafico no autorizado, pero la decision de negocio vive en el microservicio. No confiaria toda la autorizacion al gateway.

## 12. Web MVC vs WebFlux

La mayoria de servicios del proyecto usan Spring MVC:

- `spring-boot-starter-web`
- Servlet
- Tomcat
- `HttpSecurity`
- `SecurityFilterChain`
- `OncePerRequestFilter`

El gateway usa WebFlux:

- Netty
- `ServerHttpSecurity`
- `SecurityWebFilterChain`
- `ReactiveJwtDecoder`
- `CorsWebFilter`

Regla practica:

```text
Microservicio normal -> Spring MVC
Gateway/reactive edge -> WebFlux
```

## 13. CSRF, CORS y Stateless

**CSRF** suele deshabilitarse en APIs REST con JWT porque no dependes de cookies de sesion del navegador.

**Stateless** significa que el servidor no guarda sesion HTTP. Cada request trae todo lo necesario en el token.

**CORS** debe controlarse principalmente en el gateway, porque es el borde que consume el frontend.

Estado actual:

- Gateway tiene CORS con `Authorization`, `Content-Type` e `Idempotency-Key`.
- Esto fue importante porque pedidos usa `Idempotency-Key` y el navegador/gateway necesita permitir ese header.

## 14. Errores que Tuviste y Como Diagnosticarlos

### 403 intermitente en product-service

Sintoma:

```text
Un request daba 200 y el siguiente 403.
```

Causa que se descubrio:

```text
Redis estaba cacheando objetos con una serializacion que no reconstruia bien tipos/listas.
```

Solucion:

```text
Configurar correctamente Redis CacheManager/serializer y reiniciar/reconstruir imagenes.
```

Leccion:

> No todo 403 viene de roles. A veces el problema esta en un filtro, cache, serializacion o token propagado.

### Header `Idempotency-Key` bloqueado

Sintoma:

```text
POST /api/orders fallaba al pasar por gateway.
```

Causa:

```text
CORS no permitia el header Idempotency-Key.
```

Solucion:

```java
config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
```

Leccion:

> Si un request funciona directo al servicio pero falla por gateway, revisar CORS, rutas, headers permitidos y seguridad del gateway.

### `RequestBody` incorrecto en Feign

Sintoma probable:

```text
Feign no manda el body como esperas.
```

Causa:

```java
import io.swagger.v3.oas.annotations.parameters.RequestBody;
```

Solucion:

```java
import org.springframework.web.bind.annotation.RequestBody;
```

Leccion:

> En clients Feign, las anotaciones deben ser las de Spring MVC, no las de Swagger.

## 15. Checklist Para Agregar Seguridad a un Servicio Nuevo

Ejemplo: `catalog-query-service`.

### Paso 1. Agregar dependencia de security y commons

En el `pom.xml` del servicio:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>com.wwun.acme</groupId>
    <artifactId>acme-commons</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Paso 2. Crear `JwtAuthFilter`

Usar el mismo patron de product/order/cart:

```java
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtAuthConverter jwtAuthConverter;

    public JwtAuthFilter(JwtService jwtService){
        this.jwtAuthConverter = new JwtAuthConverter(jwtService);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(HEADER_AUTHORIZATION);

        if(authHeader != null && authHeader.startsWith(PREFIX_HEADER)){
            String token = authHeader.substring(PREFIX_HEADER.length()).trim();
            Authentication auth = jwtAuthConverter.convert(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
```

### Paso 3. Crear `SecurityConfig`

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception{
        return http.authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .csrf(csrf -> csrf.disable())
            .build();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtService jwtService) {
        return new JwtAuthFilter(jwtService);
    }
}
```

### Paso 4. Proteger controllers con `@PreAuthorize`

```java
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@PostMapping("/products/batch")
public ResponseEntity<List<CatalogProductResponseDTO>> getCatalogProducts(...)
```

### Paso 5. Propagar JWT en Feign

Crear:

```java
@Configuration
public class FeignSecurityConfig {

    @Bean
    public RequestInterceptor jwtFeignInterceptor(){
        return new JwtFeignInterceptor();
    }
}
```

Y usarlo:

```java
@FeignClient(name = "msvc-products", configuration = FeignSecurityConfig.class)
public interface ProductClient {
}
```

```java
@FeignClient(name = "msvc-inventories", configuration = FeignSecurityConfig.class)
public interface InventoryClient {
}
```

### Paso 6. Agregar ruta al gateway

La ruta debe apuntar al nombre Eureka del servicio:

```yaml
- id: catalog-query-service
  uri: lb://msvc-catalog-query
  predicates:
    - Path=/api/catalog/**
```

### Paso 7. Probar

```text
1. Login en /api/auth/login.
2. Copiar token.
3. Llamar /api/catalog/... con Authorization: Bearer <token>.
4. Verificar que catalog puede llamar product e inventory.
5. Probar sin token: debe fallar.
6. Probar con rol insuficiente: debe dar 403.
```

## 16. Que Decir en Entrevista

Version corta:

> En Acme Shop uso JWT stateless. `auth-service` emite el token despues de validar credenciales contra `user-service`. El token incluye `userId`, `email` y roles. El gateway valida tokens como OAuth2 Resource Server y bloquea requests no autenticadas. Los microservicios tambien validan el JWT con un filtro propio y aplican autorizacion fina con `@PreAuthorize`. Para llamadas Feign, propago el token actual con un interceptor para que los servicios downstream mantengan el mismo contexto de seguridad.

Version senior:

> Separaria autenticacion de autorizacion. El gateway hace seguridad gruesa: valida que el token sea valido y bloquea trafico obvio. Los microservicios hacen seguridad fina: roles, ownership y reglas de negocio. No confiaria solo en el gateway porque un servicio podria ser invocado internamente o mal configurado. Tambien evitaria secretos hardcodeados, usaria Config Server o secrets manager, expiracion corta, refresh tokens si aplica, logs con correlation id y pruebas de 401/403.

## 17. Mejoras Pendientes

- Implementar seguridad completa en `catalog-query-service`.
- Revisar `inventory-service/FeignSecurityConfig`, actualmente vacio.
- Estandarizar si todos los servicios usaran filtro custom o Resource Server.
- Agregar refresh tokens si se quiere flujo mas real.
- Agregar tests de seguridad:
  - sin token -> 401/403.
  - token invalido -> 401/403.
  - rol incorrecto -> 403.
  - rol correcto -> 200.
- Evitar `allowedOriginPatterns("*")` en produccion.
- Mover secretos a Config Server/secret manager.
- Agregar Swagger con soporte Bearer token.

## 18. Estado Mental del Proyecto

La documentacion anterior no estaba "mal"; estaba mezclando etapas.

Evolucion real:

```text
Etapa 1:
  Spring Security clasico con JwtAuthenticationFilter + JwtValidationFilter.

Etapa 2:
  JwtService mas limpio y reusable.

Etapa 3:
  acme-commons para compartir JWT entre servicios.

Etapa 4:
  auth-service separado de user-service.

Etapa 5:
  gateway valida JWT como Resource Server.

Etapa 6:
  microservicios aplican seguridad fina con @PreAuthorize.
```

Esta evolucion es normal. Lo importante para entrevista es explicar por que cambiaste:

> Empece con filtros custom para entender el mecanismo interno. Luego movi JWT a `acme-commons`, separe auth de user y deje el gateway como Resource Server para acercarme mas a una arquitectura de microservicios real.






















=========================

Resource Server vs Filtro Propio
Tu situación actual es híbrida:

Gateway:
  usa Spring Security OAuth2 Resource Server

Microservicios:
  usan JwtAuthFilter propio + JwtAuthConverter
Eso significa que el gateway usa el mecanismo estándar de Spring:

.oauth2ResourceServer(oauth2 -> oauth2.jwt())
Spring se encarga de leer el Bearer token, validar firma, expiración y crear el Authentication.

En cambio, tus microservicios hacen eso manualmente con:

JwtAuthFilter -> JwtAuthConverter -> JwtService
Eso funciona, y para aprender es excelente. Pero en producción real suele preferirse estandarizar para reducir código propio de seguridad.

La versión más production level sería:

Gateway:
  Resource Server

Product:
  Resource Server

Order:
  Resource Server

Inventory:
  Resource Server

Catalog:
  Resource Server
Así todos validan JWT con el mismo mecanismo estándar.

Entonces qué te conviene
Para este momento, yo haría esto:

Ahora:
  implementa catalog con el mismo patrón que ya tienes:
  SecurityConfig + JwtAuthFilter + FeignSecurityConfig

Luego:
  crea una mejora técnica documentada:
  "migrar microservicios a OAuth2 Resource Server"
¿Por qué? Porque si cambias todo ahora, te arriesgas a romper varios servicios justo cuando estás cerrando catalog. Primero termina el flujo. Después mejoras seguridad de forma controlada.

FeignSecurityConfig
Sí, para catalog-query-service lo necesitas si catalog llama a product-service e inventory-service, y esos servicios piden JWT.

Sin FeignSecurityConfig, pasa esto:

Cliente -> Catalog con Authorization ✅
Catalog -> Product sin Authorization ❌
Product responde 403
Con FeignSecurityConfig:

Cliente -> Catalog con Authorization ✅
Catalog -> Product con el mismo Authorization ✅
Catalog -> Inventory con el mismo Authorization ✅
Entonces para catalog necesitas:

@FeignClient(name = "msvc-products", configuration = FeignSecurityConfig.class)
y lo mismo para inventory.

Refresh Tokens
Ahora seguramente tienes solo access token.

Login -> access token -> se usa hasta expirar
Eso está bien para portfolio y MVP.

Más production level:

Login -> access token corto + refresh token largo
Ejemplo:

Access token: 15 minutos
Refresh token: 7 días / 30 días
El access token se usa en APIs. Cuando expira, el cliente manda el refresh token a:

POST /api/auth/refresh
y recibe un access token nuevo.

No lo haría ahora mismo antes de terminar catalog. Lo pondría como siguiente mejora de auth-service.

Security Tests
Estos sí convienen bastante para entrevista. Mínimo por servicio:

Sin token -> 401/403
Token inválido -> 401/403
Token con USER accediendo endpoint ADMIN -> 403
Token con ADMIN -> 200/201
Endpoint público health -> 200
Feign sin token propagado -> falla
Feign con token propagado -> funciona
Para empezar, testea catalog-query-service después de implementarlo.

Mover Secretos
Ahora el secreto JWT viene de config, pero lo importante production level es:

No hardcodear jwt.secret
No commitear secretos
Usar Config Server con repo privado, variables de entorno o secret manager
En producción real:

Local/dev:
  .env / docker compose env

Cloud:
  AWS Secrets Manager / Parameter Store
  Azure Key Vault
  GCP Secret Manager
  Kubernetes Secrets
Qué no debes dejar como hueco
Para catalog, checklist real:

1. SecurityConfig completo
2. JwtAuthFilter completo
3. FeignSecurityConfig completo
4. ProductClient usa FeignSecurityConfig
5. InventoryClient usa FeignSecurityConfig
6. Controller con @PreAuthorize
7. Gateway route hacia catalog
8. Config Server tiene config de catalog
9. Docker compose incluye catalog
10. Probar por gateway con Bearer token
11. Probar sin token
12. Probar token USER y ADMIN
Mi recomendación clara:

Hoy:
  cierra catalog con el patrón actual del proyecto.

Después:
  mejora security:
    - Resource Server en todos los microservicios
    - refresh tokens
    - security tests
    - secretos fuera del repo
