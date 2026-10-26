# Guía paso a paso: discovery-server

Es el servicio más corto: no tiene clases propias, solo la clase principal y su configuración. En la rama `universidad/microservicios` ya viene completo dentro del esqueleto. Aquí la tarea es **entender** qué hace cada línea y comprobar que funciona, porque todos los demás servicios dependen de él.

- Contrato: [README.md](README.md).
- Guía general y orden de trabajo: [docs/guia-paso-a-paso.md](../../../docs/guia-paso-a-paso.md).

## Paso 1. La clase principal

**Archivo:** `src/main/java/pe/edu/unu/apuestas/discovery/DiscoveryServerApplication.java`

- `@SpringBootApplication` arranca Spring Boot.
- `@EnableEurekaServer` convierte la aplicación en un servidor Eureka: un registro donde cada servicio anota su nombre (`spring.application.name`) y su dirección.
- La dependencia que lo hace posible está en el `pom.xml`: `spring-cloud-starter-netflix-eureka-server`.

**Para pensar:** ¿por qué el gateway puede llamar a `lb://ms-catalogo` sin conocer su IP? Porque le pregunta a Eureka qué instancias de `ms-catalogo` están registradas.

## Paso 2. La configuración

**Archivo:** `src/main/resources/application.yml`

| Propiedad | Para qué |
|---|---|
| `server.port: ${PORT:8761}` | Puerto estándar de Eureka. `${PORT:8761}` usa 8761 si no existe la variable `PORT`. |
| `eureka.client.register-with-eureka: false` | El servidor no se registra a sí mismo. |
| `eureka.client.fetch-registry: false` | Es el único Eureka: no descarga el registro de otro. |
| `eureka.server.enable-self-preservation: false` | En desarrollo los servicios se reinician seguido; sin esto, el panel mostraría servicios muertos con el aviso «EMERGENCY!». |
| `eureka.server.eviction-interval-timer-in-ms: 5000` | Cada 5 s quita los servicios que dejaron de enviar su latido. |

## Paso 3. Comprobar que funciona

```bash
cd servicios/renzo/discovery-server
./mvnw spring-boot:run
```

1. Abre http://localhost:8761. Debe mostrar el panel de Eureka, todavía sin instancias.
2. Arranca cualquier otro servicio (por ejemplo el esqueleto de `api-gateway`) y recarga el panel: debe aparecer en **Instances currently registered with Eureka** con estado `UP`.
3. Apaga ese servicio con Ctrl+C: en unos segundos desaparece del panel (eso es el `eviction-interval`).

## Paso 4. La prueba

**Archivo:** `src/test/java/pe/edu/unu/apuestas/discovery/DiscoveryServerApplicationTests.java`

Solo comprueba que el contexto arranca (`contextLoads`). Córrela con `./mvnw test`.

## Terminado cuando

- [ ] El panel abre en http://localhost:8761.
- [ ] Al final de la semana, los 8 servicios (gateway y los 7 de negocio) aparecen con estado `UP`.
- [ ] Puedes explicar con tus palabras qué pasaría si Eureka se cae: los servicios ya registrados siguen funcionando un rato con la lista que tienen guardada, pero ninguno nuevo se puede encontrar.
