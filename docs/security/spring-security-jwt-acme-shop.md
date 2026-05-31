# Spring Security + JWT en Acme Shop

Este documento consolida las notas originales de Spring Security/JWT y las alinea con la implementacion actual de Acme Shop.

La idea importante: las notas antiguas explicaban varias formas de implementar JWT. El proyecto actual ya evoluciono. Hoy Acme Shop usa:

- `auth-service` para autenticar y emitir JWT.
- `user-service` para usuarios, roles, password hashing y validacion interna de credenciales.
- `acme-commons` para codigo compartido de JWT.
- `api-gateway-server` como borde de entrada y Resource Server.
- Microservicios de negocio con `JwtAuthFilter` propio y `@PreAuthorize`.
- Feign con propagacion del token cuando un servicio llama a otro.

## Como Usar Este Documento

Este documento tiene tres niveles:

```text
Secciones 1-14:
  Entender el flujo actual, las piezas internas y los errores reales del proyecto.

Seccion 15:
  Replicar seguridad rapido en un servicio nuevo usando el patron actual del proyecto.

Secciones 16-18:
  Preparar entrevista, mejoras production-level y recordar la evolucion tecnica.
```

No todo lo descrito aqui debe implementarse siempre.

Regla practica:

```text
Para entender internals:
  estudia JwtAuthFilter, JwtAuthConverter, SecurityContextHolder y AuthUserPrincipal.

Para mantener consistencia con Acme Shop hoy:
  usa el patron custom-filter actual en los microservicios.

Para un sistema nuevo production-level:
  preferir OAuth2 Resource Server, claims, scopes/roles, IdP y token propagation.
```

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

### AuthUserPrincipal

`AuthUserPrincipal` es un principal personalizado del proyecto.

En Spring Security, `Authentication` suele tener tres partes:

```text
principal:
  quien es el usuario

credentials:
  password, token o credencial usada

authorities:
  roles/permisos
```

En Acme Shop, el filtro custom crea algo parecido a:

```java
new UsernamePasswordAuthenticationToken(principal, token, authorities)
```

Donde:

```text
principal = AuthUserPrincipal(userId, username)
credentials = token JWT
authorities = ROLE_USER / ROLE_ADMIN
```

La razon de usar `AuthUserPrincipal` fue no depender solo del username.

En microservicios reales muchas veces necesitas:

```text
userId
username
roles
```

Ejemplo:

```java
SecurityUtils.getCurrentUserId()
SecurityUtils.getCurrentUsername()
```

Frase corta:

> `AuthUserPrincipal` define que datos del usuario quiero tener disponibles despues de validar el JWT.

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

### SecurityContextHolder

Esta linea:

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
```

significa:

```text
Dame el usuario autenticado de la request actual.
```

`SecurityContextHolder` es la memoria de seguridad de Spring para la request actual.

Flujo:

```text
JWT entra
  -> JwtAuthFilter
  -> JwtAuthConverter
  -> AuthUserPrincipal(userId, username)
  -> UsernamePasswordAuthenticationToken(principal, token, roles)
  -> SecurityContextHolder
```

Luego:

```text
@PreAuthorize puede revisar roles.
Controllers pueden recibir Authentication.
Services/helpers pueden usar SecurityUtils.
FeignInterceptor puede recuperar el token actual.
```

Resumen:

```text
SecurityContextHolder guarda quien soy en esta request.
AuthUserPrincipal define que datos tengo sobre ese usuario.
SecurityUtils lee esos datos de forma comoda.
JwtFeignInterceptor reutiliza el token para llamadas internas.
```

### SecurityUtils

`SecurityUtils` es una clase helper para evitar repetir esto por todo el codigo:

```java
SecurityContextHolder.getContext().getAuthentication().getPrincipal()
```

En lugar de eso, el proyecto usa:

```java
SecurityUtils.getCurrentUserId()
SecurityUtils.getCurrentUsername()
SecurityUtils.getCurrentToken()
SecurityUtils.isAdmin(auth)
```

Esto es util en services, interceptors o helpers donde no quieres pasar `Authentication` como parametro todo el tiempo.

Importante:

```text
SecurityUtils no es exclusivo del filtro custom.
Tambien puede existir con Resource Server.
```

Lo que cambia es como se lee el usuario actual.

Con filtro custom:

```text
Authentication principal = AuthUserPrincipal
Authentication credentials = token
```

Con Resource Server:

```text
Authentication normalmente es JwtAuthenticationToken
El token y los claims viven dentro del Jwt
```

Por eso, si se migra a Resource Server, `SecurityUtils` se puede adaptar para leer:

```text
JwtAuthenticationToken -> Jwt -> claims -> userId / username / roles
```

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

### Necesito `AuthUserPrincipal` si uso Resource Server?

No necesariamente.

Con Resource Server hay varias formas profesionales de acceder a `userId`.

#### Forma 1: leer el claim directamente desde el JWT

Si el token trae:

```json
{
  "sub": "william",
  "userId": "..."
}
```

puedes leerlo desde `JwtAuthenticationToken`:

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();

if (auth instanceof JwtAuthenticationToken jwtAuth) {
    String userId = jwtAuth.getToken().getClaimAsString("userId");
}
```

En este modelo, no necesitas `AuthUserPrincipal`.

#### Forma 2: usar `JwtAuthenticationConverter`

Spring Security permite configurar un converter para transformar claims del JWT en authorities o incluso adaptar el principal.

Esto sirve para:

```text
roles -> GrantedAuthority
scope -> authorities
userId -> principal/custom representation
```

Esta opcion es mas estandar que escribir todo el filtro manualmente.

#### Forma 3: mantener un helper tipo `SecurityUtils`

Aunque uses Resource Server, puedes mantener `SecurityUtils`, pero adaptado:

```text
Si Authentication es AuthUserPrincipal -> flujo custom actual.
Si Authentication es JwtAuthenticationToken -> flujo Resource Server.
```

Conclusion:

```text
Necesitar userId no obliga a usar JwtAuthFilter custom.
Resource Server tambien puede darte userId desde claims.
```

Frase de entrevista:

> En la version custom use `AuthUserPrincipal` para tener `userId` y `username` como principal. Si migro a Resource Server, puedo leer `userId` desde los claims del `JwtAuthenticationToken` o configurar un `JwtAuthenticationConverter`. No necesito mantener un filtro custom solo por necesitar el userId.

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

### Que es `RequestTemplate` en Feign

En `JwtFeignInterceptor` aparece este metodo:

```java
public void apply(RequestTemplate template){
    String token = SecurityUtils.getCurrentToken();
    
    if(token == null || token.isBlank()){
        return;
    }

    template.header(TokenJwtConfig.HEADER_AUTHORIZATION, TokenJwtConfig.PREFIX_HEADER + " " + token);
}
```

Aqui `template` no es `RestTemplate`.

`RequestTemplate` es una clase de Feign que representa la request HTTP saliente antes de enviarse.

Frase clave:

> `template` representa la request HTTP que Feign esta a punto de enviar.

Otra frase clave:

> Antes de que Feign mande la request real, te da oportunidad de modificarla.

Por eso el interceptor puede agregar headers:

```java
template.header("Authorization", "Bearer abc");
```

Flujo:

```text
catalog-query-service recibe:
  Authorization: Bearer abc

catalog-query-service llama product-service con Feign.

JwtFeignInterceptor se ejecuta antes de enviar la request.

RequestTemplate todavia es una request en construccion.

El interceptor agrega:
  Authorization: Bearer abc

Feign envia la request real a product-service.
```

Sin interceptor:

```text
catalog-query-service -> product-service
  sin Authorization

product-service:
  401/403
```

Con interceptor:

```text
catalog-query-service -> product-service
  Authorization: Bearer abc

product-service:
  valida token y permite la request
```

Analogía:

```text
RequestTemplate = sobre antes de enviarlo
JwtFeignInterceptor = persona que le pega la etiqueta Authorization
Feign = cartero que manda el sobre
```

Nota importante de estado actual:

- `inventory-service` tiene `FeignSecurityConfig`, pero actualmente esta vacio.
- `catalog-query-service` ya tiene `SecurityConfig`, `JwtAuthFilter` y `FeignSecurityConfig` con el patron custom actual.

Checklist para `catalog-query-service`:

```text
1. Verificar SecurityConfig.
2. Verificar JwtAuthFilter.
3. Verificar FeignSecurityConfig con JwtFeignInterceptor.
4. Verificar configuration = FeignSecurityConfig.class en ProductClient e InventoryClient.
5. Probar request por gateway con Bearer token.
6. Probar request sin token si la ruta debe ser protegida.
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

- Probar seguridad completa en `catalog-query-service` por gateway.
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

## 18. Lecciones Aprendidas: Catalog Security

### `JwtService` no encontrado

Error real:

```text
Parameter 0 of method jwtAuthFilter required a bean of type
com.wwun.acme.security.JwtService that could not be found.
```

Causa:

`JwtService` vive en `acme-commons`, dentro del paquete:

```text
com.wwun.acme.security
```

Pero `catalog-query-service` tiene su clase principal en:

```text
com.wwun.acme.catalog
```

Spring Boot escanea por defecto desde el paquete de la clase principal hacia abajo. Entonces:

```text
com.wwun.acme.catalog
  ve com.wwun.acme.catalog.*
  no ve com.wwun.acme.security.*
```

Solucion usada:

```java
@SpringBootApplication(scanBasePackages = {
    "com.wwun.acme.catalog",
    "com.wwun.acme.security"
})
public class CatalogQueryApplication {
}
```

Regla mental:

```text
Si un bean esta en otro modulo/paquete, no basta con tener la dependencia Maven.
Spring tambien debe escanearlo o importarlo como configuracion.
```

Respuesta senior:

> A shared library dependency only puts classes on the classpath. It does not automatically make every class a Spring bean unless component scanning or auto-configuration imports it. If a shared `JwtService` is defined in commons, each service must scan/import that configuration or move the application base package high enough.

### Gateway 404 vs 401

Durante pruebas con catalog:

```text
POST localhost:8090/api/catalogs -> 404
POST localhost:8090/api/catalogs -> 401
```

Como interpretarlo:

```text
404:
  El gateway no encontro ruta o la ruta no estaba cargada/servicio no estaba registrado todavia.

401:
  La ruta ya existe, pero el gateway esta bloqueando por token ausente/invalido.
```

Por eso, pasar de 404 a 401 puede ser una buena senal: significa que el gateway ya encontro la ruta y ahora el problema es autenticacion.

Para probar por gateway:

```http
POST http://localhost:8090/api/catalogs
Authorization: Bearer <token>
Content-Type: application/json
```

Body:

```json
{
  "productIds": [
    "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
  ]
}
```

### `permitAll` no elimina seguridad downstream

Aunque catalog permita `/api/catalogs/**`, catalog llama a product/inventory con Feign.

Si product/inventory tienen:

```java
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
```

entonces catalog necesita propagar el token cuando llama downstream.

Flujo:

```text
Cliente -> gateway -> catalog
catalog -> product
catalog -> inventory
```

Si catalog no manda `Authorization` a product/inventory, el primer servicio puede entrar, pero la llamada interna puede fallar con 401/403.

Respuesta senior:

> Public or semi-public aggregation endpoints can still call secured downstream services. In that case I need a clear decision: propagate the user token, use service credentials, or expose a read-only internal endpoint. I do not assume that `permitAll` at the BFF/catalog layer makes downstream authorization irrelevant.

## 19. Estado Mental del Proyecto

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
