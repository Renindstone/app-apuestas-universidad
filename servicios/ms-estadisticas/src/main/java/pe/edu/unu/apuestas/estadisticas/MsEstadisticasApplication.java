package pe.edu.unu.apuestas.estadisticas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Estadísticas de cada equipo en cada partido y sus promedios: de un equipo, de la liga y del árbitro.
 * Es la materia prima de ms-predicciones. Los datos los carga ms-ingesta por POST /interno/estadisticas.
 */
@SpringBootApplication
public class MsEstadisticasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsEstadisticasApplication.class, args);
    }
}
