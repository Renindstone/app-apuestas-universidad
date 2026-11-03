package pe.edu.unu.apuestas.catalogo.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import pe.edu.unu.apuestas.catalogo.entity.Liga;
import pe.edu.unu.apuestas.catalogo.repository.LigaRepository;

/**
 * Al arrancar, inserta las ligas del proyecto que todavía no estén en la tabla liga (RN-CAT-01).
 * Se revisa liga por liga para que una base ya creada reciba también la Serie A de Brasil.
 */
@Component
public class LigasIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LigasIniciales.class);

    // BRA es el código que usa ms-ingesta para la Serie A de Brasil (football-data no le da uno de tipo Div).
    static final List<Liga> LIGAS = List.of(
            new Liga("E0", "Premier League", "Inglaterra"),
            new Liga("SP1", "LaLiga", "España"),
            new Liga("D1", "Bundesliga", "Alemania"),
            new Liga("BRA", "Serie A de Brasil", "Brasil"));

    private final LigaRepository ligas;

    public LigasIniciales(LigaRepository ligas) {
        this.ligas = ligas;
    }

    @Override
    public void run(String... args) {
        List<Liga> faltan = LIGAS.stream()
                .filter(l -> ligas.findByCodigo(l.getCodigo()).isEmpty())
                .map(l -> new Liga(l.getCodigo(), l.getNombre(), l.getPais()))
                .toList();
        if (faltan.isEmpty()) {
            return;
        }
        ligas.saveAll(faltan);
        log.info("Ligas creadas: {}.", faltan.stream().map(Liga::getCodigo).toList());
    }
}
