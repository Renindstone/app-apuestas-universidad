package pe.edu.unu.apuestas.cuotas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Cuotas de cada partido por casa, mercado y selección, con el margen de la casa y las probabilidades sin margen.
 * Las de bet365 y Pinnacle llegan desde ms-ingesta y las de Betano las ingresa el usuario a mano.
 * EnableFeignClients: consulta a ms-catalogo si existe el partido de una cuota manual.
 */
@SpringBootApplication
@EnableFeignClients
public class MsCuotasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsCuotasApplication.class, args);
    }
}
