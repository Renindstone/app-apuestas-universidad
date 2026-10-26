# Guía paso a paso: ms-usuarios

Cuentas de usuario: registro, inicio de sesión con JWT, perfil y banca. Es el primer servicio con base de datos, así que aquí aparece por primera vez el recorrido completo, el mismo que seguirán los demás servicios: **entidad → repositorio → DTO → excepciones → servicio → controlador → pruebas**.

- Contrato: [README.md](README.md) (endpoints, JSON y reglas RN-USU-01 a RN-USU-07). Tabla: sección 6.1 de la documentación.
- Guía general y orden de trabajo: [docs/guia-paso-a-paso.md](../../../docs/guia-paso-a-paso.md).
- Todos los archivos van en `src/main/java/pe/edu/unu/apuestas/usuarios/`. Para ver uno en la solución:
  `git show universidad/codigo-completo:servicios/renzo/ms-usuarios/src/main/java/pe/edu/unu/apuestas/usuarios/<archivo>`

## Paso 0. Lo que ya trae el esqueleto

- **`pom.xml`:**
  - Web, Data JPA, PostgreSQL, Validation, Eureka Client y Actuator.
  - `spring-security-crypto`: solo el `BCryptPasswordEncoder`, sin el starter completo de Spring Security.
  - Las librerías `jjwt` para firmar el token y `springdoc` para Swagger.
  - H2, la base de datos en memoria que usan las pruebas.
- **`application.yml`:**
  - El datasource apunta a `usuarios_db` en `localhost:5441`.
  - `PhysicalNamingStrategyStandardImpl` y `globally_quoted_identifiers` respetan los nombres de columna tal cual (`UsuNombreUsuario`).
  - `jwt.secreto` (variable `JWT_SECRET`), `jwt.horas: 8` y `admin.clave` (variable `ADMIN_PASSWORD`).
- **`src/test/resources/config/application.yml`:** en las pruebas cambia PostgreSQL por H2 y apaga Eureka.

Levanta PostgreSQL con Docker (ver la guía general) y luego `./mvnw spring-boot:run`. Debe arrancar en el puerto 8081.

## Paso 1. La entidad

**Archivos:** `entity/Rol.java` y `entity/Usuario.java`

- `Rol` es un enum con `ADMIN` y `APOSTADOR`.
- `Usuario` lleva `@Entity` y `@Table(name = "usuario")`, con dos `@UniqueConstraint` con nombre (`UQ_UsuNombreUsuario`, `UQ_UsuEmail`).

| Campo | Columna | Detalle |
|---|---|---|
| `id` | `UsuIdUsuario` | `@Id @GeneratedValue(strategy = IDENTITY)` |
| `nombreUsuario` | `UsuNombreUsuario` | hasta 30, no nulo |
| `email` | `UsuEmail` | hasta 100, no nulo, siempre en minúsculas |
| `claveHash` | `UsuClaveHash` | hasta 100, el hash BCrypt, **nunca** la clave |
| `rol` | `UsuRol` | `@Enumerated(EnumType.STRING)`, hasta 15 |
| `banca` | `UsuBanca` | `BigDecimal`, `precision = 12, scale = 2`, por defecto 0 |
| `fechaRegistro` | `UsuFechaRegistro` | `Instant`, `updatable = false` |

**Pistas:**
- JPA necesita un constructor sin argumentos. Hazlo `protected` y agrega otro público con los datos de la cuenta.
- Pon la fecha de registro en un método `@PrePersist`, así nadie se olvida de llenarla.
- No pongas setters para todo: solo `setBanca`, que es lo único que cambia.

**Comprueba:** arranca el servicio y mira en DBeaver que existe la tabla `usuario` en `usuarios_db`, con esas columnas.

## Paso 2. El repositorio

**Archivo:** `repository/UsuarioRepository.java`

Una interfaz que extiende `JpaRepository<Usuario, Long>`, con tres métodos que Spring Data arma solo a partir del nombre:

- `Optional<Usuario> findByNombreUsuario(String nombreUsuario)`
- `boolean existsByNombreUsuario(String nombreUsuario)`
- `boolean existsByEmail(String email)`

## Paso 3. Los DTO

**Carpeta:** `dto/`. Usa `record`: son inmutables y no necesitan getters.

| DTO | Campos |
|---|---|
| `RegistroRequest` | `nombreUsuario`, `email`, `clave`, `claveConfirmacion`, con validaciones: `@NotBlank`, `@Size(max = 30)`, `@Email`, `@Size(min = 6, max = 72)`. Escribe un mensaje en español en cada una. |
| `RegistroResponse` | `id`, `nombreUsuario`, `rol` |
| `LoginRequest` | `nombreUsuario`, `clave`, ambos `@NotBlank` |
| `LoginResponse` | `token`, `tipo`, `expiraEn`, `rol`, `nombreUsuario` |
| `UsuarioDTO` | `id`, `nombreUsuario`, `email`, `rol`, `banca`, más un método estático `de(Usuario)` |
| `BancaRequest` | `banca` (`@NotNull`) |
| `ErrorDTO` | `error`, `detalle`: el formato común de todos los servicios |

El rol **no** va en `RegistroRequest`: el servidor siempre pone `APOSTADOR` (RN-USU-03).

## Paso 4. Excepciones y manejador de errores

**Carpeta:** `exception/`

- `ApiException extends RuntimeException`: guarda un `HttpStatus` y un código corto.
- Una subclase por error:

| Clase | HTTP | Código |
|---|---|---|
| `ValidacionException` | 400 | `VALIDACION` |
| `CredencialesInvalidasException` | 401 | `CREDENCIALES_INVALIDAS` («Usuario o contraseña incorrectos») |
| `UsuarioNoEncontradoException` | 404 | `USUARIO_NO_ENCONTRADO` |
| `UsuarioDuplicadoException` | 409 | `USUARIO_DUPLICADO` |

`ManejadorErrores` lleva `@RestControllerAdvice` y un `@ExceptionHandler` para cada caso:

| Excepción | Respuesta |
|---|---|
| `ApiException` | su propio estado y código |
| `MethodArgumentNotValidException` | 400 `VALIDACION`, con el mensaje del primer campo que falló |
| `HttpMessageNotReadableException` | 400 `FORMATO_INVALIDO` (el JSON está roto) |
| `MissingRequestHeaderException` | 401: alguien llamó sin pasar por el gateway |
| `DataIntegrityViolationException` | 409: dos registros simultáneos chocaron con el UNIQUE |
| `Exception` | 500 `ERROR_INTERNO`, se registra en el log y no se muestran detalles técnicos |

## Paso 5. BCrypt y JWT

**Archivos:** `config/SeguridadConfig.java` y `service/JwtService.java`

- `SeguridadConfig` es una `@Configuration` con un `@Bean PasswordEncoder` que devuelve `new BCryptPasswordEncoder()`.
- `JwtService`:
  - recibe `@Value("${jwt.secreto}")` y `@Value("${jwt.horas}")`;
  - si la clave tiene menos de 32 bytes, no deja arrancar el servicio, igual que el gateway;
  - tiene un método `generar(Usuario)` que firma el token con `sub` = id, y los claims `usuario` y `rol`, `issuedAt` y `expiration` (8 h);
  - tiene un método `segundosDeVida()` para el campo `expiraEn` (28800).

## Paso 6. El servicio

**Archivo:** `service/UsuarioService.java`

- **`registrar`** (`@Transactional`):
  1. Las claves deben coincidir; si no, `ValidacionException`.
  2. Quita los espacios del nombre y pasa el correo a minúsculas (RN-USU-01).
  3. Si el nombre o el correo existen, `UsuarioDuplicadoException`.
  4. Guarda con `encoder.encode(clave)` y el rol `APOSTADOR`.
- **`login`** (`@Transactional(readOnly = true)`):
  1. Busca por nombre.
  2. Si no existe **o** la clave no coincide, lanza la misma `CredencialesInvalidasException` (RN-USU-04).
  3. Si todo está bien, devuelve el token.
- **`perfil(id)`** y **`actualizarBanca(id, banca)`**: la banca no puede ser negativa (RN-USU-06). Redondéala a 2 decimales.

**Para pensar:** si el usuario no existe y respondes enseguida, pero cuando existe tardas lo que tarda BCrypt, un atacante puede medir el tiempo y saber qué cuentas existen. La solución compara siempre contra un hash, aunque sea uno falso.

## Paso 7. Los controladores

**Archivos:** `controller/AuthController.java` y `controller/UsuarioController.java`

| Controlador | Ruta | Método | Detalle |
|---|---|---|---|
| `AuthController` (`/auth`) | `POST /registro` | `registrar` | `@ResponseStatus(HttpStatus.CREATED)` |
| | `POST /login` | `login` | |
| `UsuarioController` (`/me`) | `GET` | `perfil` | |
| | `PUT /banca` | `actualizarBanca` | |

- Usa `@Valid @RequestBody` para que se apliquen las validaciones del paso 3.
- En `/me`, el id del usuario llega en `@RequestHeader("X-Usuario-Id") Long usuarioId`. El servicio **no** valida el JWT: confía en que el gateway puso esa cabecera.

**Comprueba** (directo al servicio, sin el gateway):

```bash
curl -i -X POST localhost:8081/auth/registro -H 'Content-Type: application/json' \
  -d '{"nombreUsuario":"lucia","email":"Lucia@Correo.pe","clave":"Secreta123","claveConfirmacion":"Secreta123"}'
curl -i -X POST localhost:8081/auth/login -H 'Content-Type: application/json' \
  -d '{"nombreUsuario":"lucia","clave":"Secreta123"}'
curl -i localhost:8081/me -H 'X-Usuario-Id: 1'
```

Repite el registro con el mismo correo en minúsculas: debe responder 409.

## Paso 8. El admin inicial

**Archivo:** `config/AdminInicial.java`

Un `@Component` que implementa `CommandLineRunner`: su método `run` se ejecuta una vez, al terminar de arrancar.

1. Si ya existe `admin`, no hace nada.
2. Si `admin.clave` está vacía o tiene menos de 6 caracteres, avisa con `log.warn` y sigue. No inventa una clave.
3. Si no, crea `admin` con el correo `admin@apuestas.local` y el rol `ADMIN`.

Ojo: como solo se crea si no existe, cambiar `ADMIN_PASSWORD` después no cambia la clave del admin. Pasó durante el proyecto (sección 7.1).

## Paso 9. Pruebas

**Archivos:** `src/test/java/pe/edu/unu/apuestas/usuarios/service/UsuarioServiceTest.java` y `.../controller/FlujoUsuarioTest.java`

- **`UsuarioServiceTest`** es una prueba unitaria: el repositorio es un `mock(UsuarioRepository.class)` de Mockito, y el encoder es `new BCryptPasswordEncoder(4)`, que es más rápido. Casos:
  - CB-08: un correo duplicado con mayúsculas responde 409 y no guarda nada (`verify(repositorio, never()).save(any())`).
  - El registro guarda el hash, el correo en minúsculas y el rol APOSTADOR.
  - El login correcto devuelve un token que se puede leer con la misma clave.
  - El login fallido da el mismo mensaje con un usuario inexistente y con una clave mala.
  - Una banca negativa responde 400.
- **`FlujoUsuarioTest`** es una prueba de integración: lleva `@SpringBootTest` y arma `MockMvc` con `MockMvcBuilders.webAppContextSetup(contexto)`. Corre contra H2. Recorre registro → duplicado → login → perfil → banca, más un caso sin cabecera (401) y otro con datos no válidos (400).

## Terminado cuando

- [ ] Los 4 endpoints responden lo que dice el contrato, también a través del gateway (`/api/usuarios/...`).
- [ ] El token que genera este servicio lo acepta el gateway: misma `JWT_SECRET`.
- [ ] El admin se crea al arrancar.
- [ ] `./mvnw test` pasa.

Si no alcanza el tiempo, recupera solo este servicio desde la solución (ver la guía general).
