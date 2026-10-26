package pe.edu.unu.apuestas.ingesta.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unu.apuestas.ingesta.dto.EjecucionDTO;
import pe.edu.unu.apuestas.ingesta.dto.EjecucionIniciadaDTO;
import pe.edu.unu.apuestas.ingesta.entity.TipoIngesta;
import pe.edu.unu.apuestas.ingesta.service.IngestaService;

/**
 * POST /api/ingesta/ejecutar y GET /api/ingesta/ejecuciones. Solo el ADMIN: lo controla el gateway (RN-GAT-02).
 */
@RestController
public class IngestaController {

    private final IngestaService ingesta;

    public IngestaController(IngestaService ingesta) {
        this.ingesta = ingesta;
    }

    // ?tipo=HISTORICO (las 2 temporadas) o PROXIMOS. Responde 202 enseguida y la ingesta sigue en segundo plano.
    @PostMapping("/ejecutar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EjecucionIniciadaDTO ejecutar(@RequestParam TipoIngesta tipo) {
        return ingesta.lanzar(tipo, false);
    }

    // ?limite=20: las últimas ejecuciones, de la más reciente a la más antigua.
    @GetMapping("/ejecuciones")
    public List<EjecucionDTO> ejecuciones(@RequestParam(defaultValue = "20") int limite) {
        return ingesta.ejecuciones(limite);
    }
}
