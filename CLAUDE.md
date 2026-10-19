# App Apuestas (rama universidad/microservicios): guía para Claude

Proyecto universitario de un equipo de 3 personas: 9 microservicios con Spring Boot y Spring Cloud. Hay que construirlo en una semana, del martes 20 al lunes 26 de octubre de 2026. Es una versión simplificada de la rama `main` (Python), que **no se toca desde esta rama**.

## Modo universidad: el equipo programa, Claude ayuda

- **No escribas el código de los servicios** (clases Java, controladores, entidades, tests) salvo que el usuario lo pida explícitamente para algo concreto. Por defecto, explica, da pistas, revisa el código del equipo, encuentra errores y propone cómo corregirlos.
- Cuando el usuario pida ayuda con código, empieza por lo mínimo: explica el concepto, señala el archivo y la línea, y da un fragmento corto si hace falta. Evita reescribir clases enteras.
- Si te piden revisar un PR o un servicio, contrástalo con su contrato en `servicios/<servicio>/README.md` y con la documentación.
- Puedes editar libremente la documentación (`docs/`, los README) y la infraestructura (`infra/`) si el usuario lo pide.

## Fuentes de verdad

- `docs/Documentacion.md`: requerimientos, arquitectura, BD, plan de la semana y pruebas.
- `servicios/<servicio>/README.md`: **contrato** de cada servicio (endpoints y JSON). Si el código y el contrato no coinciden, gana el contrato, salvo que el equipo decida cambiarlo.
- `README.md`: convenciones comunes (mercados, errores, fechas y paquetes).

## Convenciones

- Java 21, Maven y el paquete `pe.edu.unu.apuestas.<servicio>`. Código, comentarios y commits en español.
- Una base de datos por servicio: un servicio nunca accede a la base de datos de otro.
- Columnas con el prefijo de 3 letras de su tabla (convención de la cátedra).
- Fechas en UTC. La web las muestra en hora de Lima.
- Honestidad: el modelo de Poisson es didáctico y no le gana a las casas de apuestas. No lo presentes como si lo hiciera.
