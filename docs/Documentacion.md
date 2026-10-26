# Documentación del proyecto: Analizador de Apuestas de Fútbol con Microservicios

> Universidad Nacional de Ucayali · Facultad de Ingeniería de Sistemas e Ingeniería Civil · Escuela Profesional de Ingeniería de Sistemas
>
> **Grupo:** _Integrante 1_ · _Integrante 2_ · _Integrante 3_
> **Asignatura:** _(completar)_ · **Docente:** _(completar)_
> **Lugar y fecha:** Pucallpa, Perú, _(fecha de entrega)_

Versión resumida, siguiendo la estructura de la documentación del proyecto MTC. Las secciones marcadas con _(completar)_ se llenan al terminar la semana.

---

## 1. Perfil del proyecto

### 1.1. Planteamiento de la problemática

Quien apuesta en fútbol desde el Perú (bet365, Betano) decide normalmente por intuición. La información que necesitaría está dispersa en varios sitios: resultados, estadísticas de cada equipo (tiros, córners, tarjetas), el árbitro designado y las cuotas de cada casa. Nadie la junta en un solo lugar ni la convierte en probabilidades. Por eso es difícil saber si una cuota paga más de lo que corresponde al riesgo real, es decir, si tiene *valor*.

### 1.2. Propósito y justificación

Construir una plataforma web que reúna esos datos, estime la probabilidad de cada mercado con un modelo estadístico sencillo y la compare con la cuota de la casa. Con eso indica qué apuestas tienen valor esperado positivo y propone una **apuesta del día**. El proyecto sirve además para aplicar la **arquitectura de microservicios**: servicios pequeños e independientes, con su propia base de datos, que se registran en un servidor de descubrimiento y se exponen por un único API Gateway.

### 1.3. Objetivos

**Objetivo general:** desarrollar una plataforma web basada en microservicios que integre resultados, estadísticas y cuotas de fútbol, calcule probabilidades por mercado y recomiende apuestas con valor esperado positivo.

**Objetivos específicos:**
- Integrar de forma automática y periódica los datos públicos de football-data.co.uk (resultados, estadísticas, árbitro y cuotas).
- Calcular, para cada partido, la probabilidad de los mercados 1X2, goles, ambos marcan, tarjetas y córners.
- Comparar esas probabilidades con las cuotas de las casas y proponer una apuesta del día.
- Permitir que cada usuario registre sus apuestas y vea su rendimiento (ROI).
- Desplegar los 9 microservicios con Docker Compose usando un solo comando.

### 1.4. Alcance

**Incluye:**
- Ligas: Premier League (E0), LaLiga (SP1) y Bundesliga (D1).
- Temporadas: la actual y la anterior.
- Apuestas: solo antes del partido.
- Mercados: 1X2, más/menos goles, ambos marcan, más/menos tarjetas (con factor árbitro) y más/menos córners.
- Cuotas: bet365 tomadas de football-data, y Pinnacle solo hasta enero de 2026, cuando football-data dejó de publicarlas; las de Betano las ingresa el usuario a mano.

**No incluye** (queda en la rama `main` o como trabajo futuro):
- Champions League (football-data no la publica).
- Estadísticas de jugadores, alineaciones y lesiones.
- Hándicap asiático y apuestas en vivo.
- Hacer apuestas reales: el sistema solo recomienda.

### 1.5. Supuestos y restricciones

**Supuestos:**
- football-data.co.uk sigue publicando sus CSV gratuitos, que se actualizan un par de veces por semana.
- Los integrantes tienen Java 21, Docker y Git.

**Restricciones:**
- Una semana de desarrollo, del **martes 20 al lunes 26 de octubre de 2026**, con 3 integrantes.
- Presupuesto cero: solo fuentes de datos gratuitas.
- El dato de árbitro solo existe para la Premier League. En LaLiga y Bundesliga el factor árbitro no se aplica.
- Es un proyecto académico. Un modelo de Poisson simple **no le gana a las casas de apuestas** de forma sostenida, y así debe presentarse.

### 1.6. Modelo de negocio

El proyecto es académico y no busca ingresos durante el curso. Aun así, conviene plantear cómo funcionaría si se llevara al mercado. Para eso se usa el lienzo de modelo de negocio (*Business Model Canvas*) de Osterwalder y Pigneur, que resume en nueve bloques a quién se le ofrece valor, cómo se le llega y de dónde salen los ingresos y los costos. Los precios y los planes son supuestos por validar, no resultados. En el Word, el lienzo tiene su forma clásica: la propuesta de valor va al centro.

| Bloque | Contenido |
|---|---|
| Segmentos de clientes | Apostadores recreativos mayores de 18 años en el Perú que usan bet365 o Betano · Aficionados que quieren aprender estadística aplicada al fútbol · Creadores de contenido que necesitan datos y probabilidades |
| Propuesta de valor | Resultados, estadísticas, árbitro y cuotas en un solo lugar · Probabilidad y cuota justa de cada mercado, explicadas · Detecta cuotas con valor esperado positivo y propone una apuesta del día con umbrales públicos · Registro de apuestas y ROI real · No promete ganancias: «No apostar también es una decisión» |
| Canales | Web responsive (PC y celular) · Redes sociales: análisis de cada jornada · Comunidades de fútbol |
| Relación con clientes | Autoservicio con cuenta propia · Historial público de la apuesta del día, con aciertos y fallos · Contenido educativo · Mensajes de juego responsable |
| Fuentes de ingresos (modelo freemium, por validar) | Plan gratuito: partidos, mercados 1X2 y goles, y registro de apuestas · Plan Premium mensual: todos los mercados, apuesta del día, historial completo y alertas · Publicidad de marcas que no sean de apuestas · Acceso a la API de datos para creadores de contenido |
| Recursos clave | Datos públicos de 3 ligas · Modelo de Poisson y código propio · Infraestructura con Docker · Equipo de 3 desarrolladores |
| Actividades clave | Ingesta diaria y control de los datos · Validar el modelo contra Pinnacle · Operar los 9 microservicios · Contenido educativo |
| Socios clave | football-data.co.uk (datos gratuitos) · Proveedor de nube o VPS · Pasarela de pagos local (p. ej., Culqi o Mercado Pago) · Creadores de contenido y comunidades de fútbol |
| Estructura de costos | Servidor para los 11 contenedores (durante el curso, S/ 0: corre en local) · Dominio web (el certificado TLS puede ser gratuito) · Horas de desarrollo y mantenimiento · Publicidad en redes y comisión de la pasarela de pagos · A futuro, datos de pago (alineaciones, xG) |

**Decisiones del modelo:**
- **Sin comisiones de afiliados.** Los programas de afiliados de las casas de apuestas suelen pagar una parte de lo que pierden los usuarios referidos, o un monto por cada nuevo depositante. Cobrar así crearía un conflicto de interés, porque la plataforma ganaría más cuando el usuario pierde. Por eso se descarta.
- **Se vende información y disciplina, no ganancias.** El modelo de Poisson no le gana a las casas de forma sostenida (sección 1.5). El plan Premium no puede anunciarse como una forma de ganar dinero: ofrece ahorro de tiempo, comprensión de las cuotas y control de la banca.
- **Marco legal y juego responsable.** En el Perú, las apuestas deportivas a distancia están reguladas por la Ley N.° 31557, modificada por la Ley N.° 31806, y por su reglamento (D.S. N.° 005-2023-MINCETUR), vigente desde febrero de 2024. La plataforma no recibe apuestas, solo las analiza. Aun así, debe dirigirse solo a mayores de edad, mostrar mensajes de juego responsable y comprobar que las casas que menciona tengan autorización vigente del MINCETUR.
- **Validación antes de invertir.** Un piloto gratuito durante una temporada mediría cuántos usuarios vuelven cada semana y cuántos pagarían por el plan Premium. Si no se alcanza un mínimo, el proyecto se queda como herramienta educativa.

---

## 2. Metodología

Se usa **Scrum** adaptado a una semana. Hay un solo sprint de 7 días con *Daily Scrum* de 15 minutos cada noche. En él se revisa qué se terminó, qué se hará al día siguiente y qué bloquea.

- **Sprint Planning** (martes 20, mañana): leer este documento, acordar los contratos de API (sección 5.4) y repartir las historias.
- **Ejecución** (martes 20 a domingo 25): cada integrante desarrolla sus 3 microservicios. Para no bloquearse entre sí, cada servicio responde **datos de prueba fijos (mocks)** desde el primer día, respetando el contrato, y luego los reemplaza por la lógica real. Esto aplica la lección de la retrospectiva del proyecto MTC: definir el formato JSON de los contratos al inicio.
- **Sprint Review** (lunes 26): demostración completa con `docker compose up`.
- **Retrospectiva** (lunes 26): qué funcionó y qué mejorar _(completar)_.

Valores de trabajo: autonomía, respeto, responsabilidad, foco y transparencia (avance visible en el tablero y en los commits).

---

## 3. Personas y roles

Cada integrante es **dueño de 3 microservicios** (1 de infraestructura o seguridad y 2 de negocio) y responde por su código, sus pruebas y su Dockerfile.

| Persona | Rol Scrum | Microservicios a cargo | Detalle |
|---|---|---|---|
| _Integrante 1_ | Equipo de desarrollo (Datos) | `ms-catalogo`, `ms-ingesta`, `ms-estadisticas` | Ligas, equipos, árbitros y partidos. Descarga de los CSV y cálculo de promedios. |
| _Integrante 2_ | Equipo de desarrollo (Modelo y valor) | `ms-cuotas`, `ms-predicciones`, `ms-apuestas` | Cuotas y margen. Modelo de Poisson, valor esperado, apuesta del día y seguimiento de apuestas. |
| _Integrante 3_ | Scrum Master y equipo de desarrollo (Plataforma) | `discovery-server`, `api-gateway`, `ms-usuarios` y la `web-app` | Infraestructura (Eureka, Gateway y seguridad JWT), cuentas de usuario, interfaz web y `docker-compose` final. |

Los roles de Product Owner (prioriza el backlog) y QA (revisa las pruebas de los demás) se rotan entre los tres. Cada uno prueba los servicios de otro compañero.

---

## 4. Análisis

### 4.1. Historia de usuario central

> Como apostador, quiero ver los partidos de la fecha con la probabilidad que estima el sistema para cada mercado y compararla con la cuota de mi casa de apuestas, para saber qué apuestas tienen valor y cuál es la apuesta del día.

**Criterios de aceptación:**
- El sistema muestra los partidos programados de las 3 ligas, filtrables por fecha y liga.
- Para cada partido muestra la probabilidad y la cuota justa de cada mercado, y explica de dónde salen (promedios de cada equipo y del árbitro).
- Al ingresar una cuota, el sistema calcula el valor esperado y el stake sugerido.
- La apuesta del día cumple los umbrales de la sección 4.3 (RF-12). Si ninguna los cumple, el sistema dice "Hoy no hay apuesta del día".

### 4.2. Actores y casos de uso

| Actor | Caso de uso |
|---|---|
| Visitante | CU-01 Crear cuenta |
| Apostador | CU-02 Iniciar sesión |
| Apostador | CU-03 Consultar partidos por fecha y liga |
| Apostador | CU-04 Analizar un partido (estadísticas, árbitro y probabilidades) |
| Apostador | CU-05 Evaluar una cuota (valor esperado) |
| Apostador | CU-06 Consultar la apuesta del día |
| Apostador | CU-07 Registrar una apuesta y ver su rendimiento |
| Apostador | CU-08 Registrar cuotas de Betano a mano |
| Administrador | CU-09 Lanzar la ingesta manualmente y ver su historial |
| Sistema | CU-10 Ingesta programada y liquidación automática |

**Fichas de los casos de uso principales:**

| Código | CU-04 |
|---|---|
| Actor | Apostador |
| Caso de uso | Analizar un partido |
| Descripción | El apostador elige un partido y ve los promedios de ambos equipos (como local o como visitante), el perfil del árbitro y la probabilidad de cada mercado. |
| Precondición | La ingesta se ejecutó al menos una vez y el partido existe en `ms-catalogo`. |
| Flujo principal | 1) El apostador elige el partido. 2) `web-app` llama a `GET /api/predicciones/partidos/{id}` a través del gateway. 3) `ms-predicciones` consulta a `ms-catalogo` (equipos y árbitro) y a `ms-estadisticas` (promedios). 4) Calcula las probabilidades con Poisson y las devuelve. 5) `web-app` también pide las cuotas a `ms-cuotas` y muestra todo junto. |
| Postcondición | El apostador ve probabilidades, cuotas justas y cuotas de mercado del partido. |

| Código | CU-06 |
|---|---|
| Actor | Apostador |
| Caso de uso | Consultar la apuesta del día |
| Descripción | El sistema evalúa todas las combinaciones partido–mercado–cuota del día y elige la de mayor valor esperado entre las que cumplen los umbrales. |
| Precondición | Hay partidos programados ese día con cuotas cargadas. |
| Flujo principal | 1) `ms-apuestas` pide a `ms-catalogo` los partidos del día. 2) Por cada partido pide la predicción a `ms-predicciones` y las cuotas a `ms-cuotas`. 3) Calcula el valor esperado de cada selección. 4) Filtra por los umbrales y elige la mejor. 5) La guarda como *pick* y la devuelve. |
| Postcondición | Queda registrada la apuesta del día, o el mensaje de que hoy no la hay. |

| Código | CU-10 |
|---|---|
| Actor | Sistema (tarea programada) |
| Caso de uso | Ingesta programada y liquidación |
| Descripción | Cada día `ms-ingesta` descarga los CSV y reparte los datos entre los servicios. Luego `ms-apuestas` liquida los picks y las apuestas de partidos ya jugados. |
| Precondición | football-data.co.uk está disponible. Si no lo está, la ejecución queda registrada con estado ERROR y se reintenta al día siguiente. |
| Flujo principal | 1) A las 05:00 (hora de Lima) `ms-ingesta` descarga los CSV de la temporada. 2) Por cada fila, registra el partido en `ms-catalogo`, las estadísticas en `ms-estadisticas` y las cuotas en `ms-cuotas`. 3) Guarda el registro de la ejecución. 4) A las 06:00 `ms-apuestas` liquida lo pendiente con el resultado y las estadísticas finales. |
| Postcondición | Los datos y los resultados de las apuestas quedan al día. |

### 4.3. Requerimientos

**Funcionales:**

| ID | Prioridad | Requerimiento | Microservicio |
|---|---|---|---|
| RF-01 | Alta | Registrar una cuenta (usuario, correo, contraseña con BCrypt). El rol APOSTADOR lo asigna el servidor. | ms-usuarios |
| RF-02 | Alta | Iniciar sesión y recibir un token JWT con el rol (ADMIN o APOSTADOR). | ms-usuarios |
| RF-03 | Alta | Descargar automáticamente los resultados, estadísticas, árbitros y cuotas de cada temporada desde football-data. | ms-ingesta |
| RF-04 | Alta | Descargar los próximos partidos con sus cuotas (`fixtures.csv`). | ms-ingesta |
| RF-05 | Alta | Consultar ligas, equipos y partidos por fecha, liga y estado. | ms-catalogo |
| RF-06 | Alta | Calcular los promedios a favor y en contra de un equipo (goles, tiros, tiros a puerta, córners, faltas, tarjetas), como local o como visitante y en sus últimos N partidos. | ms-estadisticas |
| RF-07 | Media | Mostrar el perfil del árbitro: tarjetas y faltas por partido. | ms-estadisticas |
| RF-08 | Alta | Calcular la probabilidad y la cuota justa de cada mercado de un partido. | ms-predicciones |
| RF-09 | Alta | Consultar las cuotas de un partido y su margen, con las probabilidades sin margen. | ms-cuotas |
| RF-10 | Media | Registrar a mano cuotas de Betano para un partido. | ms-cuotas |
| RF-11 | Alta | Evaluar una cuota: valor esperado y stake sugerido (Kelly fraccional). | ms-apuestas |
| RF-12 | Alta | Generar la apuesta del día con cuota entre 1.50 y 2.60, probabilidad ≥ 42 % y valor esperado ≥ 3 %. | ms-apuestas |
| RF-13 | Alta | Registrar mis apuestas (partido, mercado, selección, casa, cuota y monto). | ms-apuestas |
| RF-14 | Media | Liquidar automáticamente los picks y las apuestas de partidos terminados. | ms-apuestas |
| RF-15 | Media | Mostrar mi rendimiento (ganadas, perdidas, beneficio, ROI) y el historial de la apuesta del día. | ms-apuestas |
| RF-16 | Baja | Registrar cada ejecución de la ingesta (inicio, fin, filas, estado, error). Solo la ve el ADMIN. | ms-ingesta |

**No funcionales:**

| ID | Requerimiento |
|---|---|
| RNF-01 Arquitectura | Microservicios independientes que se registran en Eureka. El único punto de entrada es el API Gateway. |
| RNF-02 Datos | Una base de datos por microservicio. Ningún servicio lee la base de datos de otro: todo se pide por REST. |
| RNF-03 Seguridad | Autenticación JWT validada en el gateway y autorización por rol. Las rutas `/interno/**` no se exponen fuera de la red de Docker. |
| RNF-04 Resiliencia | Si un servicio cae, los demás responden con un error controlado (timeout y mensaje claro), nunca con un 500 genérico. |
| RNF-05 Despliegue | Todo el sistema se levanta con `docker compose up --build`. |
| RNF-06 Rendimiento | El análisis de un partido responde en menos de 2 segundos. |
| RNF-07 Documentación | Cada servicio publica su API con OpenAPI (Swagger UI). |
| RNF-08 Honestidad | El sistema muestra probabilidades y valor esperado, nunca "apuestas seguras". |

### 4.4. Historias de usuario y Sprint Backlog

Son 9 historias, 3 por integrante:

| ID | Historia | Puntos | Prioridad | Responsable |
|---|---|---|---|---|
| HU-01 | Como sistema, quiero descargar los CSV de football-data y repartir sus datos entre los servicios. | 8 | Alta | Integrante 1 |
| HU-02 | Como apostador, quiero consultar los partidos por fecha y liga. | 5 | Alta | Integrante 1 |
| HU-03 | Como apostador, quiero ver los promedios de cada equipo y el perfil del árbitro. | 8 | Alta | Integrante 1 |
| HU-04 | Como apostador, quiero ver las cuotas de un partido sin el margen de la casa y poder añadir las de Betano. | 5 | Alta | Integrante 2 |
| HU-05 | Como apostador, quiero la probabilidad de cada mercado de un partido. | 13 | Alta | Integrante 2 |
| HU-06 | Como apostador, quiero la apuesta del día y registrar mis apuestas con su rendimiento. | 13 | Alta | Integrante 2 |
| HU-07 | Como equipo, quiero un registro de servicios y un gateway único con seguridad JWT. | 8 | Alta | Integrante 3 |
| HU-08 | Como visitante, quiero crear una cuenta e iniciar sesión. | 5 | Alta | Integrante 3 |
| HU-09 | Como apostador, quiero una interfaz web para usar todo lo anterior. | 8 | Alta | Integrante 3 |

### 4.5. Reglas de negocio

Las reglas de negocio dicen qué debe cumplir el sistema, sin importar cómo se programe. Hay dos grupos. Las **generales** valen para todo el sistema. Las **específicas** las aplica un microservicio concreto. La columna *Relación* indica el requerimiento, el caso de uso, la prueba o el servicio con que se conecta cada regla. RN-01 a RN-08 son las mismas que muestra la presentación.

Cada `servicios/<integrante>/<servicio>/README.md` repite sus reglas específicas con el mismo código. Si una regla cambia, se actualizan los dos documentos y se avisa en el daily, igual que con los contratos.

#### 4.5.1. Reglas generales

| ID | Regla | Relación |
|---|---|---|
| RN-01 Margen de la casa | Cada cuota esconde una probabilidad implícita: `q = 1 / cuota`. Las probabilidades implícitas de todas las selecciones de un mercado suman más de 1, y ese exceso es el margen de la casa: `margen = Σq − 1`. Por ejemplo, las cuotas 2.10, 3.40 y 3.60 dan un margen de 4.8 %. | RF-09 · ms-cuotas |
| RN-02 Probabilidad sin margen | La probabilidad que el mercado asigna a cada selección se obtiene quitando el margen con el método multiplicativo: `p = q / Σq`. Así, las probabilidades vuelven a sumar 1. | RF-09 · ms-cuotas |
| RN-03 Valor esperado | `EV = p × cuota − 1`, donde `p` es la probabilidad del modelo. Es lo que se gana o se pierde en promedio por cada unidad apostada, si `p` es correcta. | RF-11 · CB-05 · ms-apuestas |
| RN-04 Veredicto | Una cuota es CON_VALOR si su EV es de 3 % o más, MARGINAL si su EV está entre 0 y 3 %, y SIN_VALOR si su EV es 0 o negativo. | RF-11 · CB-06 · ms-apuestas |
| RN-05 Stake sugerido | Se apuesta un cuarto de lo que indica el criterio de Kelly, con un tope de 3 % de la banca: `stake = min(0.25 × EV / (cuota − 1), 0.03)`. Si el EV es 0 o negativo, el stake es 0. | RF-11 · CB-05 · ms-apuestas |
| RN-06 Apuesta del día | Son candidatas las cuotas de bet365 o Betano entre 1.50 y 2.60, con probabilidad de 42 % o más, EV de 3 % o más y al menos 5 partidos de muestra por equipo. La probabilidad mezcla al 50 % el modelo y el mercado sin margen de la misma casa (RN-02), para que una racha corta no infle el valor. Gana la de mayor EV, y hay como mucho una por día. Si ninguna cumple, el sistema dice «Hoy no hay apuesta del día». | RF-12 · CU-06 · CN-07 · ms-apuestas |
| RN-07 Liquidación | Una apuesta ganada deja `monto × (cuota − 1)` de beneficio y una perdida resta el monto. Todas las líneas terminan en .5, así que nunca hay empate (*push*). Solo se liquidan partidos FINALIZADO. | RF-14 · CB-07 · ms-apuestas |
| RN-08 Límites del sistema | El sistema solo recomienda. No se conecta a ninguna casa de apuestas, no apuesta por el usuario y no maneja dinero real. El usuario apuesta por su cuenta y, si quiere, registra la apuesta para medir su rendimiento. Solo se analizan mercados previos al partido, sin apuestas en vivo. El factor árbitro solo existe en la Premier League. | Secciones 1.4 y 1.5 |
| RN-09 Mercados válidos | Solo existen los mercados, selecciones y líneas de la tabla del README raíz: 1X2, GOLES (1.5, 2.5 y 3.5), AMBOS_MARCAN, TARJETAS (3.5, 4.5 y 5.5) y CORNERS (8.5, 9.5 y 10.5). Cualquier otra combinación se rechaza con 400. Las tarjetas suman amarillas y rojas de ambos equipos. | README raíz · todos los servicios |
| RN-10 Ligas, temporadas y casas | Se trabaja con la Premier League (E0), LaLiga (SP1) y la Bundesliga (D1). Se cargan la temporada actual y la anterior. La anterior solo sirve de historia para los promedios, porque al empezar una temporada cada equipo tiene pocos partidos: las recomendaciones siempre son sobre partidos por jugar. Las casas son bet365 y Pinnacle, que vienen de football-data, y Betano, que se ingresa a mano. football-data dejó de publicar las cuotas de Pinnacle en enero de 2026: solo las tienen los partidos anteriores, y de la temporada actual solo llegan las de bet365. | Sección 1.4 · RF-03 |
| RN-11 Origen de los datos | Los partidos, las estadísticas y las cuotas solo entran por `ms-ingesta`, desde football-data.co.uk. La única excepción son las cuotas de Betano, que ingresa el usuario. `ms-catalogo` es el dueño de los IDs de liga, equipo, árbitro y partido. | RF-03, RF-04 y RF-10 · Sección 5.2 |
| RN-12 Fechas | Todas las fechas se guardan en UTC. «Un día» siempre es un día de Lima, de 05:00 a 05:00 UTC. La web muestra las horas en hora de Lima, y las tareas programadas usan la zona America/Lima. | README raíz |
| RN-13 Cuentas y roles | Hay dos roles. APOSTADOR es el que el servidor asigna a toda cuenta nueva. ADMIN corresponde a un único usuario que se crea al arrancar. Solo el ADMIN lanza la ingesta y ve su historial. | RF-01, RF-02 y RF-16 · CN-03 |
| RN-14 Privacidad de las apuestas | Cada usuario ve solo sus propias apuestas y su rendimiento. El usuario siempre se identifica por el token (la cabecera `X-Usuario-Id` que pone el gateway), nunca por un dato del JSON. | RF-13 y RF-15 · RNF-03 |
| RN-15 Honestidad | El sistema muestra probabilidades, valor esperado y de dónde sale cada número. Nunca habla de «apuestas seguras» ni promete ganancias: el modelo de Poisson es didáctico y no le gana a las casas de forma sostenida. | RNF-08 · Sección 1.5 |
| RN-16 Juego responsable | La plataforma se dirige solo a mayores de 18 años y muestra mensajes de juego responsable. Cuando no hay apuesta del día, lo dice con claridad: «No apostar también es una decisión». Solo menciona casas con autorización vigente del MINCETUR. | Sección 1.6 |

#### 4.5.2. Reglas específicas de cada microservicio

Las reglas RN-01 y RN-02 las implementa `ms-cuotas`, y las RN-03 a RN-07, `ms-apuestas`. Por eso no se repiten aquí. `discovery-server` no tiene reglas de negocio propias, porque solo registra y localiza servicios.

**api-gateway** (Integrante 3)

| ID | Regla | Relación |
|---|---|---|
| RN-GAT-01 | Solo son públicas (sin token) estas rutas: `POST /api/usuarios/auth/**`, los `GET` de `/api/catalogo/**`, `/actuator/health` y Swagger. Todo lo demás exige un token bien firmado y vigente. Si falta, responde 401. | RNF-03 · CB-09 · CN-02 |
| RN-GAT-02 | `/api/ingesta/**` exige además el rol ADMIN. Si no lo tiene, responde 403. | RN-13 · CN-03 |
| RN-GAT-03 | Ninguna ruta `/interno/**` se atiende desde fuera: responde 403 sin reenviar la petición. | RNF-03 · CN-04 |
| RN-GAT-04 | Las cabeceras `X-Usuario-Id` y `X-Usuario-Rol` que mande el cliente se borran y se reemplazan por las del token, para que nadie pueda suplantar a otro usuario. | RN-14 |
| RN-GAT-05 | Solo la web (`http://localhost:8090`) puede llamar a la API desde un navegador (CORS). | RNF-03 |

**ms-usuarios** (Integrante 3)

| ID | Regla | Relación |
|---|---|---|
| RN-USU-01 | El nombre de usuario (hasta 30 caracteres) y el correo son únicos. El correo se guarda en minúsculas, así que «Renzo@Correo.pe» y «renzo@correo.pe» cuentan como el mismo. Un duplicado responde 409 USUARIO_DUPLICADO y no se guarda nada. | RF-01 · CB-08 |
| RN-USU-02 | La clave tiene de 6 a 72 caracteres y debe coincidir con su confirmación. Solo se guarda su hash BCrypt, nunca la clave. | RF-01 |
| RN-USU-03 | Toda cuenta nueva recibe el rol APOSTADOR, aunque el JSON traiga otro. | RN-13 |
| RN-USU-04 | Si el inicio de sesión falla, el mensaje es el mismo cuando el usuario no existe y cuando la clave es incorrecta. Así no se revela qué cuentas existen. | RF-02 |
| RN-USU-05 | El token dura 8 horas. Cuando vence, hay que iniciar sesión otra vez. | RF-02 · CB-09 |
| RN-USU-06 | La banca es un monto que declara el usuario, mayor o igual a 0 (por defecto, 0). El sistema no mueve dinero real. | RN-08 |
| RN-USU-07 | Al arrancar, si no existe, se crea el usuario `admin`. Su clave sale de la variable `ADMIN_PASSWORD` y nunca se escribe en el código. | RN-13 |

**ms-catalogo** (Integrante 1)

| ID | Regla | Relación |
|---|---|---|
| RN-CAT-01 | Solo existen tres ligas: E0, SP1 y D1. Se insertan al arrancar si la tabla está vacía. | RN-10 |
| RN-CAT-02 | Un cruce local–visitante ocurre una sola vez por temporada. Por eso el alta de partidos es un *upsert*: si el cruce ya existe, se actualiza. Enviar la misma fila dos veces no duplica nada. | RF-05 |
| RN-CAT-03 | Los equipos y los árbitros se crean solos la primera vez que llegan en una fila. Sus nombres son únicos. | RF-05 |
| RN-CAT-04 | Un partido nace PROGRAMADO y pasa a FINALIZADO cuando llegan sus goles. Un partido FINALIZADO nunca vuelve a PROGRAMADO, aunque después llegue una fila sin goles. | RF-05 · RF-14 |
| RN-CAT-05 | El árbitro queda vacío cuando no se conoce: siempre en LaLiga y la Bundesliga, y en los partidos futuros. | RN-08 |
| RN-CAT-06 | La fecha de los filtros es un día de Lima: va de las 05:00 UTC de ese día a las 05:00 UTC del día siguiente. | RN-12 |

**ms-ingesta** (Integrante 1)

| ID | Regla | Relación |
|---|---|---|
| RN-ING-01 | Es el único servicio que lee football-data. Descarga cada archivo como mucho una vez por ejecución, nunca en un bucle. | RN-11 · RF-03 |
| RN-ING-02 | Se ejecuta sola en hora de Lima: HISTORICO a las 05:00 (solo la temporada actual) y PROXIMOS a las 08:00 y a las 16:00. Solo el ADMIN puede lanzarla a mano. | RF-03 y RF-04 · CU-09 y CU-10 |
| RN-ING-03 | La hora del CSV es la de Londres y se convierte a UTC, de modo que el cambio de horario británico se aplica solo. | RN-12 · CB-01 |
| RN-ING-04 | Cada fila se procesa en este orden: primero el catálogo, que devuelve los IDs; luego las estadísticas, solo si el partido ya se jugó; y por último las cuotas, solo si la fila las trae. | RF-03 · CB-02 |
| RN-ING-05 | Una fila con error se anota y no detiene la ejecución. Si la fuente no responde, la ejecución termina en ERROR y se reintenta en la siguiente hora programada. | CU-10 · RNF-04 |
| RN-ING-06 | Cada ejecución queda registrada con su tipo, fuente, inicio, fin, filas, estado y error. | RF-16 · CN-05 |

**ms-estadisticas** (Integrante 1)

| ID | Regla | Relación |
|---|---|---|
| RN-EST-01 | Cada partido se guarda como dos filas, una por equipo, y en cada una el rival es el otro equipo. Si el mismo partido llega otra vez, se actualizan esas filas. | RF-06 |
| RN-EST-02 | Los promedios *a favor* salen de las filas del equipo. Los promedios *en contra* salen de las filas de su rival en esos mismos partidos. | RF-06 |
| RN-EST-03 | Se toman los últimos N partidos (10 por defecto), del más reciente al más antiguo y sin importar la temporada. Se pueden filtrar como local, como visitante o todos. | RF-06 · RN-10 |
| RN-EST-04 | Si un equipo no tiene partidos, la respuesta indica 0 partidos y deja los promedios vacíos (`null`), nunca en cero: no tener datos no es lo mismo que no hacer goles. | RF-06 |
| RN-EST-05 | El perfil del árbitro suma las tarjetas de ambos equipos en cada partido que dirigió y lo compara con la media de su liga. | RF-07 |
| RN-EST-06 | Un partido que aún no se jugó no tiene estadísticas: la consulta responde 404. | RF-14 |

**ms-cuotas** (Integrante 2)

| ID | Regla | Relación |
|---|---|---|
| RN-CUO-01 | Toda cuota es mayor que 1, y las que ingresa el usuario deben ser mayores que 1.01. | RF-10 · CB-04 |
| RN-CUO-02 | Las cuotas de bet365 y Pinnacle solo llegan por la ingesta. Las de Betano solo se ingresan a mano y se guardan con tipo MANUAL y con el ID del usuario que las cargó. | RN-10 · RF-10 |
| RN-CUO-03 | Una cuota manual solo se acepta si el partido existe en `ms-catalogo` (si no, 404) y si el mercado, la selección y la línea son válidos (si no, 400). | RF-10 · RN-09 |
| RN-CUO-04 | La consulta muestra la cuota más reciente de cada casa, mercado, selección y línea. Si la misma cuota se guarda dos veces, se actualiza en lugar de duplicarse. | RF-09 |
| RN-CUO-05 | El margen y las probabilidades sin margen (RN-01 y RN-02) solo se calculan con el mercado completo. Si falta una selección, la respuesta es 409 MERCADO_INCOMPLETO. | RF-09 |

**ms-predicciones** (Integrante 2)

| ID | Regla | Relación |
|---|---|---|
| RN-PRE-01 | No guarda nada: en cada petición pide los datos y vuelve a calcular. | RF-08 |
| RN-PRE-02 | El valor esperado de cada estadística combina la media de la liga con el ataque de un equipo y la defensa del otro. El local se mide jugando en casa y el visitante jugando fuera, en sus últimos 10 partidos. | RF-08 |
| RN-PRE-03 | Si un equipo tiene pocos partidos, cada factor se acerca a la media de la liga: `factor' = (n × factor + 5) / (n + 5)`. | RF-08 |
| RN-PRE-04 | Los mercados de goles, 1X2 y ambos marcan salen de una matriz de Poisson de 0 a 10 goles por equipo. Los de córners y tarjetas usan Poisson sobre el total del partido. | RF-08 · CB-03 |
| RN-PRE-05 | Las tarjetas se ajustan con el factor árbitro. Ese factor se acerca a 1 cuando el árbitro tiene pocos partidos (con un peso de 10), y vale 1 si el partido no tiene árbitro. | RF-07 · RN-08 |
| RN-PRE-06 | La cuota justa de una selección es `1 / probabilidad`. La respuesta trae todos los mercados y líneas de RN-09, con una explicación de dónde sale cada número. | RF-08 · RN-15 |
| RN-PRE-07 | Si `ms-catalogo` o `ms-estadisticas` no responden, devuelve 503: nunca inventa una probabilidad. | RNF-04 |

**ms-apuestas** (Integrante 2)

| ID | Regla | Relación |
|---|---|---|
| RN-APU-01 | La probabilidad de una evaluación siempre sale de `ms-predicciones`, y el usuario solo manda la cuota. Por eso evaluar una cuota funciona aunque `ms-cuotas` no responda. | RF-11 · RN-03 |
| RN-APU-02 | La apuesta del día se genera sola a las 09:00 de Lima. Si ya existe para esa fecha, se devuelve la guardada, y solo se recalcula cuando se pide con `recalcular=true`. | RF-12 · CU-06 |
| RN-APU-03 | Todas las candidatas se guardan como *pick*, pero solo la de mayor EV se marca como apuesta del día. Pinnacle no se usa para elegirla, porque el equipo apuesta en bet365 y Betano. | RF-12 · RN-06 |
| RN-APU-04 | Sin cuotas no hay apuesta del día: si `ms-cuotas` no responde, devuelve 503. | RNF-04 |
| RN-APU-05 | Al registrar una apuesta, se guarda también el nombre del partido («Arsenal vs Chelsea»), para mostrar el historial sin consultar a `ms-catalogo`. El monto debe ser mayor que 0 y la cuota, mayor que 1. | RF-13 · RN-09 |
| RN-APU-06 | La liquidación corre a las 06:00 de Lima. Cada mercado gana o pierde según la tabla del contrato; por ejemplo, GOLES MAS gana si el total de goles supera la línea. Tarjetas y córners necesitan las estadísticas finales, y si todavía no están, la apuesta queda pendiente hasta el día siguiente. | RF-14 · CU-10 · CB-07 |
| RN-APU-07 | El rendimiento solo cuenta las apuestas liquidadas del propio usuario: `ROI = beneficio / monto apostado`, en total y por mercado. | RF-15 · CN-09 · RN-14 |
| RN-APU-08 | El historial de la apuesta del día solo cuenta los picks liquidados, con un stake fijo de 1 unidad. Así mide la calidad de las recomendaciones y no depende de la banca de cada usuario. | RF-15 |

**web-app** (Integrante 3)

| ID | Regla | Relación |
|---|---|---|
| RN-WEB-01 | Solo habla con el `api-gateway`, nunca directamente con un microservicio. | RNF-01 |
| RN-WEB-02 | El token se guarda en la sesión del servidor, no en el navegador. Si el gateway responde 401, se borra la sesión y se vuelve a `/login`. | RNF-03 |
| RN-WEB-03 | Todas las horas se muestran en hora de Lima. | RN-12 |
| RN-WEB-04 | Si una sección falla, la página se muestra igual, con un aviso como «Cuotas no disponibles por ahora», y nunca con una pantalla de error. | RNF-04 · CN-08 |
| RN-WEB-05 | Una cuota se pinta de verde cuando supera a la cuota justa, es decir, cuando su valor esperado es positivo. | RN-03 |
| RN-WEB-06 | La pantalla de ingesta solo aparece para el ADMIN. | RN-13 |
| RN-WEB-07 | El pie de cada página dice: «Solo para mayores de 18 años. Juega con responsabilidad.» | RN-16 |

---

## 5. Arquitectura de software

### 5.1. Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot + Spring Cloud (Initializr elige versiones compatibles entre sí) |
| Registro y descubrimiento | Spring Cloud Netflix Eureka Server |
| API Gateway | Spring Cloud Gateway |
| Comunicación entre servicios | REST síncrono con OpenFeign y balanceo por Eureka (`lb://`) |
| Seguridad | JWT (librería `jjwt`): lo firma `ms-usuarios` y lo valida el gateway. Contraseñas con BCrypt. |
| Persistencia | Spring Data JPA + PostgreSQL 16, con una base de datos por servicio dentro de un mismo contenedor |
| Tareas programadas | Spring `@Scheduled` (zona America/Lima) |
| Lectura de CSV | Apache Commons CSV |
| Documentación de API | springdoc-openapi (Swagger UI) |
| Interfaz web | Spring Boot + Thymeleaf + Bootstrap 5 (`web-app`) |
| Pruebas | JUnit 5 + Mockito |
| Contenedores | Docker y Docker Compose |
| Control de versiones | Git y GitHub, rama `universidad/microservicios` |

### 5.2. Arquitectura lógica

Los 9 microservicios se dividen en **2 de infraestructura** y **7 de negocio**. La `web-app` es un cliente y no se cuenta entre los microservicios.

```mermaid
flowchart TB
    U[Navegador] --> W[web-app :8090]
    W --> G[api-gateway :8080]
    FD[(football-data.co.uk)] -. CSV .-> IN
    subgraph NEG [Microservicios de negocio]
        US[ms-usuarios :8081]
        IN[ms-ingesta :8083]
        CA[ms-catalogo :8082]
        ES[ms-estadisticas :8084]
        CU[ms-cuotas :8085]
        PR[ms-predicciones :8086]
        AP[ms-apuestas :8087]
        IN --> CA
        IN --> ES
        IN --> CU
        PR --> CA
        PR --> ES
        AP --> CA
        AP --> PR
        AP --> CU
        AP --> ES
    end
    G --> NEG
    NEG -. se registran .-> D[discovery-server :8761]
    G -. se registra .-> D
```

| # | Microservicio | Tipo | Responsabilidad | Base de datos | Dueño |
|---|---|---|---|---|---|
| 1 | `discovery-server` | Infraestructura | Registro y descubrimiento (Eureka) | — | Integrante 3 |
| 2 | `api-gateway` | Infraestructura | Punto de entrada único, rutas, validación JWT y CORS | — | Integrante 3 |
| 3 | `ms-usuarios` | Negocio | Registro, login, roles y banca del usuario | `usuarios_db` | Integrante 3 |
| 4 | `ms-catalogo` | Negocio | Ligas, equipos, árbitros y partidos (fuente de los IDs) | `catalogo_db` | Integrante 1 |
| 5 | `ms-ingesta` | Negocio | Descarga los CSV, los reparte entre servicios y guarda el historial de ejecuciones | `ingesta_db` | Integrante 1 |
| 6 | `ms-estadisticas` | Negocio | Estadísticas por partido y equipo, promedios y perfil de árbitro | `estadisticas_db` | Integrante 1 |
| 7 | `ms-cuotas` | Negocio | Cuotas por casa y mercado, margen y probabilidades sin margen | `cuotas_db` | Integrante 2 |
| 8 | `ms-predicciones` | Negocio | Modelo de Poisson: probabilidades por mercado. No guarda estado. | — | Integrante 2 |
| 9 | `ms-apuestas` | Negocio | Valor esperado, apuesta del día, apuestas del usuario, liquidación y ROI | `apuestas_db` | Integrante 2 |

**Patrones aplicados:**
- *API Gateway*.
- *Service Registry / Discovery*.
- *Database per Service*.
- *Composición de API*: `ms-predicciones` y `ms-apuestas` arman su respuesta pidiendo datos a otros servicios.
- *Anti-corruption layer*: solo `ms-ingesta` conoce el formato de football-data; los demás reciben JSON limpio.

**Identidad entre servicios:** `ms-catalogo` es dueño de los IDs de partido, equipo, liga y árbitro. Los demás servicios guardan esos IDs como números sin llave foránea, porque viven en otra base de datos.

### 5.3. Flujo de la apuesta del día

```mermaid
sequenceDiagram
    participant W as web-app
    participant G as api-gateway
    participant A as ms-apuestas
    participant C as ms-catalogo
    participant P as ms-predicciones
    participant Q as ms-cuotas
    W->>G: GET /api/apuestas/<br/>apuesta-del-dia (JWT)
    G->>A: GET /apuesta-del-dia<br/>(X-Usuario-Id)
    A->>C: GET /partidos?fecha=hoy<br/>&estado=PROGRAMADO
    loop cada partido
        A->>P: GET /partidos/{id}
        A->>Q: GET /partidos/{id}/cuotas
    end
    A->>A: EV = p × cuota − 1<br/>filtrar umbrales y elegir el máximo
    A-->>G: pick o "Hoy no hay<br/>apuesta del día"
    G-->>W: 200 OK
```

### 5.4. Contratos de API

El contrato completo de cada servicio (endpoints, ejemplos JSON y errores) está en el `README.md` de su carpeta, dentro de `servicios/`. **Ese README es el contrato:** si alguien lo cambia, avisa al equipo en el daily. Resumen:

| Servicio | Endpoints públicos (vía gateway: `/api/<servicio>` + ruta, p. ej. `/api/catalogo/partidos`) | Endpoints internos (solo red Docker) |
|---|---|---|
| ms-usuarios | `POST /auth/registro`, `POST /auth/login`, `GET /me`, `PUT /me/banca` | — |
| ms-catalogo | `GET /ligas`, `GET /equipos`, `GET /partidos`, `GET /partidos/{id}` | `POST /interno/partidos` |
| ms-ingesta | `POST /ejecutar` (ADMIN), `GET /ejecuciones` (ADMIN) | — |
| ms-estadisticas | `GET /equipos/{id}/promedios`, `GET /ligas/{id}/promedios`, `GET /arbitros/{id}/perfil`, `GET /partidos/{id}/estadisticas` | `POST /interno/estadisticas` |
| ms-cuotas | `GET /partidos/{id}/cuotas`, `GET /partidos/{id}/probabilidades-mercado`, `POST /cuotas/manual` | `POST /interno/cuotas` |
| ms-predicciones | `GET /partidos/{id}` | — |
| ms-apuestas | `POST /evaluaciones`, `GET /apuesta-del-dia`, `GET /picks/historial`, `POST /mis-apuestas`, `GET /mis-apuestas`, `GET /mis-apuestas/rendimiento` | `POST /interno/liquidar` |

### 5.5. Seguridad

1. `ms-usuarios` firma el JWT (HS256) con la clave `JWT_SECRET`. El token incluye `sub` (id del usuario), `usuario` y `rol`, y dura 8 horas.
2. Un filtro global del `api-gateway` valida el token. Luego **borra** las cabeceras `X-Usuario-*` que vengan del cliente y añade `X-Usuario-Id` y `X-Usuario-Rol` con los datos del token.
3. **Rutas públicas:** `/api/usuarios/auth/**` y los `GET` de `/api/catalogo/**`. Todo lo demás exige token, y `/api/ingesta/**` exige además el rol ADMIN.
4. El gateway rechaza cualquier ruta `/interno/**`, que solo es accesible dentro de la red de Docker.
5. En `docker-compose` solo se publican los puertos del gateway (8080) y de la web (8090). Los demás servicios no son accesibles desde fuera.

### 5.6. Arquitectura física

```mermaid
flowchart TB
    subgraph Host
      subgraph red-apuestas [red Docker: apuestas-net]
        D[discovery-server]
        G[api-gateway]
        S[7 microservicios de negocio]
        PG[(postgres:16<br/>6 bases de datos)]
        W[web-app]
      end
    end
    Navegador -->|:8090| W
    Navegador -->|:8080 Swagger/API| G
    S --> PG
    S -. HTTPS saliente .-> FD[(football-data.co.uk)]
```

| Contenedor | Imagen | Puerto interno | Puerto publicado |
|---|---|---|---|
| postgres | postgres:16 | 5432 | 5441 (solo para desarrollo) |
| discovery-server | build propio | 8761 | 8761 (panel Eureka) |
| api-gateway | build propio | 8080 | 8080 |
| ms-usuarios … ms-apuestas | build propio | 8081 … 8087 | — |
| web-app | build propio | 8090 | 8090 |

Los puertos de PostgreSQL (5441) y de la web (8090) se eligieron para no chocar con el proyecto MTC (5440 y 8082) ni con la rama `main` (5433).

### 5.7. Diagramas de flujo de los microservicios

Los diagramas de esta sección muestran, paso a paso, qué hace cada microservicio y en qué momento llama a otro. Complementan la arquitectura lógica (5.2) y la secuencia de la apuesta del día (5.3). Siguen los contratos de `servicios/<integrante>/<servicio>/README.md`: si un contrato cambia, también hay que actualizar su diagrama. Las fuentes Graphviz están en [`docs/diagramas/flujo/`](diagramas/flujo/). Para regenerar una imagen: `dot -Tpng -Gdpi=220 archivo.dot -o archivo.png`.

**Simbología** (con los colores de la arquitectura lógica):

| Símbolo | Significado |
|---|---|
| Rectángulo redondeado oscuro | Inicio: una petición HTTP, una tarea programada (`@Scheduled`) o el arranque del servicio |
| Rectángulo redondeado gris o rojo | Fin o respuesta: gris si es correcta, rojo si es un error, con su código HTTP |
| Rectángulo | Proceso del propio servicio. Azul: microservicio de negocio. Verde: infraestructura. Amarillo: `web-app` |
| Rectángulo con barras laterales | Proceso predefinido: llamada REST (Feign) a otro microservicio, cuyo nombre va en negrita |
| Rombo | Decisión, con salidas Sí/No o varias opciones |
| Cilindro | Base de datos del servicio o fuente externa |
| Círculo con letra | Conector: el flujo sigue en el otro círculo con la misma letra |
| Hoja con esquina doblada | Nota aclaratoria |
| Flecha continua / discontinua | Orden de los pasos / lectura o escritura de datos |

Cuando un servicio tiene varios endpoints, cada recuadro punteado agrupa un flujo independiente.

#### 5.7.1. Flujo general del sistema

Lo que hace el sistema sin intervención del usuario: el arranque con Docker Compose y las tres tareas programadas del día, en hora de Lima.

![Diagrama de flujo general: arranque y tareas programadas](diagramas/flujo/g1_tareas.png)

El recorrido del usuario desde que abre la web hasta que cierra la sesión:

![Diagrama de flujo general: uso del sistema por el apostador](diagramas/flujo/g2_usuario.png)

#### 5.7.2. Flujo de cada microservicio

Hay un diagrama por servicio, en el orden de la tabla de la sección 5.2. Cuando un servicio tiene varios endpoints, se muestran sus flujos principales. Se omiten los GET de solo lectura más simples.

**discovery-server** (Integrante 3). Eureka solo espera eventos: registros, latidos y búsquedas por nombre. Si un servicio deja de enviar latidos, sale del registro y el gateway deja de mandarle peticiones. Los 30 y 90 segundos son los valores por defecto de Eureka.

![Diagrama de flujo de discovery-server](diagramas/flujo/s1_discovery.png)

**api-gateway** (Integrante 3). Es el filtro `JwtAuthFilter` con sus reglas en el orden en que deben evaluarse. Lo primero es borrar las cabeceras `X-Usuario-*` que mande el cliente, para que nadie pueda hacerse pasar por otro usuario.

![Diagrama de flujo de api-gateway](diagramas/flujo/s2_gateway.png)

**ms-usuarios** (Integrante 3). Registro, inicio de sesión, banca y creación del usuario admin al arrancar. El rol siempre lo asigna el servidor, y el login responde el mismo error si falla el usuario o la clave.

![Diagrama de flujo de ms-usuarios](diagramas/flujo/s3_usuarios.png)

**ms-catalogo** (Integrante 1). El upsert interno es idempotente: enviar dos veces la misma fila no duplica el partido. La consulta convierte el día de Lima en un rango UTC antes de filtrar.

![Diagrama de flujo de ms-catalogo](diagramas/flujo/s4_catalogo.png)

**ms-ingesta** (Integrante 1). Cada ejecución recorre los archivos y, dentro de cada uno, las filas. Una fila con error se anota en el log y no detiene la ejecución. Los errores de `ms-estadisticas` y `ms-cuotas` se tratan igual.

![Diagrama de flujo de ms-ingesta](diagramas/flujo/s5_ingesta.png)

**ms-estadisticas** (Integrante 1). Cada partido se guarda como dos filas, una por equipo. Así los promedios a favor y en contra salen con un `AVG`, sin joins (sección 6.4).

![Diagrama de flujo de ms-estadisticas](diagramas/flujo/s6_estadisticas.png)

**ms-cuotas** (Integrante 2). La cuota manual de Betano se valida contra `ms-catalogo` antes de guardarse. Las probabilidades sin margen usan el método multiplicativo.

![Diagrama de flujo de ms-cuotas](diagramas/flujo/s7_cuotas.png)

**ms-predicciones** (Integrante 2). No tiene base de datos: en cada petición pide los datos y calcula. Si el partido no tiene árbitro (LaLiga y Bundesliga), el factor árbitro vale 1.

![Diagrama de flujo de ms-predicciones](diagramas/flujo/s8_predicciones.png)

**ms-apuestas** (Integrante 2). Es el servicio con más flujos, así que se divide en tres diagramas. El primero es la apuesta del día: si `ms-cuotas` no responde, devuelve 503, porque sin cuotas no hay valor esperado que calcular.

![Diagrama de flujo de ms-apuestas: apuesta del día](diagramas/flujo/s9a_apuestas_dia.png)

La liquidación recorre los picks y las apuestas pendientes. Los mercados de tarjetas y córners necesitan además las estadísticas finales del partido. Si todavía no están, la apuesta sigue pendiente hasta el día siguiente.

![Diagrama de flujo de ms-apuestas: liquidación](diagramas/flujo/s9b_apuestas_liq.png)

Los endpoints que usa el apostador desde la web: evaluar una cuota, registrar una apuesta y ver su rendimiento. El rendimiento solo cuenta las apuestas del usuario del token.

![Diagrama de flujo de ms-apuestas: evaluar, registrar y rendimiento](diagramas/flujo/s9c_apuestas_otros.png)

---

## 6. Diseño de base de datos

Hay una base de datos por servicio. Las columnas llevan el prefijo de 3 letras de su tabla, como pide la cátedra. Los campos `...IdPartido`, `...IdEquipo`, `...IdArbitro` y `...IdUsuario` que apuntan a datos de **otro servicio** no tienen llave foránea física (ver 5.2).

```mermaid
erDiagram
    LIGA ||--o{ EQUIPO : agrupa
    LIGA ||--o{ PARTIDO : contiene
    EQUIPO ||--o{ PARTIDO : "juega de local"
    EQUIPO ||--o{ PARTIDO : "juega de visitante"
    ARBITRO |o--o{ PARTIDO : dirige
```

### 6.1. `usuarios_db`

**usuario**

| Campo | Tipo | Nulo | Descripción |
|---|---|---|---|
| UsuIdUsuario | INTEGER IDENTITY PK | No | Identificador |
| UsuNombreUsuario | VARCHAR(30) UNIQUE | No | Nombre de usuario |
| UsuEmail | VARCHAR(100) UNIQUE | No | Correo, en minúsculas |
| UsuClaveHash | VARCHAR(100) | No | Contraseña cifrada con BCrypt |
| UsuRol | VARCHAR(15) | No | ADMIN o APOSTADOR (CHECK) |
| UsuBanca | NUMERIC(12,2) | No | Banca declarada, por defecto 0 |
| UsuFechaRegistro | TIMESTAMP | No | Por defecto NOW() |

### 6.2. `catalogo_db`

| Tabla | Campos principales | Restricciones |
|---|---|---|
| liga | LigIdLiga PK, LigCodigo (E0, SP1, D1), LigNombre, LigPais | UNIQUE(LigCodigo) |
| equipo | EquIdEquipo PK, EquNombre, EquIdLiga FK | UNIQUE(EquNombre) |
| arbitro | ArbIdArbitro PK, ArbNombre | UNIQUE(ArbNombre) |
| partido | ParIdPartido PK, ParIdLiga FK, ParTemporada ('2627'), ParFechaHora TIMESTAMPTZ (UTC), ParIdEquipoLocal FK, ParIdEquipoVisitante FK, ParIdArbitro FK (nulo), ParGolesLocal (nulo), ParGolesVisitante (nulo), ParEstado (PROGRAMADO o FINALIZADO) | UNIQUE(ParIdEquipoLocal, ParIdEquipoVisitante, ParTemporada): cada cruce ocurre una vez por temporada |

### 6.3. `ingesta_db`

| Tabla | Campos |
|---|---|
| ejecucion_ingesta | EjeIdEjecucion PK, EjeTipo (HISTORICO o PROXIMOS), EjeFuente (URL), EjeInicio, EjeFin, EjeEstado (EN_CURSO, OK o ERROR), EjeFilas, EjeError (TEXT) |

### 6.4. `estadisticas_db`

| Tabla | Campos | Restricciones |
|---|---|---|
| estadistica_partido | EpaIdEstadistica PK, EpaIdPartido, EpaIdLiga, EpaTemporada, EpaFecha, EpaIdEquipo, EpaIdRival, EpaCondicion (LOCAL o VISITANTE), EpaIdArbitro (nulo), EpaGoles, EpaTiros, EpaTirosPuerta, EpaCorners, EpaFaltas, EpaAmarillas, EpaRojas | UNIQUE(EpaIdPartido, EpaIdEquipo). Índices en EpaIdEquipo, EpaIdRival y EpaIdArbitro. |

Hay **una fila por equipo y partido**. Los promedios *a favor* de un equipo son las filas con `EpaIdEquipo = X`, y los promedios *en contra* son las filas con `EpaIdRival = X`. Así basta un `AVG` sin joins.

### 6.5. `cuotas_db`

| Tabla | Campos | Restricciones |
|---|---|---|
| cuota | CuoIdCuota PK, CuoIdPartido, CuoCasa (bet365, pinnacle o betano), CuoMercado, CuoSeleccion, CuoLinea NUMERIC(4,2) (nulo), CuoValor NUMERIC(6,2), CuoTipo (APERTURA, CIERRE o MANUAL), CuoIdUsuario (nulo), CuoCapturadaEn | CHECK(CuoValor > 1). Índice en CuoIdPartido. |

### 6.6. `apuestas_db`

| Tabla | Campos |
|---|---|
| pick | PicIdPick PK, PicFecha, PicIdPartido, PicDescripcion ("Arsenal vs Chelsea"), PicMercado, PicSeleccion, PicLinea, PicCasa, PicCuota, PicProbabilidad, PicValorEsperado, PicStakePct, PicEsApuestaDelDia, PicEstado (ABIERTO o LIQUIDADO), PicResultado (GANADA o PERDIDA, nulo) |
| apuesta_usuario | ApuIdApuesta PK, ApuIdUsuario, ApuIdPartido, ApuDescripcion, ApuMercado, ApuSeleccion, ApuLinea, ApuCasa, ApuCuota, ApuMonto, ApuFecha, ApuResultado (nulo), ApuGanancia (nulo) |

`PicDescripcion` y `ApuDescripcion` copian el nombre del partido a propósito. Así el historial se muestra sin llamar a `ms-catalogo` por cada fila: es una desnormalización aceptada en microservicios.

---

## 7. Desarrollo: plan de la semana

**Regla de oro:** desde el día 1, cada endpoint responde con datos fijos (mock) que respetan el contrato. Así nadie espera a nadie. La lógica real reemplaza al mock a lo largo de la semana.

| Día | Integrante 1 (Datos) | Integrante 2 (Modelo y valor) | Integrante 3 (Plataforma) |
|---|---|---|---|
| **Mar 20** | Crear `ms-catalogo`, `ms-ingesta` y `ms-estadisticas` con Initializr, con `/actuator/health` y registro en Eureka. Entidades JPA del catálogo. | Crear `ms-cuotas`, `ms-predicciones` y `ms-apuestas`. Entidad `cuota` y mock de `GET /partidos/{id}`. | `discovery-server` funcionando. `api-gateway` con rutas a los 7 servicios. Levantar `infra/docker-compose.yml` (PostgreSQL). |
| **Mié 21** | Endpoints de `ms-catalogo` (incluido el upsert interno). Lectura del CSV en `ms-ingesta` (Commons CSV, fechas dd/MM/yyyy, hora de Londres a UTC). | `ms-cuotas` completo: guardar, consultar y probabilidades sin margen. | `ms-usuarios`: registro y login con BCrypt y JWT. |
| **Jue 22** | `ms-ingesta` llama por Feign a catálogo, estadísticas y cuotas. Entidad e inserción en `ms-estadisticas`. | `ms-predicciones`: promedios a fuerzas, matriz de Poisson, 1X2, goles y ambos marcan. | Filtro JWT global en el gateway y reglas por rol. `web-app`: login y registro. |
| **Vie 23** | `ms-estadisticas`: promedios de equipo (local, visitante, últimos N), de liga y perfil de árbitro. | `ms-predicciones`: tarjetas con factor árbitro y córners. `ms-apuestas`: `POST /evaluaciones`. | `web-app`: lista de partidos y pantalla de análisis. |
| **Sáb 24** | `@Scheduled` de la ingesta histórica y de próximos partidos, más el historial de ejecuciones. Pruebas unitarias. | `ms-apuestas`: apuesta del día, mis apuestas, liquidación y rendimiento. Pruebas unitarias. | `web-app`: apuesta del día, mis apuestas, rendimiento y carga de cuotas de Betano. |
| **Dom 25** | Dockerfile de sus 3 servicios. Prueba de extremo a extremo de la ingesta. | Dockerfile de sus 3 servicios. Prueba de extremo a extremo de la apuesta del día. | Dockerfiles y `docker-compose.yml` completo con los 11 contenedores (PostgreSQL, los 9 servicios y la web). Integración general. |
| **Lun 26** | Matrices de prueba (sección 8), completar la documentación y ensayar la demo. | Igual que el Integrante 1. | Igual que el Integrante 1. |

**Definición de terminado:** cada historia está terminada cuando su endpoint responde por el gateway, tiene al menos 2 pruebas unitarias, aparece en Swagger y está en la rama `universidad/microservicios`.

### 7.1. Retrospectiva

**Qué funcionó**

- **Los contratos primero.** Cada servicio tenía su `README.md` con endpoints, JSON y reglas antes de escribir código. Así cada servicio se construyó y probó por separado, y al unirlos en el `docker-compose.yml` no hubo que cambiar ningún contrato.
- **Un orden de construcción según las dependencias:** gateway, usuarios, catálogo, ingesta, estadísticas, cuotas, predicciones y apuestas. Cada servicio encontraba listos a los que llama por Feign, así que no hicieron falta los mocks de la «regla de oro».
- **Pruebas en dos niveles.** Hay 96 pruebas unitarias y de integración en los 10 módulos, todas en verde, y las 9 pruebas de caja negra pasaron contra el sistema completo (sección 8).
- **La resiliencia se cumplió:** si `ms-cuotas` cae, la web sigue mostrando las probabilidades con un aviso (CN-08), y la evaluación manual no depende de las cuotas (RN-APU-01).

**Qué se retrasó y por qué**

- **La implementación empezó el sábado 24 y no el martes 20.** Del lunes 19 al martes 20 se hicieron el replanteamiento, la documentación, los diagramas de flujo y la presentación. Los servicios y la web se programaron el sábado 24 y el domingo 25, por falta de tiempo del equipo entre semana. Para recuperar el retraso, el código se escribió con ayuda de un asistente de IA (Claude), servicio por servicio, y el equipo revisó cada entrega antes de pasar a la siguiente.
- **Docker estuvo disponible recién el domingo 25 por la noche.** Hasta entonces, las pruebas de integración usaron un PostgreSQL temporal y los jar levantados a mano. Con Docker listo, el sistema completo se probó de punta a punta el mismo día.
- **Cambios de diseño sobre la marcha:**
  - Pinnacle dejó de publicar cuotas en enero de 2026. Por eso la apuesta del día compara contra el mercado sin margen de la misma casa (RN-06).
  - Usado solo, el modelo daba apuestas del día con EV de 20–30 %, que no es valor real. Se mezcló al 50 % con el mercado (RN-15).
  - Se quitaron los CSV de muestra: la ingesta ahora descarga los datos de football-data.co.uk.

**Lecciones**

- El usuario `admin` solo se crea si no existe (RN-USU-07). Si `ADMIN_PASSWORD` cambia en el `.env` después del primer arranque, el admin conserva la clave anterior. En ese caso hay que borrar el volumen (`down -v`) o cambiar la clave en la base de datos.
- Con 10 JVM, el sistema necesita unos 5 GB de memoria. El tope de 384 MB por servicio (`JAVA_OPCIONES`) fue necesario para que todo cupiera en una laptop.

---

## 8. Plan de pruebas

Cada integrante escribe las pruebas de caja blanca de sus servicios y ejecuta las de caja negra de **otro** compañero.

### 8.1. Caja blanca (cobertura de caminos)

| ID | Método | Camino | Entrada | Salida esperada | Resultado (25/10/2026) |
|---|---|---|---|---|---|
| CB-01 | `CsvFootballDataParser.parsear()` | A: fila completa | Fila E0 con fecha 24/10/2026 15:00 (hora de Londres) | `PartidoCsv` con hora UTC 14:00 y estadísticas llenas | Pasa (`CsvFootballDataParserTest`) |
| CB-02 | `CsvFootballDataParser.parsear()` | B: partido sin jugar (fixtures) | Fila sin FTHG/FTAG | Goles nulos y estado PROGRAMADO | Pasa (`CsvFootballDataParserTest` y `ProcesadorIngestaTest`) |
| CB-03 | `PoissonService.matriz()` | A: λ válidos | λL = 1.5, λV = 1.1 | Las probabilidades de 1X2 suman 1 (± 0.001) | Pasa (`PoissonServiceTest`) |
| CB-04 | `CuotaService.quitarMargen()` | B: cuota no válida | Cuotas [2.0, 0.9, 3.5] | `CuotaInvalidaException` | Pasa (`CuotaServiceTest`) |
| CB-05 | `ValorService.evaluar()` | A: valor positivo | p = 0.50, cuota = 2.20 | Valor esperado 0.10 y stake ≤ 3 % | Pasa (`ValorServiceTest`) |
| CB-06 | `ValorService.evaluar()` | B: sin valor | p = 0.40, cuota = 2.20 | Valor esperado −0.12, stake 0 y veredicto SIN_VALOR | Pasa (`ValorServiceTest`) |
| CB-07 | `LiquidacionService.liquidar()` | A: más de 2.5 goles | Resultado 2-1, selección MAS, línea 2.5 | GANADA, ganancia = monto × (cuota − 1) | Pasa (`LiquidacionServiceTest`) |
| CB-08 | `UsuarioService.registrar()` | B: correo duplicado | Correo ya existente | `UsuarioDuplicadoException`, sin guardar nada | Pasa (`UsuarioServiceTest` y `FlujoUsuarioTest`) |
| CB-09 | `JwtAuthFilter` (gateway) | B: token vencido | JWT con `exp` en el pasado | 401 y la petición no llega al servicio | Pasa (`JwtAuthFilterTest`) |

Corrida del 25/10/2026: `./mvnw test` en los 10 módulos, con 96 pruebas, 0 fallos y 0 errores.

### 8.2. Caja negra

Se ejecutaron el 25/10/2026 contra el `docker-compose.yml` completo, por el gateway (:8080) y la web (:8090).

| ID | Funcionalidad | Pasos | Resultado esperado | Resultado obtenido |
|---|---|---|---|---|
| CN-01 | Registro e inicio de sesión | Registrarse y luego iniciar sesión | Recibe un token y entra al panel | Pasa: registro 201 con rol APOSTADOR, login 200 con token y la web redirige al panel |
| CN-02 | Acceso sin token | Llamar a `GET /api/apuestas/mis-apuestas` sin token | 401 | Pasa: 401 |
| CN-03 | Rol insuficiente | Un APOSTADOR llama a `POST /api/ingesta/ejecutar` | 403 | Pasa: 403 |
| CN-04 | Ruta interna bloqueada | `POST /api/catalogo/interno/partidos` desde fuera | 403 o 404, sin datos creados | Pasa: 403 con token y sin él; el catálogo siguió con 1308 partidos |
| CN-05 | Ingesta manual | El ADMIN ejecuta la ingesta HISTORICO | La ejecución termina en OK con más de 0 filas y aparecen partidos en el catálogo | Pasa: 6 archivos (3 ligas × 2 temporadas) en OK, 1279 filas en 24 s y 1308 partidos en el catálogo |
| CN-06 | Análisis de partido | Abrir un partido de la Premier | Muestra promedios, árbitro y probabilidades en menos de 2 s | Pasa: Ipswich vs Nott'm Forest con árbitro, goles esperados, matriz de marcadores y promedios, en 0.07 s |
| CN-07 | Apuesta del día sin candidatos | Fecha sin partidos | Mensaje "Hoy no hay apuesta del día" | Pasa: con la fecha 15/07/2026 responde "Hoy no hay apuesta del día: ninguna apuesta supera los umbrales…" |
| CN-08 | Servicio caído | Detener `ms-cuotas` y abrir un análisis | Se ven las probabilidades y el aviso "cuotas no disponibles", sin error 500 | Pasa: la página responde 200 con las probabilidades y el aviso "Cuotas no disponibles por ahora" |
| CN-09 | Rendimiento | Registrar 3 apuestas de partidos ya jugados y liquidar | Ganadas, perdidas y ROI correctos | Pasa: 3 apuestas a LOCAL con cuota 2.00 y 10 de monto (1-1, 0-1 y 3-1) dan 1 ganada, 2 perdidas, beneficio −10 y ROI −33.3 % |

---

## 9. Conclusiones y recomendaciones

### 9.1. Conclusiones

Se cumplieron los cinco objetivos específicos de la sección 1.3:

- **Integración de datos.** `ms-ingesta` descarga los CSV de football-data.co.uk tres veces al día (05:00, 08:00 y 16:00, hora de Lima) y reparte sus datos entre los servicios. En la prueba CN-05 cargó 1279 filas de 3 ligas y 2 temporadas en 24 s.
- **Probabilidades por mercado.** `ms-predicciones` calcula con Poisson los mercados 1X2, goles, ambos marcan, tarjetas (con factor árbitro) y córners. En CN-06 respondió en 0.07 s.
- **Apuesta del día.** `ms-apuestas` compara esas probabilidades con las cuotas y propone como mucho una apuesta al día. Si ninguna supera los umbrales, lo dice (CN-07). Usado solo, el modelo daba valores esperados de 20–30 %, así que se mezcla al 50 % con el mercado (RN-06). Es un modelo didáctico que no le gana a las casas de forma sostenida, y el sistema solo recomienda (RN-15).
- **Seguimiento de apuestas.** Cada usuario registra sus apuestas, que se liquidan solas cada día a las 06:00, y ve sus ganadas, perdidas y ROI (CN-09).
- **Despliegue.** Un solo `docker compose up` levanta los 9 microservicios, la web y PostgreSQL con sus 6 bases de datos. Las 96 pruebas automáticas y las 9 de caja negra pasaron (sección 8).

**Qué aportó separar el sistema en microservicios**, frente al monolito del proyecto MTC:

- Cada servicio tuvo un dueño, un contrato, una base de datos y pruebas propias. Por eso se pudo construir y probar cada uno por separado, y al unirlos no hubo que cambiar ningún contrato (7.1).
- Una falla queda aislada: si `ms-cuotas` cae, el análisis sigue mostrando las probabilidades con un aviso (CN-08).
- Si el contrato no cambia, un cambio en un servicio solo obliga a recompilar y reiniciar ese servicio.

**Qué costó:**

- Más configuración: el registro de servicios (Eureka), el gateway con la validación JWT, las llamadas Feign entre servicios y 6 bases de datos, en lugar de una aplicación con una base.
- Más llamadas por la red: un análisis de partido pasa por el gateway y por varios servicios. Cada llamada puede fallar, y hubo que decidir qué hacer en cada caso (5.7).
- Más recursos: 10 JVM necesitan unos 5 GB de memoria, y en una laptop hubo que limitar la memoria y la CPU de cada servicio.
- Más difícil de depurar: un error puede atravesar el gateway y dos o tres servicios, cada uno con su propio log.

Para un sistema de este tamaño, un monolito habría sido más simple de construir y desplegar. La separación se justifica por el objetivo del curso: aplicar la arquitectura de microservicios con Spring Boot y Spring Cloud.

### 9.2. Trabajo futuro

- **Mensajería asíncrona** (RabbitMQ): `ms-ingesta` publicaría un evento `partido.finalizado` y `ms-apuestas` liquidaría al recibirlo, en lugar de cada día a una hora fija.
- **Circuit breaker** (Resilience4j) en las llamadas Feign, y Config Server centralizado.
- **Más datos**: alineaciones, lesiones, estadísticas de jugadores, Champions League y hándicap asiático. La rama `main` del repositorio ya los implementa en Python.
- **Modelo**: ponderar los partidos recientes, usar goles esperados (xG) y mezclar la probabilidad del modelo con la del mercado.
- **Despliegue**: Kubernetes y una integración continua que ejecute las pruebas en cada push.
