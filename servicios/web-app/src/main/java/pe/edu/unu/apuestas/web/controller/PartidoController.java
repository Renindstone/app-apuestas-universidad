package pe.edu.unu.apuestas.web.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import pe.edu.unu.apuestas.web.client.ApuestasClient;
import pe.edu.unu.apuestas.web.client.CatalogoClient;
import pe.edu.unu.apuestas.web.client.CuotasClient;
import pe.edu.unu.apuestas.web.dto.CuotaDTO;
import pe.edu.unu.apuestas.web.dto.CuotaManualRequest;
import pe.edu.unu.apuestas.web.dto.EvaluacionDTO;
import pe.edu.unu.apuestas.web.dto.EvaluacionRequest;
import pe.edu.unu.apuestas.web.exception.ApiException;
import pe.edu.unu.apuestas.web.exception.ServicioNoDisponibleException;
import pe.edu.unu.apuestas.web.form.CuotaBetanoForm;
import pe.edu.unu.apuestas.web.form.EvaluacionForm;
import pe.edu.unu.apuestas.web.service.AnalisisService;
import pe.edu.unu.apuestas.web.vista.Mercados;
import pe.edu.unu.apuestas.web.vista.Mercados.Opcion;

/**
 * /partidos/{id} (Análisis), el formulario «evaluar cuota» y /partidos/{id}/cuota-betano.
 */
@Controller
public class PartidoController {

    private final AnalisisService analisis;
    private final ApuestasClient apuestas;
    private final CatalogoClient catalogo;
    private final CuotasClient cuotas;

    public PartidoController(AnalisisService analisis, ApuestasClient apuestas, CatalogoClient catalogo, CuotasClient cuotas) {
        this.analisis = analisis;
        this.apuestas = apuestas;
        this.catalogo = catalogo;
        this.cuotas = cuotas;
    }

    // Pantalla de análisis. Si viene de evaluar una cuota, el resultado llega como atributo flash.
    @GetMapping("/partidos/{id}")
    public String analisis(@PathVariable long id, Model model) {
        if (!model.containsAttribute("evaluacionForm")) {
            model.addAttribute("evaluacionForm", new EvaluacionForm());
        }
        return pintarAnalisis(id, model);
    }

    // Evalúa una cuota con POST /api/apuestas/evaluaciones. La probabilidad la pone el modelo (RN-APU-01).
    @PostMapping("/partidos/{id}/evaluar")
    public String evaluar(@PathVariable long id, @Valid @ModelAttribute("evaluacionForm") EvaluacionForm form, BindingResult resultado,
            Model model, RedirectAttributes flash) {
        if (!resultado.hasErrors()) {
            try {
                Opcion o = Mercados.desdeClave(form.getSeleccion());
                EvaluacionDTO evaluacion = apuestas.evaluar(
                        new EvaluacionRequest(id, o.mercado(), o.seleccion(), o.linea(), form.getCasa(), form.getCuota()));
                flash.addFlashAttribute("evaluacion", evaluacion);
                flash.addFlashAttribute("evaluacionForm", form);
                return "redirect:/partidos/" + id + "#evaluar";
            } catch (IllegalArgumentException e) {
                resultado.rejectValue("seleccion", "invalida", "Elige una selección de la lista.");
            } catch (ApiException e) {
                resultado.reject("api", e.getDetalle());
            } catch (ServicioNoDisponibleException e) {
                resultado.reject("servicio", "No se pudo evaluar: el servicio de apuestas no responde por ahora.");
            }
        }
        return pintarAnalisis(id, model);
    }

    // Formulario para cargar una cuota de Betano a mano.
    @GetMapping("/partidos/{id}/cuota-betano")
    public String formularioBetano(@PathVariable long id, Model model) {
        model.addAttribute("betanoForm", new CuotaBetanoForm());
        return pintarBetano(id, model);
    }

    // Guarda la cuota con POST /api/cuotas/cuotas/manual y vuelve al análisis.
    @PostMapping("/partidos/{id}/cuota-betano")
    public String guardarBetano(@PathVariable long id, @Valid @ModelAttribute("betanoForm") CuotaBetanoForm form, BindingResult resultado,
            Model model, RedirectAttributes flash) {
        if (!resultado.hasErrors()) {
            try {
                Opcion o = Mercados.desdeClave(form.getSeleccion());
                cuotas.guardarManual(new CuotaManualRequest(id, "betano", o.mercado(), o.seleccion(), o.linea(), form.getCuota()));
                flash.addFlashAttribute("aviso", "Cuota de Betano guardada. Ya aparece en la tabla de cuotas.");
                return "redirect:/partidos/" + id;
            } catch (IllegalArgumentException e) {
                resultado.rejectValue("seleccion", "invalida", "Elige una selección de la lista.");
            } catch (ApiException e) {
                resultado.reject("api", e.getDetalle());
            } catch (ServicioNoDisponibleException e) {
                resultado.reject("servicio", "No se pudo guardar: el servicio de cuotas no responde por ahora.");
            }
        }
        return pintarBetano(id, model);
    }

    private String pintarAnalisis(long id, Model model) {
        model.addAttribute("v", analisis.armar(id));
        model.addAttribute("casas", AnalisisService.CASAS);
        model.addAttribute("opciones", Mercados.porMercado());
        return "partido";
    }

    // Datos de la pantalla de Betano: el partido y las cuotas de Betano que ya se cargaron (null si ms-cuotas no responde).
    private String pintarBetano(long id, Model model) {
        model.addAttribute("partido", catalogo.partido(id));
        List<CuotaDTO> deBetano;
        try {
            deBetano = cuotas.cuotas(id).stream().filter(c -> "betano".equals(c.casa())).toList();
        } catch (ServicioNoDisponibleException | ApiException e) {
            deBetano = null;
        }
        model.addAttribute("cuotasBetano", deBetano);
        model.addAttribute("opciones", Mercados.porMercado());
        return "cuota-betano";
    }
}
