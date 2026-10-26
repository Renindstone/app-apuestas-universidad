package pe.edu.unu.apuestas.web.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.edu.unu.apuestas.web.client.IngestaClient;
import pe.edu.unu.apuestas.web.dto.EjecucionDTO;
import pe.edu.unu.apuestas.web.exception.ApiException;
import pe.edu.unu.apuestas.web.exception.ServicioNoDisponibleException;

/**
 * /admin/ingesta (solo ADMIN, lo controla SesionInterceptor): lanzar la ingesta a mano y ver las ejecuciones.
 */
@Controller
public class IngestaController {

    private final IngestaClient ingesta;

    public IngestaController(IngestaClient ingesta) {
        this.ingesta = ingesta;
    }

    // Tabla de ejecuciones. Si alguna sigue EN_CURSO, la página se recarga sola cada 5 segundos.
    @GetMapping("/admin/ingesta")
    public String ejecuciones(Model model) {
        List<EjecucionDTO> lista;
        try {
            lista = ingesta.ejecuciones(20);
        } catch (ServicioNoDisponibleException | ApiException e) {
            lista = null;
        }
        model.addAttribute("ejecuciones", lista);
        model.addAttribute("enCurso", lista != null && lista.stream().anyMatch(e -> "EN_CURSO".equals(e.estado())));
        return "admin-ingesta";
    }

    // Lanza HISTORICO o PROXIMOS. ms-ingesta responde 202 y sigue trabajando en segundo plano.
    @PostMapping("/admin/ingesta/ejecutar")
    public String ejecutar(@RequestParam String tipo, RedirectAttributes flash) {
        String elegido = "PROXIMOS".equals(tipo) ? "PROXIMOS" : "HISTORICO";
        try {
            ingesta.ejecutar(elegido);
            flash.addFlashAttribute("aviso", "PROXIMOS".equals(elegido)
                    ? "Carga de próximos partidos en curso. La tabla se actualiza sola."
                    : "Carga del histórico en curso. La tabla se actualiza sola.");
        } catch (ApiException e) {
            flash.addFlashAttribute("aviso", e.getDetalle());
        } catch (ServicioNoDisponibleException e) {
            flash.addFlashAttribute("aviso", "La ingesta no responde por ahora. Intenta de nuevo en unos minutos.");
        }
        return "redirect:/admin/ingesta";
    }
}
