# Config Server en Acme Shop

Este documento consolida las notas de Spring Cloud Config Server y las alinea con la implementacion real de Acme Shop.

Config Server pertenece al tema de configuracion centralizada: como evitar que cada microservicio tenga configuracion duplicada, inconsistente o dificil de cambiar.

## 1. Idea Principal

Sin Config Server:

```text
product-service/application.yml
order-service/application.yml
cart-service/application.yml
inventory-service/application.yml
gateway/application.yml
```

Cada servicio guarda su propia configuracion.

Con Config Server:

```text
config repo Git
  -> config-server
      -> product-service
      -> order-service
      -> cart-service
      -> inventory-service
      -> gateway
```

Los microservicios preguntan al Config Server:

```text
quien soy?
que profile tengo?
dame mi configuracion
```

## 2. Que Problema Resuelve

Config Server ayuda con:

- centralizar configuracion
- versionar cambios con Git
- separar configuracion por ambiente
- evitar duplicacion entre servicios
- cambiar configuracion sin tocar el jar
- tener rollback de configuracion
- mantener nombres, URLs, puertos y parametros tecnicos fuera del codigo

Ejemplos de configuracion:

- `server.port`
- datasource URL
- Redis host/port
- Kafka bootstrap servers
- Eureka URL
- rutas del Gateway
- logging level
- JWT expiration
- feature flags simples
- timeouts
- resilience configs

## 3. Que No Debe Ir En Config Server

No debe ir:

- clases Java
- beans
- logica de negocio
- reglas de dominio complejas
- codigo
- tokens personales
- passwords en claro
- llaves privadas en claro

Regla:

```text
Config Server guarda valores de configuracion, no comportamiento de negocio.
```

Si un valor cambia por ambiente, puede ser candidato.

Si es secreto, debe ir cifrado o en un sistema de secrets.

## 4. Config Server En Acme Shop

Modulo:

[config-server](/home/william/Documents/portfolio/acme-shop/config-server)

Clase principal:

[ConfigServerApplication.java](/home/william/Documents/portfolio/acme-shop/config-server/src/main/java/com/wwun/acme/config_server/ConfigServerApplication.java)

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

`@EnableConfigServer` convierte este microservicio en servidor de configuracion.

## 5. Configuracion Del Servidor

Archivo:

[application.yml](/home/william/Documents/portfolio/acme-shop/config-server/src/main/resources/application.yml)

```yaml
server:
    port: 8888

spring:
    application:
        name: config-server
    cloud:
        config:
            server:
                git:
                    uri: https://github.com/wwun/acme-shop-config.git
                    default-label: main
                    clone-on-start: true
                    skip-ssl-validation: true
```

Significado:

`server.port: 8888`

Puerto tipico de Spring Cloud Config Server.

`spring.application.name: config-server`

Nombre del servicio.

`spring.cloud.config.server.git.uri`

Repositorio donde viven los archivos de configuracion.

`default-label: main`

Rama por defecto.

`clone-on-start: true`

El Config Server clona el repo al arrancar. Esto ayuda a detectar problemas temprano.

`skip-ssl-validation: true`

Util para resolver problemas locales, pero no es una buena practica para produccion.

En produccion se debe validar SSL correctamente.

## 6. Configuracion Del Cliente

Ejemplo real:

[product-service/application.yml](/home/william/Documents/portfolio/acme-shop/product-service/src/main/resources/application.yml)

```yaml
spring:
    application:
        name: msvc-products
    config: 
        import: optional:configserver:http://config-server:8888
```

Puntos importantes:

`spring.application.name`

Debe coincidir con el nombre del archivo en el repo de configuracion.

Si el servicio se llama:

```yaml
name: msvc-products
```

Config Server buscara archivos como:

```text
msvc-products.yml
msvc-products-docker.yml
msvc-products-dev.yml
msvc-products-prod.yml
```

`spring.config.import`

En Spring Boot moderno se usa esto:

```yaml
spring:
  config:
    import: optional:configserver:http://config-server:8888
```

Esto reemplaza el estilo antiguo basado en `bootstrap.yml/properties` para muchos casos.

`optional:`

Permite que la aplicacion pueda arrancar aunque Config Server no este disponible, si tiene configuracion local suficiente.

En produccion, para servicios que dependen criticamente de configuracion remota, quizas prefieras fail-fast en vez de optional.

## 7. Profiles

Los profiles separan configuracion por ambiente.

Ejemplo:

```text
msvc-products.yml
msvc-products-dev.yml
msvc-products-docker.yml
msvc-products-prod.yml
```

En Docker Compose, Acme usa:

```yaml
environment:
  - SPRING_PROFILES_ACTIVE=docker
```

Entonces el servicio pide configuracion para el profile `docker`.

Endpoint conceptual:

```text
http://config-server:8888/msvc-products/docker
```

Desde host local:

```text
http://localhost:8888/msvc-products/docker
```

## 8. Config Server En Docker Compose

En [docker-compose.yml](/home/william/Documents/portfolio/acme-shop/docker-compose.yml), `config-server` arranca primero:

```yaml
config-server:
  ports:
    - "8888:8888"
  healthcheck:
    test: ["CMD-SHELL", "wget -qO- http://localhost:8888/actuator/health | grep -q '\"status\"\\s*:\\s*\"UP\"'"]
```

Luego otros servicios dependen de el:

```yaml
depends_on:
  config-server:
    condition: service_healthy
```

Esto evita que un servicio intente pedir configuracion antes de que Config Server este listo.

Importante:

Dentro de Docker se usa:

```text
http://config-server:8888
```

No:

```text
http://localhost:8888
```

Porque `localhost` dentro de un contenedor apunta al mismo contenedor.

## 9. Config Server, Eureka Y Gateway

Orden tipico en Acme:

```text
config-server
  -> eureka-server
  -> microservicios
  -> api-gateway
```

Por que?

Eureka necesita configuracion.

Los microservicios necesitan configuracion antes de registrarse en Eureka.

El gateway necesita configuracion para rutas, seguridad y discovery.

## 10. Como Probar Config Server

Probar health:

```bash
curl http://localhost:8888/actuator/health
```

Probar configuracion de un servicio:

```bash
curl http://localhost:8888/msvc-products/docker
```

Formato general:

```text
/{application}/{profile}
/{application}/{profile}/{label}
```

Ejemplo:

```text
/msvc-products/docker/main
```

## 11. Git Como Backend

El repo Git de configuracion permite:

- versionar cambios
- revisar historial
- rollback
- pull requests
- auditoria basica

Pero no significa que cada request de cada microservicio va a Git.

Flujo:

```text
Config Server clona/lee Git
microservicio pide config al Config Server al arrancar
microservicio usa esa configuracion
```

Si Git cae despues de que los servicios arrancaron, los servicios no se caen automaticamente por eso.

Pero si Config Server necesita arrancar desde cero y no puede leer Git, puede fallar.

## 12. Refresh Dinamico

Spring Cloud permite refrescar configuracion con Actuator.

Opcion manual:

```text
POST /actuator/refresh
```

Opcion distribuida:

```text
Spring Cloud Bus + Kafka/RabbitMQ
POST /actuator/busrefresh
```

Para production real, refrescar config dinamicamente debe usarse con cuidado.

No toda configuracion es segura de cambiar en caliente.

Ejemplos que pueden ser peligrosos:

- datasource
- security
- secrets
- rutas criticas
- timeouts sin pruebas

## 13. Secrets

Tus notas tenian ejemplos de tokens personales. Eso es justo lo que no debe quedar en documentacion ni repositorios.

Si alguna vez esos tokens fueron reales:

```text
revocarlos inmediatamente
rotarlos
revisar historial Git
```

En Config Server:

No guardar secrets en claro.

Opciones:

- variables de entorno
- Spring Cloud Config encryption
- Kubernetes Secrets
- HashiCorp Vault
- cloud secrets manager
- SOPS/Sealed Secrets si usas GitOps

Ejemplo aceptable:

```yaml
jwt:
  secret: ${JWT_SECRET}
```

No:

```yaml
jwt:
  secret: miSecretRealEnClaro
```

## 14. Lo Academico vs Produccion

### Academico / Spring Cloud clasico

```text
Spring Cloud Config Server
Git repo de configuracion
Eureka
Gateway
Docker Compose
```

Es una buena forma de aprender:

- configuracion centralizada
- profiles
- config por servicio
- dependencia de arranque
- rutas gateway externalizadas

### Produccion moderna

Depende de la plataforma.

En Kubernetes, muchas empresas prefieren:

- ConfigMaps para configuracion no sensible
- Secrets o External Secrets para secrets
- Vault o cloud secrets manager
- GitOps con ArgoCD/Flux
- Helm/Kustomize

Spring Cloud Config Server no es malo.

Pero no siempre es el default en Kubernetes.

## 15. Observaciones De Acme

Buenas decisiones:

- `config-server` es modulo separado.
- Usa repo Git externo.
- Los servicios usan `spring.config.import`.
- Docker usa `config-server:8888`, no `localhost`.
- Compose espera healthcheck de Config Server.

Mejoras para production:

- no usar `skip-ssl-validation: true`
- proteger Config Server con auth/TLS
- no exponer Config Server publicamente
- no guardar secrets en claro
- definir fail-fast en servicios criticos
- documentar que propiedades vienen del repo remoto
- considerar Vault/Kubernetes Secrets para secretos

## 16. Paso a Paso Para Implementar

### Paso 1. Crear modulo config-server

Dependencia:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>
```

### Paso 2. Habilitar Config Server

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
}
```

### Paso 3. Configurar repo Git

```yaml
server:
  port: 8888

spring:
  application:
    name: config-server
  cloud:
    config:
      server:
        git:
          uri: https://github.com/org/project-config.git
          default-label: main
          clone-on-start: true
```

### Paso 4. Crear archivos en repo config

```text
msvc-products.yml
msvc-products-docker.yml
msvc-orders.yml
msvc-gateway.yml
```

### Paso 5. Configurar cliente

```yaml
spring:
  application:
    name: msvc-products
  config:
    import: optional:configserver:http://config-server:8888
```

### Paso 6. Activar profile

```yaml
environment:
  - SPRING_PROFILES_ACTIVE=docker
```

### Paso 7. Probar

```bash
curl http://localhost:8888/msvc-products/docker
```

### Paso 8. Proteger

- TLS
- auth interna
- secrets fuera de Git plano
- no exponer puerto fuera de red interna si no hace falta

## 17. Version Corta Para Entrevista

> Spring Cloud Config Server centralizes external configuration for Acme Shop. Services identify themselves with `spring.application.name` and import config from `config-server:8888`, while the server reads versioned YAML files from a Git repository.

## 18. Version Senior

> I treat Config Server as one implementation of centralized configuration. It is useful in Spring Cloud architectures, especially with Git-backed config and multiple profiles. In Kubernetes-based platforms, I would evaluate ConfigMaps, Secrets, External Secrets or Vault instead. Secrets should not live in Git in plain text, and config refresh must be controlled because not every property is safe to change at runtime.

## 19. Que No Decir

No diria:

Config Server reemplaza Kubernetes ConfigMaps.

Config Server debe guardar passwords en claro.

Config Server reemplaza Vault.

Si uso Git, los servicios dependen de Git en cada request.

`bootstrap.yml` siempre es obligatorio.

`optional:configserver` siempre es lo mejor para produccion.

## 20. Fuentes Utiles

- Spring Cloud Config docs: https://docs.spring.io/spring-cloud-config/reference/
- Spring Boot external config docs: https://docs.spring.io/spring-boot/reference/features/external-config.html
- Kubernetes ConfigMaps: https://kubernetes.io/docs/concepts/configuration/configmap/
- Kubernetes Secrets: https://kubernetes.io/docs/concepts/configuration/secret/
