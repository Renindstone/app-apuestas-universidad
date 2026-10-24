package pe.edu.unu.apuestas.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import pe.edu.unu.apuestas.web.client.ApuestasClient;
import pe.edu.unu.apuestas.web.dto.HistorialDTO;
import pe.edu.unu.apuestas.web.exception.ApiException;
import pe.edu.unu.apuestas.web.exception.ServicioNoDisponibleException;
import pe.edu.unu.apuestas.web.vista.Formatos;
import pe.edu.unu.apuestas.web.vista.GraficoBeneficio;

/**
 * /historial: cómo le fue a la apuesta del día en los últimos 30 días (RN-APU-08).
 */
@Controller
public class HistorialController {

    private static final int DIAS = 30;

    private final ApuestasClient apuestas;
    private final Formatos fmt;

    public HistorialController(ApuestasClient apuestas, Formatos fmt) {
        this.apuestas = apuestas;
        this.fmt = fmt;
    }

    // Resumen, gráfico de beneficio acumulado y lista de picks. Si ms-apuestas no responde, se muestra un aviso.
    @GetMapping("/historial")
    public String historial(Model model) {
        HistorialDTO historial;
        try {
            historial = apuestas.historial(DIAS);
        } catch (ServicioNoDisponibleException | ApiException e) {
            historial = null;
        }
        model.addAttribute("h", historial);
        model.addAttribute("grafico", historial == null ? null : GraficoBeneficio.desde(historial.ultimos(), fmt, 900, 220));
        model.addAttribute("dias", DIAS);
        return "historial";
    }
}
