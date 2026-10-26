package pe.edu.unu.apuestas.web.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.http.HttpServletRequest;
import pe.edu.unu.apuestas.web.sesion.SesionUsuario;

/**
 * Último recurso para los errores que un controlador no atrapó. Nunca se muestra una pantalla de error técnica.
 */
@ControllerAdvice
public class ManejadorErrores {

    private final SesionUsuario sesion;

    public ManejadorErrores(SesionUsuario sesion) {
        this.sesion = sesion;
    }

    // 401 del gateway: se borra la sesión y se vuelve al login con un aviso (RN-WEB-02).
    @ExceptionHandler(NoAutorizadoException.class)
    public String sesionVencida(HttpServletRequest peticion) {
        sesion.cerrar();
        peticion.getSession().invalidate();
        RequestContextUtils.getOutputFlashMap(peticion).put("aviso", "Tu sesión venció. Vuelve a iniciar sesión.");
        return "redirect:/login";
    }

    // Un servicio no respondió y la pantalla no tenía cómo seguir: se muestra un aviso dentro del cuaderno.
    @ExceptionHandler(ServicioNoDisponibleException.class)
    public String servicioNoDisponible(Model model) {
        model.addAttribute("titulo", "Servicio no disponible");
        model.addAttribute("mensaje", "Un servicio no responde por ahora. Intenta de nuevo en unos minutos.");
        return "aviso";
    }

    // Error 4xx que nadie atrapó (por ejemplo, un 404 del catálogo): se muestra el detalle que mandó el servicio.
    @ExceptionHandler(ApiException.class)
    public String errorDeApi(ApiException e, Model model) {
        model.addAttribute("titulo", e.getEstado() == 404 ? "No encontrado" : "No se pudo completar");
        model.addAttribute("mensaje", e.getDetalle());
        return "aviso";
    }
}
