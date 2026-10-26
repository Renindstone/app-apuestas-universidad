package pe.edu.unu.apuestas.catalogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Catálogo de ligas, equipos, árbitros y partidos. Es el dueño de los IDs: los demás servicios guardan
 * partidoId, equipoId, ligaId y arbitroId tal como los entrega este servicio (RN-11).
 */
@SpringBootApplication
public class MsCatalogoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsCatalogoApplication.class, args);
    }
}
