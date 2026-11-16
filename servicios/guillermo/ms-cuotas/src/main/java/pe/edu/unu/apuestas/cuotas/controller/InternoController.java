package pe.edu.unu.apuestas.cuotas.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unu.apuestas.cuotas.dto.CuotasRequest;
import pe.edu.unu.apuestas.cuotas.dto.GuardadasDTO;
import pe.edu.unu.apuestas.cuotas.service.CuotaService;

/**
 * Rutas internas: solo las usa ms-ingesta dentro de la red de los servicios. El gateway las bloquea desde fuera.
 */
@RestController
@RequestMapping("/interno")
public class InternoController {

    private final CuotaService cuotas;

    public InternoController(CuotaService cuotas) {
        this.cuotas = cuotas;
    }

    // POST /interno/cuotas: crea o actualiza las cuotas de una casa para un partido y responde {"guardadas": n}.
    @PostMapping("/cuotas")
    public GuardadasDTO guardar(@Valid @RequestBody CuotasRequest datos) {
        return cuotas.guardar(datos);
    }
}
