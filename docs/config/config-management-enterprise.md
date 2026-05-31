# Configuration Management Enterprise

Este documento es la version recomendada para pensar configuracion en un sistema bancario nuevo.

No esta centrado en Spring Cloud Config Server como unica opcion. Esta centrado en el problema real:

```text
como administrar configuracion y secretos de forma segura, auditable y portable
```

## 1. Decision Principal

La pregunta no es:

```text
Uso Config Server?
```

La pregunta correcta es:

```text
Donde vive la configuracion?
Donde viven los secretos?
Como se versionan?
Como se auditan?
Como llegan a los servicios?
Como se rotan?
Como se evita exponerlos?
```

## 2. Recomendacion Para Banking

Si el sistema nuevo corre en Kubernetes, mi recomendacion inicial seria:

```text
Config no sensible -> ConfigMaps
Secrets -> Kubernetes Secrets + External Secrets/Vault/cloud secrets manager
Deploy/config lifecycle -> GitOps con ArgoCD o Flux
```

No empezaria automaticamente con Spring Cloud Config Server salvo que:

- la organizacion ya lo use
- el stack sea Spring Cloud clasico
- no haya plataforma Kubernetes madura
- se necesite Git-backed config centralizado para muchos servicios Spring

## 3. Tipos De Configuracion

### Configuracion no sensible

Ejemplos:

- feature flags simples
- logging level
- URLs internas no secretas
- nombres de topicos Kafka
- timeouts
- limites
- rutas
- nombres de servicios

Puede vivir en:

- ConfigMap
- repo GitOps
- Config Server si aplica

### Secretos

Ejemplos:

- passwords de DB
- JWT signing key
- OAuth client secret
- API keys
- private keys
- tokens
- certificados

No deben vivir en Git en claro.

Pueden vivir en:

- Vault
- AWS Secrets Manager
- Azure Key Vault
- GCP Secret Manager
- Kubernetes Secrets con cifrado at-rest
- External Secrets Operator
- Sealed Secrets/SOPS para GitOps

## 4. Kubernetes ConfigMaps

ConfigMap guarda configuracion no sensible.

Ejemplo conceptual:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: payment-service-config
data:
  SPRING_PROFILES_ACTIVE: "prod"
  PAYMENT_TIMEOUT_MS: "3000"
```

Uso:

```yaml
envFrom:
  - configMapRef:
      name: payment-service-config
```

Ventaja:

- nativo Kubernetes
- simple
- visible para plataforma
- facil con GitOps

Cuidado:

- no usar para passwords
- cambios pueden requerir rollout/restart segun como se monten

## 5. Kubernetes Secrets

Kubernetes Secrets guarda datos sensibles, pero base64 no es cifrado fuerte por si solo.

Para production:

- habilitar encryption at rest
- limitar RBAC
- no exponer secrets en logs
- rotar periodicamente
- preferir integration con Vault/cloud secrets manager si aplica

Ejemplo conceptual:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: payment-service-secrets
type: Opaque
stringData:
  DB_PASSWORD: "..."
  PAYMENT_PROVIDER_API_KEY: "..."
```

En Git publico o repo normal, esto no debe ir en claro.

## 6. External Secrets / Vault

En empresas grandes, una practica comun es guardar secretos en un sistema dedicado:

- HashiCorp Vault
- AWS Secrets Manager
- Azure Key Vault
- GCP Secret Manager

Luego Kubernetes consume esos secretos mediante herramientas como:

- External Secrets Operator
- Secrets Store CSI Driver

Flujo:

```text
Vault / Cloud Secrets Manager
  -> External Secrets Operator
  -> Kubernetes Secret
  -> Pod
```

Ventaja:

- rotacion centralizada
- auditoria
- politicas de acceso
- menor exposicion en Git

## 7. GitOps

GitOps significa que el estado deseado del cluster vive en Git.

Herramientas:

- ArgoCD
- Flux

Se versiona:

- Deployments
- Services
- Ingress/Gateway API
- ConfigMaps
- referencias a Secrets
- Helm values
- Kustomize overlays

No se deberian versionar secretos en claro.

Si necesitas secrets en Git:

- SOPS
- Sealed Secrets
- External Secrets con referencias

## 8. Spring Cloud Config Server En Enterprise

Spring Cloud Config Server sigue siendo valido.

Tiene sentido si:

- arquitectura Spring Cloud clasica
- servicios Spring Boot fuera de Kubernetes
- empresa ya usa Config Server
- necesitas repo Git central para configuracion de servicios
- necesitas `/actuator/refresh` o Spring Cloud Bus

Pero en una plataforma Kubernetes moderna, muchas veces no es la primera opcion porque Kubernetes ya tiene mecanismos nativos.

Frase senior:

> I would not add Config Server by default to a Kubernetes platform. I would first evaluate ConfigMaps, Secrets, External Secrets and GitOps. Config Server is useful in Spring Cloud environments, but it is one option, not the architecture itself.

## 9. Config Server vs Kubernetes

| Tema | Config Server | Kubernetes ConfigMaps/Secrets |
| --- | --- | --- |
| Ecosistema | Spring Cloud | Kubernetes nativo |
| Backend comun | Git | API Kubernetes/GitOps |
| Cliente | Spring apps | cualquier app en Kubernetes |
| Secrets | requiere cifrado/integracion | Secrets nativos + external managers |
| Refresh dinamico | Actuator/Cloud Bus | rollout/reload/operator |
| Portabilidad fuera de K8s | Alta | Baja/media |
| Uso moderno K8s | Opcional | Muy comun |

## 10. Banking: Principios De Configuracion

Para banco, priorizar:

- auditabilidad
- separacion config/secrets
- least privilege
- rotacion de secretos
- encryption at rest
- encryption in transit
- no secrets en logs
- no secrets en imagen Docker
- no secrets en repo Git plano
- cambios revisados por PR
- rollback
- trazabilidad de cambios
- ambiente reproducible

## 11. Runtime Config vs Deploy-Time Config

No toda configuracion debe cambiarse en caliente.

### Deploy-time config

Se cambia con despliegue/rollout.

Ejemplos:

- datasource
- issuer OAuth
- Kafka bootstrap
- URLs criticas
- secrets

### Runtime config

Puede cambiar sin redeploy si esta bien controlada.

Ejemplos:

- feature flag
- limite no critico
- logging level temporal
- thresholds operativos

En banca, los cambios runtime deben estar auditados y controlados.

## 12. Feature Flags

Feature flags no siempre deben vivir en Config Server.

Para flags serios:

- LaunchDarkly
- Unleash
- FF4J
- Togglz
- plataforma interna

Sirven para:

- rollout gradual
- activar por usuario/tenant
- canary
- kill switch

No confundir:

```text
configuration management
```

con:

```text
feature flag platform
```

## 13. Paso a Paso Enterprise Para Banking

### Paso 1. Clasificar valores

Para cada propiedad:

```text
es secreto?
cambia por ambiente?
debe versionarse?
puede cambiar en runtime?
quien puede modificarlo?
como se audita?
```

### Paso 2. Definir backend

Para Kubernetes local:

```text
ConfigMaps para config no sensible
Kubernetes Secrets para secretos en dev/local
```

Para enterprise real:

```text
ConfigMaps + External Secrets + Vault/cloud secrets manager
GitOps con ArgoCD/Flux
```

### Paso 3. Definir estructura por ambiente

Ejemplo:

```text
k8s/
  base/
  overlays/
    dev/
    qa/
    prod/
```

Con Kustomize:

```text
base define manifests comunes
overlays ajustan config por ambiente
```

### Paso 4. Montar config en servicios

Opciones:

```text
env vars
mounted files
Spring Boot config tree
```

Spring Boot soporta leer config desde archivos montados, util para Kubernetes Secrets/ConfigMaps.

### Paso 5. Proteger secrets

Minimo:

- no subir secrets en claro
- RBAC limitado
- encryption at rest
- rotacion
- auditoria
- no imprimir env vars completas en logs

### Paso 6. Definir rollout

Cuando cambia config:

```text
se reinicia pod?
se hace rolling update?
se usa operator/reloader?
se refresca runtime?
```

Para production bancaria, prefiero cambios controlados por rollout sobre refresh magico para propiedades criticas.

### Paso 7. Probar

Probar:

- servicio arranca sin config requerida
- secreto faltante
- config invalida
- rollback de config
- permisos insuficientes
- rotacion de secreto
- cambio de ConfigMap

## 14. Que Implementaria En Tu Banking System

Primera version local fuerte:

- Kubernetes local con `kind` o `k3d`
- ConfigMaps por servicio
- Secrets por servicio para valores sensibles
- perfiles Spring por ambiente
- no secrets en Git plano
- manifests en `k8s/base` y `k8s/overlays/dev`

Segunda version:

- External Secrets Operator
- Vault local o mock
- ArgoCD/Flux opcional
- SOPS/Sealed Secrets si quieres GitOps con secrets cifrados

No empezaria con Config Server en el sistema bancario si el objetivo es Kubernetes moderno.

Pero si quieres mostrar experiencia Spring Cloud, puedes documentar que Acme uso Config Server y banking usara ConfigMaps/Secrets.

Eso muestra evolucion.

## 15. Respuesta Para Entrevista

Version corta:

> I separate configuration from secrets. Non-sensitive runtime configuration can live in ConfigMaps or a centralized config service, while secrets should live in a dedicated secret manager or Kubernetes Secrets with proper encryption and RBAC.

Version banking:

> For a Kubernetes-based banking platform, I would not put secrets in a Spring Config Server Git repo. I would use ConfigMaps for non-sensitive configuration, External Secrets or Vault/cloud secret manager for secrets, and GitOps for auditable deployment changes. Critical config changes should go through controlled rollout, not uncontrolled runtime refresh.

Version comparando con Acme:

> In Acme Shop I used Spring Cloud Config Server because it fits the Spring Cloud learning architecture with Eureka and Gateway. For a new banking system on Kubernetes, I would prefer Kubernetes-native config management with ConfigMaps, Secrets, External Secrets and GitOps.

## 16. Que No Diria

No diria:

Config Server es obligatorio en microservicios.

ConfigMaps reemplazan un secrets manager.

Base64 en Kubernetes Secret es suficiente seguridad.

Puedo guardar JWT secret o DB password en Git en claro.

Refresh dinamico siempre es mejor que redeploy.

Feature flags y configuration management son exactamente lo mismo.

## 17. Fuentes Utiles

- Spring Cloud Config docs: https://docs.spring.io/spring-cloud-config/reference/
- Spring Boot external config docs: https://docs.spring.io/spring-boot/reference/features/external-config.html
- Kubernetes ConfigMaps: https://kubernetes.io/docs/concepts/configuration/configmap/
- Kubernetes Secrets: https://kubernetes.io/docs/concepts/configuration/secret/
