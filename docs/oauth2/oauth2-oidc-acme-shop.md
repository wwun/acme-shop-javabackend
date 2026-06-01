# OAuth2/OIDC En Acme Shop

Este documento consolida las notas de OAuth2/OIDC y las alinea con la implementacion real de Acme Shop.

En Acme, OAuth2 se usa para login externo con Google/GitHub desde `auth-service`.

Luego `auth-service` emite un JWT interno para que el resto de microservicios usen el mismo modelo de seguridad.

La idea central:

```text
Google autentica al usuario.
Acme emite su propio JWT interno.
Los microservicios validan el JWT interno.
```

## 1. OAuth2 Vs OIDC

OAuth2:

```text
framework de autorizacion
```

Permite que una app acceda a recursos en nombre del usuario sin conocer su password.

OIDC:

```text
OAuth2 + identidad
```

OpenID Connect agrega el `ID Token`, que contiene identidad del usuario:

- `sub`
- `email`
- `name`
- `picture`
- `iss`
- `aud`
- `exp`

Cuando dices "login con Google", normalmente estas usando:

```text
OAuth2 Authorization Code Flow + OpenID Connect
```

No es solo OAuth2 puro.

## 2. Roles OAuth2

| Rol | Que es | En Acme |
| --- | --- | --- |
| Resource Owner | Usuario final | Cliente que inicia sesion con Google |
| Client | App que pide autorizacion | `auth-service` |
| Authorization Server | Emite tokens | Google / GitHub |
| Resource Server | API protegida por token | Google UserInfo o microservicios Acme |

Una confusion comun:

```text
OAuth2 no significa automaticamente login.
OIDC es lo que agrega identidad para login.
```

## 3. Flujo En Acme

Flujo simplificado:

```text
User Browser
  -> /oauth2/authorization/google
  -> Google login
  -> /login/oauth2/code/google
  -> auth-service success handler
  -> user-service upsert OAuth user
  -> auth-service genera JWT interno
  -> response con token Acme
```

Archivos principales:

- [SecurityConfig.java](/home/william/Documents/portfolio/acme-shop/auth-service/src/main/java/com/wwun/acme/auth/security/SecurityConfig.java)
- [OAuth2LoginSuccessHandler.java](/home/william/Documents/portfolio/acme-shop/auth-service/src/main/java/com/wwun/acme/auth/security/OAuth2LoginSuccessHandler.java)
- [OAuthUserClient.java](/home/william/Documents/portfolio/acme-shop/auth-service/src/main/java/com/wwun/acme/auth/feign/OAuthUserClient.java)
- [InternalUserOAuth2Controller.java](/home/william/Documents/portfolio/acme-shop/user-service/src/main/java/com/wwun/acme/user/controller/InternalUserOAuth2Controller.java)
- [UserServiceImpl.java](/home/william/Documents/portfolio/acme-shop/user-service/src/main/java/com/wwun/acme/user/service/UserServiceImpl.java)

## 4. SecurityConfig En Auth Service

`auth-service` habilita OAuth2 Login:

```java
.oauth2Login(oauth2 -> oauth2.successHandler(oAuth2LoginSuccessHandler))
```

Y permite rutas publicas:

```java
.requestMatchers("/api/auth/**", "/api/oauth2", "/oauth2/**","/login/oauth2/**").permitAll()
```

Esto es necesario porque:

- `/oauth2/authorization/google` inicia el login
- `/login/oauth2/code/google` recibe el callback
- si esas rutas exigieran JWT, el login nunca empezaria

## 5. Success Handler

El punto clave esta en:

[OAuth2LoginSuccessHandler.java](/home/william/Documents/portfolio/acme-shop/auth-service/src/main/java/com/wwun/acme/auth/security/OAuth2LoginSuccessHandler.java)

Despues de que Spring completa el login con Google, llega un `Authentication`.

El handler hace:

```java
OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
String registrationId = oauthToken.getAuthorizedClientRegistrationId();

OAuth2User oAuthUser = (OAuth2User) oauthToken.getPrincipal();
```

Luego obtiene atributos:

```java
String sub = oAuthUser.getAttribute("sub");
String email = oAuthUser.getAttribute("email");
String name = oAuthUser.getAttribute("name");
String picture = oAuthUser.getAttribute("picture");
```

`sub` es muy importante.

Es el identificador unico del usuario en el proveedor.

Para Google, `sub` es el id estable de ese usuario dentro de Google.

En Acme se manda a `user-service`:

```java
OAuthUserUpsertRequestDTO(provider, sub, email, name, picture)
```

## 6. Upsert En User Service

`auth-service` llama a `user-service`:

```java
@FeignClient(name = "msvc-users", contextId = "oauthUserClient", path = "/internal/oauth2")
public interface OAuthUserClient {
    @PostMapping("upsert")
    UserAuthResponseDTO upsert(@RequestBody OAuthUserUpsertRequestDTO request);
}
```

`user-service` hace upsert:

```text
si existe authProvider + providerSub -> actualiza email
si existe email -> asocia providerSub
si no existe -> crea usuario con ROLE_USER
```

Esto evita crear usuarios duplicados cuando el mismo usuario vuelve a entrar por Google.

## 7. JWT Interno

Despues del upsert, `auth-service` genera JWT interno:

```java
String token = jwtService.generateToken(user.id(), user.username(), user.email(), roles);
```

Esto significa:

```text
Google no autoriza directamente tus microservicios.
Google autentica al usuario.
Acme emite un token propio para su dominio.
```

Eso te permite:

- roles propios de Acme
- claims propios como `userId`
- expiracion propia
- autorizacion consistente entre servicios
- no acoplar todos los servicios al token de Google

## 8. Gateway Como Resource Server

El gateway valida el JWT interno como Resource Server:

[SecurityConfig.java](/home/william/Documents/portfolio/acme-shop/api-gateway-server/src/main/java/com/wwun/acme/api_gateway_server/security/SecurityConfig.java)

```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
```

En Acme hoy se valida con secret compartida:

```java
NimbusReactiveJwtDecoder.withSecretKey(...)
```

Esto funciona para aprender, pero en enterprise lo preferible es:

```text
issuer-uri / jwk-set-uri
llaves asimetricas
IdP como Keycloak/Okta/Auth0/Azure AD/Cognito
```

## 9. Tokens Involucrados

| Token | Lo emite | Uso |
| --- | --- | --- |
| Authorization Code | Google | Codigo temporal para intercambiar por tokens |
| Access Token externo | Google | Acceso a Google APIs |
| ID Token externo | Google/OIDC | Identidad del usuario autenticado |
| JWT interno | `auth-service` | Autorizacion dentro de Acme |

La separacion importante:

```text
ID Token de Google = identidad externa
JWT de Acme = token interno para microservicios
```

## 10. Scopes

Scopes tipicos:

```text
openid
profile
email
```

`openid` habilita OIDC y permite recibir identidad.

`profile` entrega nombre/foto.

`email` entrega email.

No pedir scopes que no necesitas.

Eso es principio de minimo privilegio.

## 11. Redirect URI

La redirect URI debe coincidir exactamente con la configurada en Google:

```text
http://localhost:8090/login/oauth2/code/{registrationId}
```

En local puede pasar por gateway.

En produccion deberia ser HTTPS:

```text
https://api.acme.com/login/oauth2/code/google
```

Errores comunes:

- redirect URI distinta
- usar `localhost` en ambiente deployed
- no permitir rutas OAuth2 en gateway
- callback apunta al servicio incorrecto

## 12. `state`, `nonce` Y PKCE

`state`:

```text
protege contra CSRF en el flujo de redireccion
```

Spring Security lo maneja por defecto.

`nonce`:

```text
ayuda a evitar replay del ID Token
```

PKCE:

```text
protege Authorization Code Flow, especialmente clientes publicos como SPA/mobile
```

En backend confidential client con client secret, PKCE puede no ser obligatorio, pero hoy es una practica moderna muy comun.

## 13. Lecciones Aprendidas En Acme

Buenas decisiones:

- `auth-service` centraliza login OAuth2
- `user-service` queda como fuente de verdad de usuarios/roles
- se guarda `provider` y `providerSub`
- se emite JWT interno con roles propios
- gateway valida JWT antes de enrutar
- se permite `/oauth2/**` y `/login/oauth2/**`

Mejoras necesarias para production-level:

- no guardar client secret en `docker-compose.yml`
- mover secrets a variables externas o secret manager
- rotar credenciales si fueron expuestas
- usar HTTPS en redirect URI
- configurar allowed origins concretos
- considerar PKCE
- manejar errores del success handler
- redirigir al frontend con token/session de forma segura en vez de escribir JSON directo en el browser
- considerar refresh tokens o session management
- estandarizar microservicios como Resource Servers

## 14. OAuth2 No Reemplaza Autorizacion Interna

Google puede decir:

```text
este usuario es william@example.com
```

Pero Google no sabe:

- si es `ROLE_ADMIN` en Acme
- si puede ver una orden
- si puede reservar stock
- si puede ejecutar un pago

Eso lo decide Acme.

Por eso `user-service` administra roles propios y `auth-service` emite JWT interno.

## 15. Version Corta Para Entrevista

> Acme uses OAuth2/OIDC for social login. Google authenticates the user, auth-service receives the OAuth2 user attributes, upserts the user in user-service, and then issues an internal JWT with Acme roles and userId for the rest of the microservices.

## 16. Version Senior

> I separate external identity from internal authorization. Google/OIDC proves who the user is, but my system still owns roles, permissions and domain authorization. After successful OAuth2 login, auth-service maps the external identity to an internal user and issues an internal JWT consumed by the gateway and services.

## 17. Que No Decir

No diria:

OAuth2 es autenticacion.

OAuth2 y OIDC son lo mismo.

El access token de Google deberia autorizar todos mis microservicios.

Google decide mis roles internos.

Puedo guardar client secret en Git/docker-compose sin problema.

Resource Server emite tokens.

Refresh token lo valida cada microservicio.

