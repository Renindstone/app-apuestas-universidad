# Una, dos o tres máquinas, con cualquier sistema operativo

Es el despliegue distribuido de la sección 5.6 del informe. Nadie escribe IP ni puertos: lo hace el **lanzador**
(`iniciar.cmd` en Windows, `./iniciar.sh` en Mac y Linux, los dos llaman a [Lanzador.java](Lanzador.java)).

## Lo que necesita cada máquina

- Docker Desktop (Windows o Mac) o Docker Engine (Linux). Si está cerrado, el lanzador lo abre y lo espera.
- Java 17 o superior (el equipo usa Java 21).
- El repo en la misma rama en todas las máquinas.
- Estar en la misma red. Si la wifi de la universidad no deja que los equipos se vean entre sí, usen el hotspot de un celular.

## Arrancar

1. **Primera máquina (el núcleo):** doble clic en `iniciar.cmd`, o `./iniciar.sh` en una terminal dentro del repo.
   ```
   ¿Qué levanto en esta máquina?
     1) Todo en esta máquina   ← Enter
     2) Núcleo + Datos          (la otra máquina pone Modelo y web)
     3) Núcleo + Modelo y web   (la otra máquina pone Datos)
     4) Solo el núcleo          (3 máquinas, como el informe)
   ```
2. **Otra máquina:** el mismo comando. Busca sola el núcleo en la red (prueba las 254 direcciones en un par de segundos;
   si hay otro Eureka en el aula, prefiere el que tiene nuestro lanzador o pregunta cuál es). La primera vez pide el
   **código del equipo** que muestra el núcleo (por ejemplo `K7QF-M2XD`) y lo guarda en el `.env`. Después ofrece primero
   **Lo que falta en la red**, así que basta con Enter. Si el núcleo ya tenía todo, lo que se elija se **mueve**: arranca aquí
   y, cuando ya está en Eureka, se apaga allá.
3. Dejar abierta la ventana del lanzador: es el **agente** de esa máquina. Se registra en Eureka (aparece como `AGENTE`)
   y avisa en la consola cuando otra máquina se une o se va.

La primera vez cada máquina construye sus imágenes (varios minutos). Después, los servicios tardan 1 o 2 minutos en aparecer en Eureka.

## Los repartos

| Máquinas | Máquina 1 (núcleo) | Máquina 2 | Máquina 3 |
|---|---|---|---|
| 1 | Todo | — | — |
| 2 (Joseph y Guillermo) | Núcleo + Datos | Modelo y web | — |
| 3 (sección 5.6) | **Núcleo:** postgres, discovery-server, api-gateway, ms-usuarios | **Datos:** ms-catalogo, ms-ingesta, ms-estadisticas | **Modelo y web:** ms-cuotas, ms-predicciones, ms-apuestas, web-app |

`ms-usuarios` va con el núcleo, porque firma los JWT con la misma clave que valida el gateway.
Con dos máquinas da igual cuál es el núcleo: el lanzador de la otra lo encuentra.

## El panel oculto

En cualquier máquina con el lanzador abierto: **http://localhost:8099**, o la **esquina inferior derecha de la web** (un cuadro
invisible de 28 × 28 px que abre el panel en otra pestaña). Desde otra computadora, por ejemplo la del profesor, esa esquina
no hace nada, porque el panel solo responde en la máquina donde corre.

Muestra cuántas máquinas hay conectadas, cuál es el núcleo y, por cada máquina, cada servicio con su estado:
**encendido** (registrado en Eureka), **arrancando** (el contenedor corre y todavía no se registra), **se reinicia** (falla al
arrancar: mira su log) o **apagado**. Se actualiza cada 5 segundos. Los servicios encendidos enlazan a su Swagger.

Desde ahí se enciende, apaga, replica o mueve cualquier servicio o bloque **en cualquier máquina**, sin terminal:

- `postgres`, `discovery-server` y `api-gateway` solo viven en el núcleo, porque las demás máquinas los buscan por su IP.
- `ms-ingesta` y `ms-apuestas` tienen tareas programadas (ingesta y liquidación): se **mueven**, nunca se duplican.
- El resto admite **réplica**: Eureka registra las dos instancias y el gateway reparte entre ellas.
- Un servicio que no está en ninguna máquina aparece arriba, con un botón para encenderlo en cada máquina.

Seguridad: el panel solo se abre desde la propia máquina (`localhost`, con un token en cada formulario y sin poder incrustarse en
otra página), y las órdenes entre máquinas van firmadas con el **código del equipo** (`CODIGO_EQUIPO` en el `.env`), incluyen
la IP de destino y caducan a los 2 minutos. Alguien en la misma wifi puede ver el panel de Eureka, pero no puede dar órdenes.
Mover primero enciende en la máquina de destino y solo apaga en la otra cuando el servicio ya responde.

## Demostración de tolerancia a fallos (dos máquinas)

1. **Réplica:** en el panel, en la máquina del núcleo, `ms-cuotas` → **Réplica aquí**. Eureka (http://IP-del-núcleo:8761) muestra
   2 instancias. Apagar la de la otra máquina: el análisis de un partido sigue mostrando las cuotas.
2. **Servicio caído:** apagar `ms-cuotas` en las dos. La web muestra el aviso en el encabezado y «Cuotas no disponibles por ahora», y
   las probabilidades siguen saliendo (CN-08). Volver a encenderlo.
3. **Se va una laptop:** cerrar el lanzador o apagar la máquina de Modelo y web. En la consola del núcleo sale «Se fue …», y en el
   panel, con **Encender en …** o **Mover Modelo y web aquí**, todo vuelve a la máquina que quedó.

## Comandos

| Comando (en Mac y Linux, `./iniciar.sh …`) | Qué hace |
|---|---|
| `iniciar` | Menú |
| `iniciar todo` · `iniciar nucleo,datos` · `iniciar modelo` · `iniciar nada` | Sin menú: esos bloques en esta máquina (`nada` = solo el panel) |
| `iniciar apagar` | Apaga lo que corre en esta máquina |
| `iniciar --ip 192.168.1.10` | Usa esa IP si la detectada no es la de la red correcta (por ejemplo, con una VPN) |
| `iniciar --nucleo 192.168.1.10` | No busca: usa ese núcleo |
| `iniciar --codigo K7QF-M2XD` | Usa ese código del equipo sin preguntarlo |
| `java infra/Lanzador.java --prueba` | Autoprueba de la lógica del lanzador, sin Docker |

Volver a ejecutar `iniciar` y dar Enter («Lo mismo que ya corre aquí») sirve para reparar: por ejemplo, si cambió la red y con ella la IP.
A mano sigue funcionando como antes: `docker compose -f infra/docker-compose.distribuido.yml --env-file .env up -d --build <servicios>`.

## Si algo no funciona

- **La otra máquina no encuentra el núcleo:** misma red (mejor un hotspot). En Windows, el lanzador ofrece abrir los puertos 5441,
  8761 y 8080-8099 solo para la red local (pide permiso de administrador). Para quitarlo después:
  `netsh advfirewall firewall delete rule name=AppApuestas`.
- **Un servicio «se reinicia»:** `docker logs app-apuestas-<servicio>-1`. Lo más común: la base del núcleo todavía no estaba lista
  (se arregla solo) o la otra máquina no llega a `IP-del-núcleo:5441`.
- **«El código del equipo no coincide»:** borra la línea `CODIGO_EQUIPO` del `.env` de la máquina que se une y vuelve a ejecutar
  `iniciar`: lo pide de nuevo. También pasa si los relojes de las máquinas difieren en más de 2 minutos.
- **Sin red:** el lanzador deja todo en esta máquina (usa `host.docker.internal`) y no se puede unir otra.
- **Lo que sigue siendo un punto único de falla:** la máquina del núcleo (PostgreSQL, Eureka y el gateway). Replicarlos queda fuera de esta versión.
