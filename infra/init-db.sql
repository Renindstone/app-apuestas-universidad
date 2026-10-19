-- Se ejecuta solo la primera vez que se crea el volumen de PostgreSQL.
-- Una base de datos por microservicio (patrón Database per Service).
CREATE DATABASE usuarios_db;
CREATE DATABASE catalogo_db;
CREATE DATABASE ingesta_db;
CREATE DATABASE estadisticas_db;
CREATE DATABASE cuotas_db;
CREATE DATABASE apuestas_db;
