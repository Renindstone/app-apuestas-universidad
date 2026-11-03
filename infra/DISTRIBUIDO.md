# Probar la app en 3 computadoras (tolerancia a fallos)

La idea: repartir los servicios en 3 máquinas, apagar uno (o una máquina entera) y ver que la web sigue funcionando.
Todo se hace con `infra/docker-compose.distribuido.yml`.

## Reparto sugerido

| Máquina | Servicios |
|---|---|
| A | `postgres`, `discovery-server`, `api-gateway`, `web-app` |
| B | `ms-usuarios`, `ms-catalogo`, `ms-ingesta`, `ms-cuotas`, `ms-predicciones` |
| C | `ms-estadisticas`, `ms-apuestas`, `ms-cuotas`, `ms-predicciones` |

`ms-cuotas` y `ms-predicciones` van en B y en C: son las **réplicas**. Eureka las registra a las dos y el gateway reparte entre ellas.
No conviene repetir `ms-ingesta` ni `ms-apuestas`: tienen tareas programadas que correrían dos veces.

## Preparación (una vez)

1. Las 3 máquinas en la misma red wifi. Anota la IP de cada una (`ipconfig`).
2. En cada máquina, copia `.env.example` a `.env` y agrega estas líneas (`IP_ESTA_MAQUINA` cambia en cada una, las demás son iguales en las 3):
   ```
   IP_ESTA_MAQUINA=<IP de esta máquina>
   IP_EUREKA=<IP de A>
   IP_POSTGRES=<IP de A>
   IP_GATEWAY=<IP de A>
   IP_WEB=<IP de A>
   ```
   El `JWT_SECRET` tiene que ser **el mismo** en las 3 máquinas.
3. Abre en el firewall de Windows los puertos 5441, 8080 a 8090 y 8761.

## Levantar

Desde la raíz del repo, primero la máquina A y después B y C:

```
# A
docker compose -f infra/docker-compose.distribuido.yml --env-file .env up -d --build postgres discovery-server api-gateway web-app
# B
docker compose -f infra/docker-compose.distribuido.yml --env-file .env up -d --build ms-usuarios ms-catalogo ms-ingesta ms-cuotas ms-predicciones
# C
docker compose -f infra/docker-compose.distribuido.yml --env-file .env up -d --build ms-estadisticas ms-apuestas ms-cuotas ms-predicciones
```

Revisa el panel de Eureka en `http://<IP de A>:8761`: ahí `MS-CUOTAS` y `MS-PREDICCIONES` deben tener 2 instancias.
La web (`http://<IP de A>:8090`) muestra abajo, en el pie, cuántas instancias hay de cada servicio.

## La demostración

1. **Réplica:** en C, `docker compose -f infra/docker-compose.distribuido.yml stop ms-cuotas`. Refresca el análisis de un partido: las cuotas siguen saliendo (las atiende B). En el pie, `ms-cuotas` pasa de 2 a 1.
2. **Máquina entera:** apaga la máquina C. Todo lo que tenía réplica sigue; lo que solo estaba en C (`ms-estadisticas`, `ms-apuestas`) aparece como aviso amarillo en el pie, y el resto de la web sigue.
3. **Sin réplica:** en B, `stop ms-predicciones` y en C también. Aparece el aviso «ms-predicciones no está encendido: no se ven las probabilidades del modelo», y la página muestra las cuotas igual.
4. **Recuperación:** vuelve a hacer `up -d` y el pie vuelve a verde.

Eureka tarda unos 10 a 15 segundos en notar que una instancia cayó (el latido es cada 5 s y se da por caída a los 10 s). Mientras tanto,
el gateway reintenta los GET en otra instancia, así el usuario no ve el error.

## Lo que sigue siendo un punto único de falla

- `discovery-server`, `api-gateway`, `web-app` y `postgres` están solo en A. Si se apaga A, cae todo. Para una prueba más fuerte habría que replicarlos
  (varios gateways con un balanceador delante, Eureka en par y PostgreSQL con réplica), y eso queda fuera de esta versión.
- Los POST (por ejemplo registrar una apuesta) no se reintentan solos, porque repetirlos podría duplicar la apuesta.
