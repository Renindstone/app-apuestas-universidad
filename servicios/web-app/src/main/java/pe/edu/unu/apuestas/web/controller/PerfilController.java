package pe.edu.unu.apuestas.web.controller;

import java.math.RoundingMode;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import pe.edu.unu.apuestas.web.client.ApuestasClient;
import pe.edu.unu.apuestas.web.client.UsuariosClient;
import pe.edu.unu.apuestas.web.dto.ApuestaDelDiaDTO;
import pe.edu.unu.apuestas.web.dto.BancaRequest;
import pe.edu.unu.apuestas.web.dto.UsuarioDTO;
import pe.edu.unu.apuestas.web.exception.ApiException;
import pe.edu.unu.apuestas.web.exception.ServicioNoDisponibleException;
import pe.edu.unu.apuestas.web.form.BancaForm;
import pe.edu.unu.apuestas.web.sesion.SesionUsuario;

/**
 * /perfil: datos de la cuenta y la banca. Llama a /api/usuarios/me y /api/usuarios/me/banca.
 */
@Controller
public class PerfilController {

    private final UsuariosClient usuarios;
    private final ApuestasClient apuestas;
    private final SesionUsuario sesion;

    public PerfilController(UsuariosClient usuarios, ApuestasClient apuestas, SesionUsuario sesion) {
        this.usuarios = usuarios;
        this.apuestas = apuestas;
        this.sesion = sesion;
    }

    @GetMapping("/perfil")
    public String perfil(Model model) {
        return pintar(model, null);
    }

    // Cambia la banca (RN-USU-06: mayor o igual a 0) y actualiza la que se ve en el encabezado.
    @PostMapping("/perfil/banca")
    public String actualizarBanca(@Valid @ModelAttribute("bancaForm") BancaForm form, BindingResult resultado, Model model,
            RedirectAttributes flash) {
        if (!resultado.hasErrors()) {
            try {
                UsuarioDTO usuario = usuarios.actualizarBanca(new BancaRequest(form.getBanca()));
                sesion.setBanca(usuario.banca());
                flash.addFlashAttribute("aviso", "Banca actualizada.");
                return "redirect:/perfil";
            } catch (ApiException e) {
                resultado.reject("api", e.getDetalle());
            } catch (ServicioNoDisponibleException e) {
                resultado.reject("servicio", "No se pudo guardar: el servicio de usuarios no responde por ahora.");
            }
        }
        return pintar(model, form);
    }

    // Pide el perfil y la apuesta del día (para mostrar el stake de hoy en soles). Lo que falle queda en null.
    private String pintar(Model model, BancaForm formConErrores) {
        UsuarioDTO usuario;
        try {
            usuario = usuarios.perfil();
            sesion.setBanca(usuario.banca());
        } catch (ServicioNoDisponibleException | ApiException e) {
            usuario = null;
        }
        ApuestaDelDiaDTO dia;
        try {
            dia = apuestas.apuestaDelDia();
        } catch (ServicioNoDisponibleException | ApiException e) {
            dia = null;
        }
        BancaForm form = formConErrores;
        if (form == null) {
            form = new BancaForm();
            form.setBanca(usuario == null || usuario.banca() == null ? null : usuario.banca().setScale(2, RoundingMode.HALF_UP));
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("pick", dia == null ? null : dia.pick());
        model.addAttribute("bancaForm", form);
        return "perfil";
    }
}
