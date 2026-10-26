package pe.edu.unu.apuestas.gateway.seguridad;

import java.util.regex.Pattern;

import org.springframework.http.HttpMethod;

/**
 * Rutas que no piden token (RN-GAT-01): el login y el registro, los GET del catálogo y el Swagger de cada servicio.
 * /actuator/health no pasa por aquí porque lo atiende el propio gateway, no una ruta.
 */
public final class RutasPublicas {

    // /api/<servicio>/v3/api-docs y lo que cuelgue de ahí.
    private static final Pattern SWAGGER = Pattern.compile("^/api/[a-z]+/v3/api-docs(/.*)?$");

    private RutasPublicas() {
    }

    public static boolean esPublica(HttpMethod metodo, String ruta) {
        if (HttpMethod.POST.equals(metodo) && ruta.startsWith("/api/usuarios/auth/")) {
            return true;
        }
        if (HttpMethod.GET.equals(metodo) && (ruta.equals("/api/catalogo") || ruta.startsWith("/api/catalogo/"))) {
            return true;
        }
        return HttpMethod.GET.equals(metodo) && SWAGGER.matcher(ruta).matches();
    }
}
