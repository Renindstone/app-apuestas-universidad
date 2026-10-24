package pe.edu.unu.apuestas.catalogo.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import pe.edu.unu.apuestas.catalogo.entity.Liga;
import pe.edu.unu.apuestas.catalogo.repository.LigaRepository;

/**
 * Al arrancar, si la tabla liga está vacía, inserta las 3 ligas del proyecto (RN-CAT-01).
 */
@Component
public class LigasIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LigasIniciales.class);

    private final LigaRepository ligas;

    public LigasIniciales(LigaRepository ligas) {
        this.ligas = ligas;
    }

    @Override
    public void run(String... args) {
        if (ligas.count() > 0) {
            return;
        }
        ligas.saveAll(List.of(
                new Liga("E0", "Premier League", "Inglaterra"),
                new Liga("SP1", "LaLiga", "España"),
                new Liga("D1", "Bundesliga", "Alemania")));
        log.info("Ligas iniciales creadas: E0, SP1 y D1.");
    }
}
