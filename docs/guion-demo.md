# Guion de la demo (lunes 26/10/2026)

Unos 12 minutos. Todos los comandos se corren desde la raíz del repositorio.

## Antes de la demo (la noche anterior o una hora antes)

1. Levantar todo:
   ```bash
   docker compose -f infra/docker-compose.yml --env-file .env up -d --build
   ```
2. En el panel de Eureka (http://localhost:8761), revisar que aparezcan los 8 servicios en `UP`: el gateway y los 7 microservicios. Tardan medio minuto en registrarse.
3. Entrar a la web (http://localhost:8090) como `admin`, con la clave `ADMIN_PASSWORD` del `.env`. Si dice «Usuario o contraseña incorrectos», el admin se creó con una clave anterior (ver la retrospectiva, sección 7.1). Hay dos salidas: usar la clave antigua, o hacer `down -v` y levantar todo de nuevo, lo que **borra los datos**.
4. En **Ingesta**, ejecutar HISTORICO y luego PROXIMOS. Hace falta internet, porque los CSV se descargan de football-data.co.uk. Las 6 ejecuciones de HISTORICO deben quedar en OK.
5. Elegir de antemano un partido **PROGRAMADO de la Premier** (es la única liga con árbitro) y anotar su URL `/partidos/{id}`.
6. Crear un usuario apostador para la demo, por ejemplo `demo`, o crearlo en vivo en el paso 2.
7. **No** correr `down -v` el día de la demo: los datos de la ingesta viven en el volumen de PostgreSQL.

## Durante la demo

| # | Tiempo | Qué mostrar | Qué decir |
|---|---|---|---|
| 1 | 1 min | Panel de Eureka (:8761) y la diapositiva de arquitectura (sección 5.2) | 9 microservicios, cada uno con su base de datos. La web solo habla con el gateway, y el gateway encuentra a los servicios por Eureka. |
| 2 | 1 min | `/registro` y `/login` | El rol siempre es APOSTADOR (nunca se toma del formulario). El JWT se guarda en la sesión del servidor, no en el navegador (RN-WEB-02). |
| 3 | 2 min | **Hoy** (`/`) | La apuesta del día solo sale si una cuota de bet365 o Betano supera los umbrales (RN-06): probabilidad que mezcla modelo y mercado, EV ≥ 3 %. El lunes 26 solo hay un partido (Sevilla–Osasuna), así que lo más probable es que diga «Hoy no hay apuesta del día». Es el comportamiento esperado: no apostar también es una decisión. |
| 4 | 3 min | **Análisis** del partido elegido | Goles esperados (λ), la matriz de marcadores de Poisson, «Comparar con las casas» (cuota justa frente a la cuota de la casa) y «De dónde salen los números». Evaluar una cuota en el formulario: EV, stake sugerido y veredicto. |
| 5 | 1 min | **Cuota de Betano** del mismo partido | No hay una fuente gratuita de cuotas de Betano, así que el usuario la carga a mano y el sistema la compara igual. |
| 6 | 1 min | **Mis apuestas** | Registrar una apuesta y mostrar el rendimiento: ganadas, perdidas y ROI. La liquidación corre sola a las 06:00 de Lima. |
| 7 | 1 min | Salir y entrar como `admin`. **Ingesta** | Solo el ADMIN la ve. Tabla de ejecuciones: archivo, filas y estado. |
| 8 | 1 min | Terminal: seguridad | Ver los comandos de abajo: sin token responde 401, y una ruta interna responde 403 aunque se tenga token. |
| 9 | 1 min | Terminal: resiliencia | Detener `ms-cuotas` y recargar el análisis: las probabilidades siguen y aparece «Cuotas no disponibles por ahora» (CN-08). Volver a encenderlo. |
| 10 | 30 s | Terminal: pruebas, y la sección 8 del documento | 96 pruebas en verde en los 10 módulos y las 9 de caja negra aprobadas. |
| 11 | 30 s | Cierre | El modelo de Poisson es didáctico: no le gana a las casas de apuestas. El sistema solo recomienda: no apuesta ni maneja dinero (RN-08). |

### Comandos de los pasos 8 a 10

```bash
# Paso 8: seguridad
curl -i http://localhost:8080/api/apuestas/mis-apuestas                 # 401: no hay token
curl -i -X POST http://localhost:8080/api/catalogo/interno/partidos     # 403: ruta interna

# Paso 9: resiliencia (recargar el análisis entre los dos comandos)
docker stop app-apuestas-ms-cuotas-1
docker start app-apuestas-ms-cuotas-1      # tarda medio minuto en volver a Eureka

# Paso 10: pruebas de un servicio (unos 7 s con las dependencias ya descargadas)
cd servicios/ms-apuestas && ./mvnw test
```

## Si algo falla en vivo

- **Un servicio no aparece en Eureka, o la web dice «No pudimos conectar con el servidor»:** esperar medio minuto. Si sigue así, revisar `docker logs app-apuestas-<servicio>-1` y reiniciarlo con `docker restart app-apuestas-<servicio>-1`.
- **No hay internet:** no ejecutar la ingesta en vivo. Los datos de la noche anterior siguen en la base, y basta con mostrar la tabla de ejecuciones.
- **La laptop se queda sin memoria:** cerrar el IDE y las pestañas que sobren. El sistema necesita unos 5 GB.
