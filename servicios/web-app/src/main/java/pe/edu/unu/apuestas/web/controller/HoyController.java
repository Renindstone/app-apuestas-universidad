package pe.edu.unu.apuestas.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pe.edu.unu.apuestas.web.service.HoyService;
import pe.edu.unu.apuestas.web.service.HoyService.VistaHoy;

/**
 * / (Hoy): la apuesta del día y los partidos del día, filtrables por liga.
 */
@Controller
public class HoyController {

    private final HoyService hoyService;

    public HoyController(HoyService hoyService) {
        this.hoyService = hoyService;
    }

    // ?liga=E0 filtra los partidos; sin parámetro se ven todas las ligas.
    @GetMapping("/")
    public String hoy(@RequestParam(required = false) String liga, Model model) {
        VistaHoy vista = hoyService.armar(liga);
        model.addAttribute("v", vista);
        model.addAttribute("liga", liga == null ? "" : liga);
        model.addAttribute("ligas", vista.ligas() != null ? vista.ligas() : HoyService.ligasPorDefecto());
        return "hoy";
    }
}
