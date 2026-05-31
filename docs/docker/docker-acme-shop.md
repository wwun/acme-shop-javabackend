# Docker y Runtime en Acme Shop

Este documento guarda lecciones aprendidas de Docker, Docker Compose y arranque de servicios.

No reemplaza los documentos de Security, Redis o Feign. Es el lugar para errores de build, imagenes, puertos, healthchecks y diferencias entre correr local vs correr en contenedores.

## 1. `ECONNREFUSED`

Error:

```text
connect ECONNREFUSED 127.0.0.1:8093
```

Significa:

```text
No hay ningun proceso escuchando en ese host/puerto desde el punto de vista del cliente.
```

En Acme paso cuando `catalog-query-service` no estaba realmente levantado en Docker.

Como diagnosticar:

```bash
docker compose ps catalog-query-service
docker compose logs --tail=100 catalog-query-service
```

Si el servicio no aparece o esta reiniciando, el problema no es Bruno ni el endpoint. El servicio no esta arriba.

## 2. `no main manifest attribute`

Error:

```text
no main manifest attribute, in app.jar
```

Significa:

```text
Docker esta ejecutando un jar que no es un Spring Boot executable jar.
```

Causa comun:

```text
El modulo no tiene spring-boot-maven-plugin con repackage.
```

Solucion en el `pom.xml` del microservicio:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <version>3.3.4</version>
            <executions>
                <execution>
                    <goals>
                        <goal>repackage</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Como confirmar:

```text
Maven debe mostrar:
  spring-boot:repackage
  Replacing main artifact ... with repackaged archive
```

## 3. Cuidado Con `COPY *.jar`

Problema:

Despues de `spring-boot:repackage`, Maven puede dejar dos archivos:

```text
catalog-query-service-1.0-SNAPSHOT.jar
catalog-query-service-1.0-SNAPSHOT.jar.original
```

Si el Dockerfile usa:

```dockerfile
COPY --from=acme-build /app/catalog-query-service/target/*.jar app.jar
```

puede copiar un artefacto no deseado o quedar ambiguo.

Solucion usada:

```dockerfile
COPY --from=acme-build /app/catalog-query-service/target/catalog-query-service-1.0-SNAPSHOT.jar app.jar
```

Respuesta senior:

> I avoid ambiguous jar wildcards in Dockerfiles when the build creates both the executable jar and `.jar.original`. I copy the expected artifact explicitly so the runtime image always uses the Spring Boot repackaged jar.

## 4. Imagen Base `acme-build`

En Acme, varios Dockerfiles copian jars desde una imagen base:

```dockerfile
COPY --from=acme-build /app/<service>/target/<service>.jar app.jar
```

Eso significa que si cambias codigo Java, a veces no basta con reconstruir solo el servicio final.

Flujo seguro:

```bash
docker compose build build-stage
docker compose build catalog-query-service
docker compose up -d catalog-query-service
```

Si no reconstruyes `acme-build`, el Dockerfile final puede copiar un jar viejo.

## 5. Variables Docker vs Config Server

Catalog obtenia config desde Config Server, pero Config Server no tenia Redis host.

Entonces Spring uso defaults:

```text
localhost:6379
```

Dentro de Docker eso estaba mal porque Redis corre en otro contenedor.

Solucion en Compose:

```yaml
catalog-query-service:
  environment:
    - SPRING_DATA_REDIS_HOST=redis
    - SPRING_DATA_REDIS_PORT=6379
```

Alternativa:

```text
Poner la misma configuracion en msvc-catalogs-docker.yml del repo de config.
```

Para production real, conviene que esto viva en configuracion externa/IaC, no hardcodeado en codigo.

## 6. Health Real

No asumir que existe:

```text
/api/<service>/health
```

Si no se implemento en el controller, no existe.

Spring Boot Actuator expone:

```text
/actuator/health
```

Ejemplo:

```bash
curl http://localhost:8093/actuator/health
```

Si responde `DOWN`, leer el componente que falla:

```text
redis DOWN
configServer DOWN
eureka DOWN
db DOWN
```

Eso apunta directamente al subsistema roto.

## 7. Checklist De Debugging

Cuando un servicio no responde:

```text
1. docker compose ps <service>
2. docker compose logs --tail=100 <service>
3. Revisar si el puerto esta publicado.
4. Revisar /actuator/health.
5. Si usa Config Server, consultar la config cargada.
6. Si usa Eureka, verificar que se registro.
7. Si usa Redis/DB/Kafka, revisar health del componente.
8. Si el error es por gateway, probar directo al servicio.
9. Si directo funciona y gateway no, revisar rutas/auth del gateway.
10. Si gateway devuelve 401, la ruta existe pero falta token valido.
```

Respuesta senior:

> I separate runtime issues from application issues. `ECONNREFUSED` usually means the process is not listening. A gateway 404 points to route discovery/config. A gateway 401 means the route matched but authentication failed. Actuator health helps identify whether the service is up but unhealthy due to Redis, database, Kafka, or discovery dependencies.

## 8. Multi-Stage Build

Docker puede construir la aplicacion en una etapa y ejecutar solo el jar en otra.

Ejemplo conceptual:

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/product-service/target/product-service-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Primera etapa:

```text
compila
tiene Maven
tiene codigo fuente
tiene poms
genera target/*.jar
```

Segunda etapa:

```text
solo tiene runtime Java
solo copia el jar final
no necesita Maven
no necesita codigo fuente
```

Ventaja:

- imagen final mas pequena
- menos superficie de ataque
- no empacas herramientas de build
- build reproducible

## 9. Build Stage Compartido En Acme

Acme usa una imagen local llamada:

```text
acme-build
```

Definida en:

[Dockerfile.base](/home/william/Documents/portfolio/acme-shop/Dockerfile.base)

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS build-stage
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests
```

Y en Compose:

```yaml
build-stage:
  build:
    context: .
    dockerfile: Dockerfile.base
  image: acme-build
  command: ["true"]
```

Luego cada servicio copia su jar desde esa imagen:

```dockerfile
COPY --from=acme-build /app/catalog-query-service/target/catalog-query-service-1.0-SNAPSHOT.jar app.jar
```

Esto funciono para Acme, pero tiene una consecuencia importante:

```text
si cambia codigo Java, debes reconstruir acme-build
```

Si solo reconstruyes el servicio final, puede copiar un jar viejo desde `acme-build`.

Flujo seguro:

```bash
docker compose build build-stage
docker compose build catalog-query-service
docker compose up -d catalog-query-service
```

O:

```bash
docker compose up --build
```

cuando quieras reconstruir todo.

## 10. Puertos Host vs Container

En Compose:

```yaml
ports:
  - "8093:8001"
```

Significa:

```text
8093 = puerto en tu maquina host
8001 = puerto dentro del contenedor
```

Bruno/browser/curl desde tu maquina usa:

```text
http://localhost:8093
```

Otros contenedores usan:

```text
http://catalog-query-service:8001
```

No:

```text
http://localhost:8093
```

Dentro de Docker, `localhost` es el contenedor actual.

## 11. Nombre De Servicio Docker Como DNS

Docker Compose crea DNS interno por nombre de servicio.

Ejemplos en Acme:

```text
config-server:8888
eureka-server:8761
redis:6379
kafka:29092
product-service:8001
```

Por eso en Docker debe usarse:

```yaml
spring:
  config:
    import: optional:configserver:http://config-server:8888
```

Y no:

```yaml
http://localhost:8888
```

## 12. Depends On No Es Suficiente

`depends_on` simple solo controla orden de arranque de contenedores.

No garantiza que la aplicacion ya este lista.

Mejor:

```yaml
depends_on:
  config-server:
    condition: service_healthy
```

Y el servicio debe tener healthcheck:

```yaml
healthcheck:
  test: ["CMD-SHELL", "wget -qO- http://localhost:8888/actuator/health | grep -q '\"status\"\\s*:\\s*\"UP\"'"]
  interval: 10s
  timeout: 5s
  retries: 10
  start_period: 20s
```

Esto evita arranques donde:

```text
servicio A arranca
pero config-server/eureka/db aun no estan listos
servicio A falla
```

## 13. Kafka En Docker

Kafka tiene una trampa clasica:

```yaml
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
```

Significa:

Desde contenedores:

```text
kafka:29092
```

Desde tu maquina host:

```text
localhost:9092
```

Si un microservicio dentro de Docker intenta usar `localhost:9092`, esta apuntando a si mismo, no al broker.

Regla:

```text
host local -> localhost:9092
contenedor -> kafka:29092
```

## 14. Redis En Docker

Redis en Acme corre como:

```yaml
redis:
  image: redis:7
  ports:
    - "6379:6379"
```

Desde host:

```text
localhost:6379
```

Desde contenedor:

```text
redis:6379
```

El problema que aparecio en `catalog-query-service` fue exactamente este:

```text
Spring uso localhost:6379 dentro del contenedor
```

Solucion:

```yaml
environment:
  - SPRING_DATA_REDIS_HOST=redis
  - SPRING_DATA_REDIS_PORT=6379
```

## 15. Secrets En Docker Compose

Compose es comodo para desarrollo, pero no debe guardar secretos reales.

En Acme hay variables sensibles en `docker-compose.yml` para OAuth.

Para production o repo publico, conviene moverlas a:

- `.env` no versionado
- Docker secrets
- Kubernetes Secrets
- Vault/cloud secrets manager
- variables del pipeline CI/CD

Ejemplo:

```yaml
environment:
  - GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
  - GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
```

Y en `.gitignore`:

```text
.env
```

Si alguna credencial real fue subida a Git:

```text
revocar
rotar
revisar historial
```

## 16. `.dockerignore`

`.dockerignore` evita copiar basura al build context.

Debe excluir:

```text
.git
target
node_modules
.idea
.vscode
*.log
.env
```

Beneficios:

- builds mas rapidos
- menos riesgo de copiar secretos
- imagenes mas limpias
- cache mas eficiente

## 17. Docker Compose Es Runtime Local, No Orquestador Enterprise

Docker Compose es excelente para:

- desarrollo local
- levantar dependencias
- probar integracion
- reproducir bugs
- demos

Pero no reemplaza Kubernetes en production enterprise.

Compose no te da de forma completa:

- rolling deployments
- autoscaling
- self-healing avanzado
- service accounts/RBAC real
- config/secrets enterprise
- ingress moderno
- scheduling multi-node
- network policies

Para Acme esta bien.

Para el sistema bancario nuevo, Docker Compose puede seguir siendo modo local rapido, pero el objetivo enterprise deberia ser Kubernetes.

## 18. Comandos Utiles

Levantar todo:

```bash
docker compose up --build
```

Levantar en background:

```bash
docker compose up -d --build
```

Ver servicios:

```bash
docker compose ps
```

Ver logs:

```bash
docker compose logs --tail=100 catalog-query-service
```

Seguir logs:

```bash
docker compose logs -f api-gateway-server
```

Reconstruir un servicio:

```bash
docker compose build catalog-query-service
docker compose up -d catalog-query-service
```

Reconstruir build-stage:

```bash
docker compose build build-stage
```

Bajar servicios:

```bash
docker compose down
```

Bajar y borrar volumenes:

```bash
docker compose down -v
```

Usar `down -v` con cuidado porque borra datos persistidos.

## 19. Paso a Paso Para Agregar Un Servicio A Docker

1. Crear Dockerfile del servicio.
2. Asegurar que el modulo genera Spring Boot executable jar.
3. Agregar servicio en `docker-compose.yml`.
4. Conectarlo a `acme-net`.
5. Definir `SPRING_PROFILES_ACTIVE=docker`.
6. Agregar dependencias con `depends_on`.
7. Usar service DNS interno, no `localhost`.
8. Publicar puerto solo si necesitas llamar directo desde host.
9. Agregar healthcheck si otros servicios dependen de el.
10. Probar `/actuator/health`.
11. Probar directo.
12. Probar por gateway.

## 20. Version Corta Para Entrevista

> In Acme Shop I use Docker Compose to run the local microservice environment: Config Server, Eureka, Gateway, services, Redis, Kafka, Prometheus and Grafana. Each service runs in its own container and communicates through Docker service names on a shared network.

## 21. Version Senior

> I treat Docker Compose as a local integration environment, not as production orchestration. It helps reproduce service dependencies, networking, ports, healthchecks and configuration issues. For production-style deployment, I would move the same services to Kubernetes using Deployments, Services, ConfigMaps, Secrets, probes, resource limits and Ingress/Gateway API.

## 22. Que No Decir

No diria:

Docker es lo mismo que Kubernetes.

Docker Compose es suficiente para production bancaria.

Si funciona en local, funciona igual en contenedores.

Dentro de Docker, `localhost` apunta a mi maquina.

`depends_on` garantiza que la app esta lista.

Puedo dejar secretos reales en `docker-compose.yml`.

`COPY *.jar` siempre es seguro.

## 23. Fuentes Utiles

- Dockerfile reference: https://docs.docker.com/reference/dockerfile/
- Docker Compose docs: https://docs.docker.com/compose/
- Docker build multi-stage docs: https://docs.docker.com/build/building/multi-stage/
