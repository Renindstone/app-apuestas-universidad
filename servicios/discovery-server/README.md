# discovery-server (Eureka) · Integrante 3 · puerto 8761

**Responsabilidad:** registro y descubrimiento de servicios. Cada microservicio se registra al arrancar con su nombre (`spring.application.name`). Feign y el gateway lo encuentran por ese nombre (`lb://ms-catalogo`), sin IPs fijas.

## Spring Initializr

- Dependencias: **Eureka Server**.
- Clase principal con `@EnableEurekaServer`.

## Configuración clave (`application.yml`)

| Propiedad | Valor | Por qué |
|---|---|---|
| `server.port` | 8761 | Puerto estándar de Eureka |
| `spring.application.name` | discovery-server | |
| `eureka.client.register-with-eureka` | false | El servidor no se registra a sí mismo |
| `eureka.client.fetch-registry` | false | |

## En los demás servicios

Agregar la dependencia **Eureka Discovery Client** y esta propiedad:

```yaml
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka}
```

`EUREKA_URL` se sobrescribe en Docker con `http://discovery-server:8761/eureka`.

## Tareas

- [ ] Proyecto creado y arrancando en 8761.
- [ ] Panel en http://localhost:8761 que muestra los otros 8 servicios registrados (el gateway y los 7 de negocio).
- [ ] Dockerfile.

**Hecho cuando:** los 8 servicios aparecen con estado UP en el panel.
