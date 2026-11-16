package pe.edu.unu.apuestas.predicciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Modelo de Poisson: calcula la probabilidad y la cuota justa de cada mercado de un partido a partir de los promedios
 * de ms-estadisticas. No guarda nada: en cada petición pide los datos y vuelve a calcular (RN-PRE-01).
 * Es un modelo didáctico: no conoce alineaciones ni lesiones y no le gana a las casas de apuestas (RN-15).
 */
@SpringBootApplication
@EnableFeignClients
public class MsPrediccionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsPrediccionesApplication.class, args);
    }
}
