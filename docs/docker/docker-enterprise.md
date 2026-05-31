# Docker Enterprise

Este documento es la version recomendada para pensar Docker en un sistema bancario o enterprise.

Docker no es la arquitectura completa. Docker resuelve empaquetado y runtime de contenedores. En production enterprise normalmente se combina con Kubernetes u otra plataforma de orquestacion.

## 1. Decision Principal

La pregunta no es:

```text
Uso Docker?
```

La pregunta correcta es:

```text
como construyo imagenes seguras, reproducibles, pequenas y listas para Kubernetes?
```

## 2. Docker vs Docker Compose vs Kubernetes

Docker:

```text
construye y ejecuta contenedores
```

Docker Compose:

```text
levanta varios contenedores juntos para desarrollo/local integration
```

Kubernetes:

```text
orquesta contenedores en production
```

Para un sistema bancario:

```text
Docker para imagenes
Docker Compose para dev/local
Kubernetes para production-like/production
```

## 3. Imagen Production-Ready

Una imagen production-ready deberia:

- usar multi-stage build
- copiar solo artefactos necesarios
- no incluir codigo fuente
- no incluir Maven/Gradle en runtime
- no incluir secrets
- usar usuario no-root si es posible
- tener version/tag claro
- tener health endpoint en la app
- ser reproducible
- ser escaneable por vulnerabilidades
- tener SBOM si la organizacion lo exige

## 4. Multi-Stage Build Recomendado

Ejemplo:

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY payment-service/pom.xml payment-service/
COPY acme-commons/pom.xml acme-commons/
RUN mvn -pl payment-service -am dependency:go-offline
COPY . .
RUN mvn -pl payment-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/payment-service/target/payment-service-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Mejoras:

- runtime usa JRE, no JDK, si no necesitas herramientas de compilacion
- dependencias se cachean antes de copiar todo el codigo
- se copia jar explicito, no wildcard ambiguo

## 5. Root vs Non-Root

En production, evitar correr como root dentro del contenedor si es posible.

Ejemplo:

```dockerfile
RUN addgroup -S app && adduser -S app -G app
USER app
```

Beneficio:

- reduce impacto si una app es comprometida
- mejor postura de seguridad
- suele ser requerido por plataformas enterprise

## 6. Secrets

Nunca guardar secrets en:

- Dockerfile
- imagen
- repositorio
- `docker-compose.yml` versionado
- logs

Usar:

- variables inyectadas por runtime
- Kubernetes Secrets
- External Secrets/Vault
- cloud secrets manager
- CI/CD secret store

Ejemplo:

```yaml
environment:
  - DB_PASSWORD=${DB_PASSWORD}
```

Y `.env` fuera de Git.

## 7. Tags De Imagen

No usar solo:

```text
latest
```

para production.

Usar tags trazables:

```text
payment-service:1.4.2
payment-service:git-sha-abc123
payment-service:2026-05-31-1430
```

En CI/CD:

```text
build -> test -> scan -> push -> deploy
```

## 8. Healthchecks

Docker Compose puede tener healthchecks.

En Kubernetes esto se transforma en:

- readinessProbe
- livenessProbe
- startupProbe

Spring Boot Actuator:

```text
/actuator/health
```

Readiness:

```text
puedo recibir trafico?
```

Liveness:

```text
debo reiniciar el contenedor?
```

No son lo mismo.

## 9. Recursos

En Compose puedes correr sin limites.

En Kubernetes/enterprise, definir:

- CPU requests/limits
- memory requests/limits
- JVM memory settings

Ejemplo:

```text
JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75
```

Si no controlas memoria JVM en contenedores, puedes tener OOMKills.

## 10. Networking

Regla de oro:

```text
localhost dentro de un contenedor es el propio contenedor
```

Entre servicios:

Docker Compose:

```text
redis:6379
kafka:29092
config-server:8888
```

Kubernetes:

```text
redis.default.svc.cluster.local:6379
payment-service:8080
```

## 11. Docker Compose En Enterprise

Docker Compose no suele ser production para bancos, pero si es util para:

- local dev
- integration environment
- demo tecnica
- levantar dependencias
- reproducir errores

Para banco, lo presentaria como:

```text
local development runtime
```

No como:

```text
production orchestration
```

## 12. CI/CD Con Docker

Pipeline esperado:

```text
checkout
unit tests
build jar
build image
scan image
push registry
deploy to environment
smoke tests
```

Herramientas:

- GitHub Actions/GitLab CI/Jenkins
- Docker Buildx
- Trivy/Grype/Snyk para scanning
- registry: GHCR/ECR/ACR/GCR/Harbor
- Kubernetes deploy con Helm/Kustomize/ArgoCD

## 13. Seguridad De Imagen

Practicas:

- usar imagenes oficiales o aprobadas
- pinnear versiones
- escanear CVEs
- no instalar paquetes innecesarios
- no correr root
- eliminar cache temporal
- no copiar `.git`
- usar `.dockerignore`
- rotar y no incluir secrets

## 14. Observability

Una app en contenedor debe escribir logs a stdout/stderr.

No depender de archivos locales dentro del contenedor.

Plataforma se encarga de recolectar:

- logs
- metrics
- traces

En Spring Boot:

- Actuator
- Micrometer
- Prometheus endpoint
- OpenTelemetry si aplica

## 15. Paso a Paso Para Banking Local

### Paso 1. Crear Dockerfile por servicio

Multi-stage build, jar explicito, runtime liviano.

### Paso 2. Crear Compose local

Levantar:

- postgres
- redis
- kafka
- keycloak
- servicios Spring
- prometheus/grafana si aplica

### Paso 3. Usar variables externas

`.env` local no versionado.

### Paso 4. Agregar healthchecks

Para dependencias y servicios principales.

### Paso 5. Probar red interna

Servicios deben hablar por nombre:

```text
postgres:5432
redis:6379
kafka:29092
```

### Paso 6. Crear manifests Kubernetes

Por cada servicio:

- Deployment
- Service
- ConfigMap
- Secret
- probes
- resources

### Paso 7. Migrar Compose a Kubernetes local

Usar:

- kind
- k3d
- minikube

### Paso 8. CI/CD

Construir imagen, testear, escanear, publicar y desplegar.

## 16. Que Implementaria En Tu Banking System

Primera fase:

- Dockerfile multi-stage por servicio
- Docker Compose para dev local
- `.env.example` sin secretos reales
- healthchecks
- logs a stdout
- Actuator

Segunda fase:

- Kubernetes local con `kind`/`k3d`
- manifests o Helm/Kustomize
- readiness/liveness probes
- resource limits
- image tags con git sha
- Trivy scan en CI

Tercera fase:

- GitOps con ArgoCD/Flux
- registry privado
- SBOM
- non-root containers
- External Secrets/Vault

## 17. Respuesta Para Entrevista

Version corta:

> I use Docker to package each microservice as an immutable image and Docker Compose for local integration. For production-style deployment, I move those images to Kubernetes with Services, ConfigMaps, Secrets, probes and resource limits.

Version banking:

> For a banking system, I would treat Docker images as deployable artifacts. The images should be minimal, reproducible, scanned for vulnerabilities, free of secrets and tagged with build metadata. Docker Compose is useful locally, but Kubernetes or an enterprise orchestrator should handle production scheduling, health, scaling and rollout.

## 18. Que No Diria

No diria:

Docker Compose es production-ready para banco.

Docker reemplaza Kubernetes.

La imagen puede contener secrets si el repo es privado.

`latest` es suficiente para production.

Healthcheck y readiness son lo mismo.

Si arranca en mi maquina, esta listo para production.

## 19. Fuentes Utiles

- Dockerfile reference: https://docs.docker.com/reference/dockerfile/
- Docker Compose docs: https://docs.docker.com/compose/
- Docker multi-stage builds: https://docs.docker.com/build/building/multi-stage/
- Kubernetes probes: https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/
