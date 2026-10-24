package pe.edu.unu.apuestas.web.sesion;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Protege las pantallas: sin sesión se va a /login, y /admin/** solo lo ve el ADMIN (RN-WEB-06).
 * Las rutas públicas se excluyen en WebConfig.
 */
@Component
public class SesionInterceptor implements HandlerInterceptor {

    private final SesionUsuario sesion;

    public SesionInterceptor(SesionUsuario sesion) {
        this.sesion = sesion;
    }

    // Se ejecuta antes de cada controlador. Si devuelve false, la petición ya se redirigió.
    @Override
    public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta, Object handler) throws Exception {
        if (!sesion.estaAutenticado()) {
            respuesta.sendRedirect(peticion.getContextPath() + "/login");
            return false;
        }
        if (peticion.getRequestURI().startsWith(peticion.getContextPath() + "/admin") && !sesion.esAdmin()) {
            FlashMap flash = RequestContextUtils.getOutputFlashMap(peticion);
            flash.put("aviso", "La ingesta solo está disponible para el administrador.");
            RequestContextUtils.saveOutputFlashMap("/", peticion, respuesta);
            respuesta.sendRedirect(peticion.getContextPath() + "/");
            return false;
        }
        return true;
    }
}
