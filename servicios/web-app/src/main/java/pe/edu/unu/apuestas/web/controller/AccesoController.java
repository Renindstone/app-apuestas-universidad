package pe.edu.unu.apuestas.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import pe.edu.unu.apuestas.web.client.UsuariosClient;
import pe.edu.unu.apuestas.web.dto.LoginResponse;
import pe.edu.unu.apuestas.web.dto.LoginRequest;
import pe.edu.unu.apuestas.web.dto.RegistroRequest;
import pe.edu.unu.apuestas.web.exception.ApiException;
import pe.edu.unu.apuestas.web.exception.NoAutorizadoException;
import pe.edu.unu.apuestas.web.exception.ServicioNoDisponibleException;
import pe.edu.unu.apuestas.web.form.LoginForm;
import pe.edu.unu.apuestas.web.form.RegistroForm;
import pe.edu.unu.apuestas.web.sesion.SesionUsuario;

/**
 * /login, /registro y /salir. Llama a /api/usuarios/auth/login y /api/usuarios/auth/registro.
 */
@Controller
public class AccesoController {

    private final UsuariosClient usuarios;
    private final SesionUsuario sesion;

    public AccesoController(UsuariosClient usuarios, SesionUsuario sesion) {
        this.usuarios = usuarios;
        this.sesion = sesion;
    }

    // Formulario de inicio de sesión. Si ya hay sesión, va directo a Hoy.
    @GetMapping("/login")
    public String login(Model model) {
        if (sesion.estaAutenticado()) {
            return "redirect:/";
        }
        LoginForm form = new LoginForm();
        form.setNombreUsuario((String) model.getAttribute("usuario"));
        model.addAttribute("form", form);
        return "login";
    }

    // Inicia sesión: guarda el JWT en la sesión del servidor (RN-WEB-02) y pide la banca para el encabezado.
    @PostMapping("/login")
    public String entrar(@Valid @ModelAttribute("form") LoginForm form, BindingResult resultado) {
        if (resultado.hasErrors()) {
            return "login";
        }
        try {
            LoginResponse respuesta = usuarios.login(new LoginRequest(form.getNombreUsuario().trim(), form.getClave()));
            sesion.iniciar(respuesta.token(), respuesta.nombreUsuario(), respuesta.rol());
            cargarBanca();
            return "redirect:/";
        } catch (NoAutorizadoException e) {
            // Mismo mensaje si falla el usuario o la clave (RN-USU-04).
            resultado.reject("credenciales", "Usuario o contraseña incorrectos.");
        } catch (ApiException e) {
            resultado.reject("api", e.getDetalle());
        } catch (ServicioNoDisponibleException e) {
            resultado.reject("servicio", "No pudimos conectar con el servidor. Intenta de nuevo en unos minutos.");
        }
        return "login";
    }

    // Formulario de registro.
    @GetMapping("/registro")
    public String registro(Model model) {
        if (sesion.estaAutenticado()) {
            return "redirect:/";
        }
        model.addAttribute("form", new RegistroForm());
        return "registro";
    }

    // Crea la cuenta y vuelve al login con el usuario ya escrito. El rol siempre lo pone el servidor (RN-USU-03).
    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("form") RegistroForm form, BindingResult resultado, RedirectAttributes flash) {
        if (!form.getClave().equals(form.getClaveConfirmacion())) {
            resultado.rejectValue("claveConfirmacion", "noCoincide", "Las contraseñas no coinciden.");
        }
        if (resultado.hasErrors()) {
            return "registro";
        }
        try {
            usuarios.registrar(new RegistroRequest(form.getNombreUsuario().trim(), form.getEmail().trim(), form.getClave(),
                    form.getClaveConfirmacion()));
            flash.addFlashAttribute("aviso", "Cuenta creada. Ya puedes iniciar sesión.");
            flash.addFlashAttribute("usuario", form.getNombreUsuario().trim());
            return "redirect:/login";
        } catch (ApiException e) {
            resultado.reject("api", e.getDetalle());
        } catch (ServicioNoDisponibleException e) {
            resultado.reject("servicio", "No pudimos conectar con el servidor. Intenta de nuevo en unos minutos.");
        }
        return "registro";
    }

    // Cierra la sesión: borra el token y la HttpSession.
    @PostMapping("/salir")
    public String salir(HttpSession session) {
        sesion.cerrar();
        session.invalidate();
        return "redirect:/login";
    }

    // Guarda la banca en la sesión para mostrarla en el encabezado. Si ms-usuarios no responde, se verá después en Perfil.
    private void cargarBanca() {
        try {
            sesion.setBanca(usuarios.perfil().banca());
        } catch (ServicioNoDisponibleException | ApiException e) {
            sesion.setBanca(null);
        }
    }
}
