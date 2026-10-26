package pe.edu.unu.apuestas.estadisticas.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unu.apuestas.estadisticas.dto.EstadisticasRequest;
import pe.edu.unu.apuestas.estadisticas.service.EstadisticasService;

/**
 * Rutas internas: solo las usa ms-ingesta dentro de la red de los servicios. El gateway las bloquea desde fuera.
 */
@RestController
@RequestMapping("/interno")
public class InternoController {

    private final EstadisticasService estadisticas;

    public InternoController(EstadisticasService estadisticas) {
        this.estadisticas = estadisticas;
    }

    // POST /interno/estadisticas: crea o actualiza las dos filas del partido y responde 200 sin cuerpo.
    @PostMapping("/estadisticas")
    public void guardar(@Valid @RequestBody EstadisticasRequest partido) {
        estadisticas.guardar(partido);
    }
}
