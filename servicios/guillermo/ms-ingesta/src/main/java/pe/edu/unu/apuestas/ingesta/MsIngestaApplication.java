package pe.edu.unu.apuestas.ingesta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ingesta de football-data.co.uk: descarga los CSV y reparte sus datos entre ms-catalogo, ms-estadisticas
 * y ms-cuotas. Es el único servicio que conoce el formato de football-data (capa anticorrupción).
 * EnableAsync: la ingesta corre en segundo plano. EnableScheduling: las ejecuciones de las 05:00, 08:00 y 16:00.
 */
@SpringBootApplication
@EnableFeignClients
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class MsIngestaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsIngestaApplication.class, args);
    }
}
