package pe.edu.unu.apuestas.apuestas.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unu.apuestas.apuestas.dto.ApuestaDelDiaDTO;
import pe.edu.unu.apuestas.apuestas.dto.EvaluacionDTO;
import pe.edu.unu.apuestas.apuestas.dto.EvaluacionRequest;
import pe.edu.unu.apuestas.apuestas.dto.HistorialDTO;
import pe.edu.unu.apuestas.apuestas.service.ApuestaDelDiaService;
import pe.edu.unu.apuestas.apuestas.service.ValorService;

/**
 * Evaluar una cuota, la apuesta del día y su historial (vía gateway: /api/apuestas + ruta, con token).
 */
@RestController
public class RecomendacionController {

    private final ValorService valor;
    private final ApuestaDelDiaService apuestaDelDia;

    public RecomendacionController(ValorService valor, ApuestaDelDiaService apuestaDelDia) {
        this.valor = valor;
        this.apuestaDelDia = apuestaDelDia;
    }

    // Probabilidad del modelo, valor esperado, stake sugerido y veredicto de la cuota que manda el usuario.
    @PostMapping("/evaluaciones")
    public EvaluacionDTO evaluar(@Valid @RequestBody EvaluacionRequest datos) {
        return valor.evaluacion(datos);
    }

    // ?fecha=2026-10-24 (por defecto, hoy en Lima) y ?recalcular=true para generarla de nuevo.
    @GetMapping("/apuesta-del-dia")
    public ApuestaDelDiaDTO apuestaDelDia(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "false") boolean recalcular) {
        return apuestaDelDia.apuestaDelDia(fecha, recalcular);
    }

    // ?dias=30: las apuestas del día liquidadas en ese periodo, con stake de 1 unidad.
    @GetMapping("/picks/historial")
    public HistorialDTO historial(@RequestParam(defaultValue = "30") int dias) {
        return apuestaDelDia.historial(dias);
    }
}
