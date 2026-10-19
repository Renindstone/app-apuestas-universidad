# ms-usuarios · Integrante 3 · puerto 8081 · BD `usuarios_db`

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

## Tareas

- [ ] Entidad, repositorio y servicio con BCrypt.
- [ ] Registro y login con JWT.
- [ ] `GET /me` y `PUT /me/banca`.
- [ ] Usuario admin inicial (`CommandLineRunner`).
- [ ] Pruebas: CB-08 (correo duplicado) y una de login correcto.
- [ ] Dockerfile.

## Pistas

- Guarden el correo con `toLowerCase()`, así evitan duplicados por mayúsculas.
- Generar el token: `Jwts.builder().subject(id).claim("rol", rol).claim("usuario", nombre).issuedAt(ahora).expiration(ahora+8h).signWith(Keys.hmacShaKeyFor(secreto.getBytes())).compact()`.
