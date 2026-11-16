package pe.edu.unu.apuestas.apuestas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Convierte probabilidades y cuotas en decisiones: valor esperado, apuesta del día, apuestas de cada usuario,
 * liquidación y rendimiento. Solo recomienda: no se conecta a ninguna casa ni maneja dinero real (RN-08).
 * EnableScheduling: la apuesta del día a las 09:00 y la liquidación a las 06:00 de Lima.
 */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class MsApuestasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsApuestasApplication.class, args);
    }
}
