# Guía de inicio (martes 20, primera hora)

## 1. Requisitos de cada integrante

- **Java 21:** `java -version`.
- **Docker** con Docker Compose: `docker compose version`.
- **Git:** clonar el repo y cambiar a la rama `universidad/microservicios`.
- **IDE:** IntelliJ IDEA o VS Code con Extension Pack for Java.
- **Postman** o Bruno para probar las APIs.

```bash
git clone https://github.com/Renindstone/app-apuestas.git
```

```bash
git switch universidad/microservicios
```

El repositorio es privado: el dueño debe agregar a los otros 2 integrantes como colaboradores en GitHub (Settings → Collaborators).

## 2. Base de datos

```bash
cp .env.example .env
```

```bash
docker compose -f infra/docker-compose.yml --env-file .env up -d postgres
```

Esto levanta solo PostgreSQL 16 en `localhost:5441` con las 6 bases de datos ya creadas. Para verlas en DBeaver: host `localhost`, puerto `5441`, usuario y contraseña los del `.env`.

## 3. Crear cada servicio con Spring Initializr

En https://start.spring.io:

| Campo | Valor |
|---|---|
| Project | Maven |
| Language | Java 21 |
| Spring Boot | La estable más reciente **que permita elegir las dependencias de Spring Cloud** (Eureka, Gateway, OpenFeign). Si salen deshabilitadas, bajen una versión. **Los 3 integrantes usan la misma.** |
| Group | `pe.edu.unu.apuestas` |
| Artifact | El nombre del servicio, por ejemplo `ms-catalogo` |
| Package name | `pe.edu.unu.apuestas.catalogo` |
| Dependencias | Las que indica el README del servicio |

Descomprimir **dentro de `servicios/<integrante>/<servicio>/`**, sin crear otra carpeta encima. El `README.md` del servicio se queda donde está.

## 4. Configuración base de un servicio de negocio

Plantilla de `src/main/resources/application.yml`. Cambien el nombre, el puerto y la base de datos:

```yaml
server:
  port: ${PORT:8082}

spring:
  application:
    name: ms-catalogo
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5441}/catalogo_db
    username: ${POSTGRES_USER:apuestas}
    password: ${POSTGRES_PASSWORD:apuestas_dev}
  jpa:
    hibernate:
      ddl-auto: update        # suficiente para una semana; en producción se usaría Flyway
    open-in-view: false

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka}

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

Las variables `${...:valor}` usan el valor por defecto en local y se sobrescriben en Docker.

## 5. Orden para levantar en local

1. PostgreSQL (paso 2).
2. `discovery-server` → http://localhost:8761.
3. `api-gateway`.
4. Los servicios de negocio, en cualquier orden.
5. `web-app` → http://localhost:8090.

Desde la carpeta de cada servicio: `./mvnw spring-boot:run`, o con el botón Run del IDE.

## 6. Llamar a otro servicio con OpenFeign

1. `@EnableFeignClients` en la clase principal.
2. Una interfaz por servicio remoto, en el paquete `client`. El nombre es el `spring.application.name` del destino:

```java
@FeignClient(name = "ms-catalogo")
public interface CatalogoClient {
    @GetMapping("/partidos/{id}")
    PartidoDTO obtenerPartido(@PathVariable Long id);
}
```

3. Copien en su servicio **solo los DTO que necesitan**. No compartan un jar común: en microservicios cada servicio es dueño de su copia del contrato.
4. Si el otro servicio no responde, Feign lanza `FeignException` o `RetryableException`. Captúrenla en el `@RestControllerAdvice` y respondan **503** con el formato de error común (RNF-04).

## 7. Dockerfile (igual para todos los servicios)

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

En el `docker-compose.yml` completo, cada servicio recibe `EUREKA_URL=http://discovery-server:8761/eureka`, `DB_HOST=postgres` y `DB_PORT=5432`, y depende (`depends_on`) de `postgres` y `discovery-server`.

### Levantar todo con Docker

```bash
docker compose -f infra/docker-compose.yml --env-file .env up -d --build
```

- La primera vez tarda varios minutos, porque cada imagen descarga Maven y sus dependencias.
- **Direcciones:** la web queda en http://localhost:8090, el gateway en http://localhost:8080 y el panel de Eureka en http://localhost:8761.
- **Arranque:** los servicios tardan medio minuto en verse entre sí, que es lo que tarda Eureka en repartir el registro.
- **Ingesta:** para cargar datos, entra a la web como `admin` (con la clave `ADMIN_PASSWORD` del `.env`). En **Ingesta**, ejecuta HISTORICO y luego PROXIMOS.
- **Memoria:** cada JVM tiene un tope de 384 MB, unos 5 GB en total. Se puede cambiar con `JAVA_OPCIONES` en el `.env`.
- **Apagar:** `docker compose -f infra/docker-compose.yml down` apaga todo; con `-v` también borra los datos de PostgreSQL.

## 8. Flujo de Git

- Cada integrante trabaja en `feature/<servicio>-<tema>`, por ejemplo `feature/ms-catalogo-upsert`.
- Hace commits pequeños y abre un PR hacia `universidad/microservicios`, que **revisa otro integrante** (así se cumple el rol rotativo de QA).
- Antes del daily de la noche, cada uno sube lo que tenga, aunque sea con mocks.
