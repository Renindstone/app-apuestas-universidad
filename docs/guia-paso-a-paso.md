# Guía paso a paso: construir los microservicios

Esta guía explica cómo construir los 9 microservicios por cuenta propia, servicio por servicio, y qué hacer si el tiempo no alcanza. Cada servicio tiene su propia guía detallada en `servicios/<integrante>/<servicio>/GUIA.md`.

## Las dos ramas

| Rama | Qué tiene | Para qué sirve |
|---|---|---|
| `universidad/microservicios` | Los servicios en **esqueleto** y las guías | Aprender construyendo cada servicio |
| `universidad/codigo-completo` | El código **completo y probado**: 96 pruebas en verde y las 9 de caja negra (sección 8 de la documentación) | Consultar la solución de un paso, recuperar un servicio si no alcanza el tiempo, y explicar el código ya hecho |

La documentación, los contratos, la infraestructura y la `web-app` son iguales en las dos ramas.

## Qué trae el esqueleto

Cada microservicio de `universidad/microservicios` arranca, pero todavía no hace nada. Ya trae:

- `README.md`: el **contrato** (endpoints, JSON y reglas de negocio);
- `GUIA.md`: los pasos para construirlo;
- `pom.xml` con todas las dependencias, `mvnw` y el `Dockerfile`;
- `src/main/resources/application.yml` con la configuración (base de datos, Eureka, JWT y propiedades propias);
- la clase principal `*Application.java`, con sus anotaciones (`@EnableFeignClients`, `@EnableScheduling`…);
- la prueba `*ApplicationTests` (`contextLoads`), y `src/test/resources/` con la configuración de H2 para las pruebas y los CSV de prueba de `ms-ingesta`.

Falta todo lo demás: `entity`, `repository`, `dto`, `exception`, `service`, `client`, `controller`, `config` y las pruebas. Eso es lo que construyen ustedes.

Dos excepciones:
- `discovery-server` ya viene completo, porque no tiene más que la clase principal y su configuración. Su guía explica qué hace cada línea.
- La `web-app` no es un microservicio y está completa en las dos ramas.

## Orden de construcción

Cada servicio necesita que los que llama ya existan. Este orden respeta esas dependencias:

| Orden | Servicio | Dueño | Necesita | Guía |
|---|---|---|---|---|
| 1 | discovery-server | Renzo | — | [GUIA](../servicios/renzo/discovery-server/GUIA.md) |
| 2 | api-gateway | Renzo | discovery-server | [GUIA](../servicios/renzo/api-gateway/GUIA.md) |
| 3 | ms-usuarios | Renzo | PostgreSQL | [GUIA](../servicios/renzo/ms-usuarios/GUIA.md) |
| 4 | ms-catalogo | Guillermo | PostgreSQL | [GUIA](../servicios/guillermo/ms-catalogo/GUIA.md) |
| 5 | ms-estadisticas | Guillermo | PostgreSQL | [GUIA](../servicios/guillermo/ms-estadisticas/GUIA.md) |
| 6 | ms-cuotas | Joseph | ms-catalogo | [GUIA](../servicios/joseph/ms-cuotas/GUIA.md) |
| 7 | ms-ingesta | Guillermo | ms-catalogo, ms-estadisticas y ms-cuotas | [GUIA](../servicios/guillermo/ms-ingesta/GUIA.md) |
| 8 | ms-predicciones | Joseph | ms-catalogo y ms-estadisticas | [GUIA](../servicios/joseph/ms-predicciones/GUIA.md) |
| 9 | ms-apuestas | Joseph | ms-catalogo, ms-predicciones, ms-cuotas y ms-estadisticas | [GUIA](../servicios/joseph/ms-apuestas/GUIA.md) |

Los tres pueden empezar el mismo día: cada guía arranca por lo que no depende de nadie.

- **Renzo:** el gateway y `ms-usuarios` solo necesitan Eureka y PostgreSQL.
- **Guillermo:** el catálogo y las estadísticas solo necesitan PostgreSQL; la lectura del CSV de la ingesta se prueba con archivos de prueba.
- **Joseph:** las fórmulas de las cuotas, el modelo de Poisson y el valor esperado son funciones puras que se prueban sin ningún otro servicio.

**Para empezar,** lean la [guía de ms-usuarios](../servicios/renzo/ms-usuarios/GUIA.md) aunque no sea su servicio. Es la que explica con más detalle el recorrido que siguen todos: entidad → repositorio → DTO → excepciones → servicio → controlador → pruebas.

## Preparar el entorno

Desde la raíz del repo:

```bash
cp .env.example .env                       # solo la primera vez
docker compose -f infra/docker-compose.yml --env-file .env up -d postgres
```

Si tienen encendido el `docker-compose` completo, apáguenlo primero. Sus contenedores ocupan los puertos de Eureka y del gateway y, si su servicio también corre en Docker, Eureka repartiría las peticiones entre los dos. **Sin `-v`**, para no borrar los datos:

```bash
docker compose -f infra/docker-compose.yml --env-file .env down
```

Luego, cada uno en su propia terminal:

```bash
cd servicios/renzo/discovery-server && ./mvnw spring-boot:run     # panel en http://localhost:8761
cd servicios/<integrante>/<servicio> && ./mvnw spring-boot:run    # el servicio que estás construyendo
```

- Mientras desarrollan, prueben el servicio **directo en su puerto** (8081 a 8087), sin el gateway ni el token. Donde el servicio pida el usuario, manden la cabecera a mano: `-H 'X-Usuario-Id: 1'`.
- Para probar por el gateway (`http://localhost:8080/api/...`), levanten también `api-gateway`.
- Para usar la web completa con los servicios que ya tengan, levanten también `servicios/renzo/web-app` (http://localhost:8090).

**Si Hibernate se queja de una columna:** la base de datos ya tiene las tablas de la versión completa, y `ddl-auto: update` agrega columnas pero nunca borra las viejas. Si su entidad tiene otros nombres, borren esa tabla en DBeaver (`DROP TABLE nombre;`) y dejen que se cree de nuevo.

## Cómo trabajar cada paso

1. Lean el paso en la `GUIA.md` y la parte del contrato (`README.md`) que le corresponde.
2. Escriban el código **sin mirar la solución**. Para eso son las pistas.
3. Compruébenlo con el `curl` o la prueba que indica el paso.
4. Recién entonces, compárenlo con la solución:

   ```bash
   # El archivo tal como quedó en la solución
   git show universidad/codigo-completo:servicios/guillermo/ms-catalogo/src/main/java/pe/edu/unu/apuestas/catalogo/entity/Partido.java

   # Qué cambia entre su versión y la solución
   git diff universidad/codigo-completo -- servicios/guillermo/ms-catalogo/src/main/java/pe/edu/unu/apuestas/catalogo/entity/Partido.java
   ```

   Si la rama ya está en GitHub, también pueden elegir `universidad/codigo-completo` en el selector de ramas y abrir el archivo.

5. Hagan un commit pequeño por paso, en su rama `feature/<servicio>-<tema>`, y abran un PR hacia `universidad/microservicios`.

No tienen que escribir el código igual a la solución. Lo que debe coincidir es el **contrato**: las rutas, el JSON y los códigos HTTP. Si su servicio cumple el contrato, los demás servicios y la web funcionan con él.

## Si no alcanza el tiempo

**Opción 1: recuperar solo un servicio.** Trae a la rama actual el código completo de ese servicio y deja los demás como están. **Reemplaza** lo que tengan en esa carpeta, así que hagan commit antes de lo que quieran conservar. `--no-overlay` también borra las clases suyas que la solución no tiene, para que no choquen con las de la solución.

```bash
git checkout --no-overlay universidad/codigo-completo -- servicios/joseph/ms-apuestas
git commit -m "Recupera ms-apuestas desde la solución"
```

**Opción 2: usar la rama completa para la demo.**

```bash
git switch universidad/codigo-completo
docker compose -f infra/docker-compose.yml --env-file .env up -d --build
```

Ojo: si levantan el `docker-compose` completo con `--build` desde `universidad/microservicios`, las imágenes se construyen con los esqueletos. Los servicios arrancan, pero no responden nada útil, y la web muestra errores.

## Explicar el código ya hecho

Si terminan usando la rama completa, las guías sirven como mapa para explicarla. Cada paso nombra los archivos que lo resuelven, en el orden en que conviene leerlos, y cuenta **por qué** se hizo así: la idempotencia del upsert, el día de Lima, las dos filas por partido, `@Async` en otro bean, el suavizado del modelo, la mezcla con el mercado…

Cada guía termina con sus pruebas: son la mejor forma de mostrar en la demo que una regla de negocio se cumple.
