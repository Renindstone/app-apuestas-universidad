package pe.edu.unu.apuestas.apuestas.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unu.apuestas.apuestas.dto.LiquidacionDTO;
import pe.edu.unu.apuestas.apuestas.service.LiquidacionService;

/**
 * Rutas internas: solo se usan dentro de la red de los servicios. El gateway las bloquea desde fuera.
 */
@RestController
@RequestMapping("/interno")
public class InternoController {

    private final LiquidacionService liquidacion;

    public InternoController(LiquidacionService liquidacion) {
        this.liquidacion = liquidacion;
    }

    // POST /interno/liquidar: lo mismo que la tarea de las 06:00, a pedido.
    @PostMapping("/liquidar")
    public LiquidacionDTO liquidar() {
        return liquidacion.liquidarPendientes();
    }
}
