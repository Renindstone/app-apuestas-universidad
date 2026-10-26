# ms-usuarios · Renzo (Integrante 3) · puerto 8081 · BD `usuarios_db`

**Responsabilidad:** cuentas de usuario. Registra, inicia sesión (emite el JWT) y guarda el perfil y la banca. Es el único servicio que conoce las contraseñas.

## Spring Initializr

- Dependencias: **Spring Web**, **Spring Data JPA**, **PostgreSQL Driver**, **Validation**, **Eureka Discovery Client** y **Actuator**.
- Además, `spring-security-crypto` (solo para `BCryptPasswordEncoder`, sin el starter de Security completo), las librerías `jjwt` y `springdoc-openapi-starter-webmvc-ui`.

## Tabla `usuario`

Ver la sección 6.1 de la documentación. Al arrancar, si no existe, se crea el usuario `admin` con rol `ADMIN`. Su contraseña sale de la variable `ADMIN_PASSWORD` y nunca va en el código.

## Endpoints (vía gateway: `/api/usuarios` + ruta)

### `POST /auth/registro` (público)

```json
{ "nombreUsuario": "renzo", "email": "renzo@correo.pe", "clave": "Secreta123", "claveConfirmacion": "Secreta123" }
```

- **201:** `{ "id": 2, "nombreUsuario": "renzo", "rol": "APOSTADOR" }`.
- **400:** las contraseñas no coinciden o hay un campo no válido (clave de 6 a 72 caracteres, email válido).
- **409:** `{"error":"USUARIO_DUPLICADO","detalle":"El nombre de usuario ya está en uso"}`, o el mismo error con el correo.
- El rol **nunca** se toma del JSON: siempre es `APOSTADOR`.

### `POST /auth/login` (público)

```json
{ "nombreUsuario": "renzo", "clave": "Secreta123" }
```

- **200:** `{ "token": "eyJhbGciOi...", "tipo": "Bearer", "expiraEn": 28800, "rol": "APOSTADOR", "nombreUsuario": "renzo" }`.
- **401:** `{"error":"CREDENCIALES_INVALIDAS","detalle":"Usuario o contraseña incorrectos"}`. El mensaje es el mismo si falla el usuario o la clave.

Contenido del JWT (HS256, 8 h): `sub` = id del usuario, `usuario` = nombre de usuario y `rol`.

### `GET /me` (con token)

Lee `X-Usuario-Id`. Responde **200**: `{ "id": 2, "nombreUsuario": "renzo", "email": "renzo@correo.pe", "rol": "APOSTADOR", "banca": 500.00 }`.

### `PUT /me/banca` (con token)

Body `{ "banca": 500.00 }`. La banca debe ser mayor o igual a 0. Responde **200** con el perfil actualizado.

## Reglas de negocio

Reglas específicas de este servicio, con el mismo código que en la [sección 4.5 de la documentación](../../../docs/Documentacion.md#45-reglas-de-negocio). Si cambian, se actualizan los dos documentos y se avisa en el daily.

| ID | Regla | Relación |
|---|---|---|
| RN-USU-01 | El nombre de usuario (hasta 30 caracteres) y el correo son únicos. El correo se guarda en minúsculas, así que «Renzo@Correo.pe» y «renzo@correo.pe» cuentan como el mismo. Un duplicado responde 409 USUARIO_DUPLICADO y no se guarda nada. | RF-01 · CB-08 |
| RN-USU-02 | La clave tiene de 6 a 72 caracteres y debe coincidir con su confirmación. Solo se guarda su hash BCrypt, nunca la clave. | RF-01 |
| RN-USU-03 | Toda cuenta nueva recibe el rol APOSTADOR, aunque el JSON traiga otro. | RN-13 |
| RN-USU-04 | Si el inicio de sesión falla, el mensaje es el mismo cuando el usuario no existe y cuando la clave es incorrecta. Así no se revela qué cuentas existen. | RF-02 |
| RN-USU-05 | El token dura 8 horas. Cuando vence, hay que iniciar sesión otra vez. | RF-02 · CB-09 |
| RN-USU-06 | La banca es un monto que declara el usuario, mayor o igual a 0 (por defecto, 0). El sistema no mueve dinero real. | RN-08 |
| RN-USU-07 | Al arrancar, si no existe, se crea el usuario `admin`. Su clave sale de la variable `ADMIN_PASSWORD` y nunca se escribe en el código. | RN-13 |

Reglas generales que aplica: RN-13 (cuentas y roles).

## Tareas

- [x] Entidad, repositorio y servicio con BCrypt.
- [x] Registro y login con JWT.
- [x] `GET /me` y `PUT /me/banca`.
- [x] Usuario admin inicial (`CommandLineRunner`, en `config/AdminInicial`).
- [x] Pruebas: CB-08 (correo duplicado) y una de login correcto (`UsuarioServiceTest`), más el flujo completo por HTTP (`FlujoUsuarioTest`, con H2 en memoria).
- [x] Dockerfile.

**Nombres de columnas:** `application.yml` usa `PhysicalNamingStrategyStandardImpl` y `globally_quoted_identifiers`, así PostgreSQL guarda los nombres tal cual (`"UsuNombreUsuario"`). En DBeaver hay que escribirlos entre comillas: `SELECT "UsuNombreUsuario" FROM usuario;`.

**Variables de entorno:** `ADMIN_PASSWORD` (sin ella no se crea el admin y el log lo avisa), `JWT_SECRET` (la misma del gateway; en local, sin `.env`, ambos usan el mismo valor de desarrollo), `DB_HOST`, `DB_PORT`, `POSTGRES_USER`, `POSTGRES_PASSWORD` y `EUREKA_URL`.

## Pistas

- Guarden el correo con `toLowerCase()`, así evitan duplicados por mayúsculas.
- Generar el token: `Jwts.builder().subject(id).claim("rol", rol).claim("usuario", nombre).issuedAt(ahora).expiration(ahora+8h).signWith(Keys.hmacShaKeyFor(secreto.getBytes())).compact()`.
