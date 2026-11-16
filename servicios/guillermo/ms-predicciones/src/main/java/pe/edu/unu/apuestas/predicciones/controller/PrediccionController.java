package pe.edu.unu.apuestas.predicciones.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unu.apuestas.predicciones.dto.PrediccionDTO;
import pe.edu.unu.apuestas.predicciones.service.PrediccionService;

/**
 * GET /api/predicciones/partidos/{id} (vía gateway). La usan la web y ms-apuestas.
 */
@RestController
public class PrediccionController {

    private final PrediccionService predicciones;

    public PrediccionController(PrediccionService predicciones) {
        this.predicciones = predicciones;
    }

    // Probabilidad y cuota justa de todos los mercados del partido, con la explicación de cada número.
    @GetMapping("/partidos/{partidoId}")
    public PrediccionDTO prediccion(@PathVariable Long partidoId) {
        return predicciones.predecir(partidoId);
    }
}
