package pe.edu.unu.apuestas.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import pe.edu.unu.apuestas.web.client.ApuestasClient;
import pe.edu.unu.apuestas.web.dto.HistorialDTO;
import pe.edu.unu.apuestas.web.service.EjecutorParalelo;
import pe.edu.unu.apuestas.web.vista.Formatos;
import pe.edu.unu.apuestas.web.vista.GraficoBeneficio;

/**
 * /historial: cómo le fue a la apuesta del día en los últimos 30 días (RN-APU-08).
 */
@Controller
public class HistorialController {

    private static final int DIAS = 30;

    private final ApuestasClient apuestas;
    private final EjecutorParalelo paralelo;
    private final Formatos fmt;

    public HistorialController(ApuestasClient apuestas, EjecutorParalelo paralelo, Formatos fmt) {
        this.apuestas = apuestas;
        this.paralelo = paralelo;
        this.fmt = fmt;
    }

    // Resumen, gráfico de beneficio acumulado y lista de picks. Si ms-apuestas no responde, el historial es null y se muestra un aviso.
    @GetMapping("/historial")
    public String historial(Model model) {
        HistorialDTO historial = paralelo.intentar(() -> apuestas.historial(DIAS)).join();
        model.addAttribute("h", historial);
        model.addAttribute("grafico", historial == null ? null : GraficoBeneficio.desde(historial.ultimos(), fmt, 900, 220));
        model.addAttribute("dias", DIAS);
        return "historial";
    }
}
