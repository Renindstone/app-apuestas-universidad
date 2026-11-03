package pe.edu.unu.apuestas.ingesta.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unu.apuestas.ingesta.dto.EjecucionIniciadaDTO;
import pe.edu.unu.apuestas.ingesta.service.IngestaService;

/**
 * Rutas /interno/**: solo las usan otros servicios dentro de la red Docker. El gateway las bloquea.
 */
@RestController
@RequestMapping("/interno")
public class InternoController {

    private final IngestaService ingesta;

    public InternoController(IngestaService ingesta) {
        this.ingesta = ingesta;
    }

    // ms-apuestas pide los resultados nuevos para liquidar las apuestas de partidos que ya terminaron. 202 o 409 si ya hay una en curso.
    @PostMapping("/resultados")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EjecucionIniciadaDTO resultados() {
        return ingesta.actualizarResultados();
    }
}
