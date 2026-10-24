package pe.edu.unu.apuestas.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import pe.edu.unu.apuestas.web.sesion.SesionInterceptor;

/**
 * Registra el interceptor de sesión en todas las rutas, salvo las públicas y los archivos estáticos.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SesionInterceptor sesionInterceptor;

    public WebConfig(SesionInterceptor sesionInterceptor) {
        this.sesionInterceptor = sesionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sesionInterceptor)
                .excludePathPatterns("/login", "/registro", "/css/**", "/js/**", "/favicon.ico", "/actuator/**", "/error");
    }
}
