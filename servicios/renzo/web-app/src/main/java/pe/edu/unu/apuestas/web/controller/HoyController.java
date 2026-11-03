package pe.edu.unu.apuestas.web.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pe.edu.unu.apuestas.web.service.HoyService;
import pe.edu.unu.apuestas.web.service.HoyService.VistaHoy;

/**
 * / (Hoy): la apuesta del día y los partidos de un día, filtrables por liga y por franja horaria de Lima.
 */
@Controller
public class HoyController {

    private final HoyService hoyService;

    public HoyController(HoyService hoyService) {
        this.hoyService = hoyService;
    }

    // Ejemplo: /?liga=E0&desde=04:30&hasta=08:00. Sin parámetros se ven todas las ligas, todo el día de hoy.
    // Un valor mal escrito en la URL se ignora, como si no estuviera (RN-WEB-04: nunca una pantalla de error).
    @GetMapping("/")
    public String hoy(@RequestParam(required = false) String liga, @RequestParam(required = false) String fecha,
            @RequestParam(required = false) String desde, @RequestParam(required = false) String hasta, Model model) {
        LocalTime horaDesde = hora(desde);
        LocalTime horaHasta = hora(hasta);
        VistaHoy vista = hoyService.armar(liga, dia(fecha), horaDesde, horaHasta);
        model.addAttribute("v", vista);
        model.addAttribute("liga", liga == null ? "" : liga);
        model.addAttribute("desde", horaDesde == null ? "" : horaDesde.toString());
        model.addAttribute("hasta", horaHasta == null ? "" : horaHasta.toString());
        model.addAttribute("ligas", vista.ligas() != null ? vista.ligas() : HoyService.ligasPorDefecto());
        return "hoy";
    }

    private static LocalTime hora(String texto) {
        try {
            return texto == null || texto.isBlank() ? null : LocalTime.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static LocalDate dia(String texto) {
        try {
            return texto == null || texto.isBlank() ? null : LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
