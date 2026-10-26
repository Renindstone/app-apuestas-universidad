package pe.edu.unu.apuestas.web.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import pe.edu.unu.apuestas.web.client.ApuestasClient;
import pe.edu.unu.apuestas.web.client.CatalogoClient;
import pe.edu.unu.apuestas.web.dto.ApuestaDTO;
import pe.edu.unu.apuestas.web.dto.ApuestaRequest;
import pe.edu.unu.apuestas.web.dto.PartidoDTO;
import pe.edu.unu.apuestas.web.dto.RendimientoDTO;
import pe.edu.unu.apuestas.web.exception.ApiException;
import pe.edu.unu.apuestas.web.exception.ServicioNoDisponibleException;
import pe.edu.unu.apuestas.web.form.ApuestaForm;
import pe.edu.unu.apuestas.web.service.EjecutorParalelo;
import pe.edu.unu.apuestas.web.vista.Formatos;
import pe.edu.unu.apuestas.web.vista.Mercados;
import pe.edu.unu.apuestas.web.vista.Mercados.Opcion;

/**
 * /mis-apuestas: registrar una apuesta, ver la lista por estado y el rendimiento. Cada usuario ve solo lo suyo (RN-14).
 */
@Controller
public class MisApuestasController {

    private final ApuestasClient apuestas;
    private final CatalogoClient catalogo;
    private final EjecutorParalelo paralelo;
    private final Formatos fmt;

    public MisApuestasController(ApuestasClient apuestas, CatalogoClient catalogo, EjecutorParalelo paralelo, Formatos fmt) {
        this.apuestas = apuestas;
        this.catalogo = catalogo;
        this.paralelo = paralelo;
        this.fmt = fmt;
    }

    // ?estado=PENDIENTE o LIQUIDADA elige la pestaña de la lista.
    @GetMapping("/mis-apuestas")
    public String misApuestas(@RequestParam(defaultValue = "PENDIENTE") String estado, Model model) {
        model.addAttribute("apuestaForm", new ApuestaForm());
        return pintar(estado, model);
    }

    // Registra la apuesta con POST /api/apuestas/mis-apuestas y vuelve a la lista de pendientes.
    @PostMapping("/mis-apuestas")
    public String registrar(@Valid @ModelAttribute("apuestaForm") ApuestaForm form, BindingResult resultado, Model model,
            RedirectAttributes flash) {
        if (!resultado.hasErrors()) {
            try {
                Opcion o = Mercados.desdeClave(form.getSeleccion());
                apuestas.registrar(new ApuestaRequest(form.getPartidoId(), o.mercado(), o.seleccion(), o.linea(), form.getCasa(),
                        form.getCuota(), form.getMonto()));
                flash.addFlashAttribute("aviso", "Apuesta registrada. Se liquidará sola cuando termine el partido.");
                return "redirect:/mis-apuestas?estado=PENDIENTE#lista";
            } catch (IllegalArgumentException e) {
                resultado.rejectValue("seleccion", "invalida", "Elige una selección de la lista.");
            } catch (ApiException e) {
                resultado.reject("api", e.getDetalle());
            } catch (ServicioNoDisponibleException e) {
                resultado.reject("servicio", "No se pudo registrar: el servicio de apuestas no responde por ahora.");
            }
        }
        return pintar("PENDIENTE", model);
    }

    // Pide la lista, el rendimiento y los partidos de hoy (para el formulario) a la vez. Lo que falle queda en null.
    private String pintar(String estado, Model model) {
        String filtro = "LIQUIDADA".equals(estado) ? "LIQUIDADA" : "PENDIENTE";
        CompletableFuture<List<ApuestaDTO>> lista = paralelo.intentar(() -> apuestas.misApuestas(filtro));
        CompletableFuture<RendimientoDTO> rendimiento = paralelo.intentar(apuestas::rendimiento);
        CompletableFuture<List<PartidoDTO>> partidos = paralelo.intentar(() -> catalogo.partidosDelDia(fmt.hoy()));
        model.addAttribute("estado", filtro);
        model.addAttribute("apuestas", lista.join());
        model.addAttribute("rendimiento", rendimiento.join());
        model.addAttribute("partidosHoy", partidos.join());
        model.addAttribute("opciones", Mercados.porMercado());
        return "mis-apuestas";
    }
}
