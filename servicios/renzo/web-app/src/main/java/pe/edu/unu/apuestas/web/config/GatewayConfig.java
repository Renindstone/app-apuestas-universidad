package pe.edu.unu.apuestas.web.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;

import pe.edu.unu.apuestas.web.dto.ErrorApiDTO;
import pe.edu.unu.apuestas.web.exception.ApiException;
import pe.edu.unu.apuestas.web.exception.NoAutorizadoException;
import pe.edu.unu.apuestas.web.exception.ServicioNoDisponibleException;
import pe.edu.unu.apuestas.web.sesion.SesionUsuario;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cliente HTTP hacia el api-gateway. La web nunca llama directo a un microservicio (RN-WEB-01).
 */
@Configuration
public class GatewayConfig {

    // RestClient único para toda la web: URL del gateway, token de la sesión, tiempos de espera y traducción de errores.
    @Bean
    public RestClient gatewayRestClient(@Value("${gateway.url}") String gatewayUrl, SesionUsuario sesion, JsonMapper jsonMapper) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(8));

        return RestClient.builder()
                .baseUrl(gatewayUrl)
                .requestFactory(fabrica)
                .configureMessageConverters(c -> c.withJsonConverter(new JacksonJsonHttpMessageConverter(jsonMapper)))
                // Añade "Authorization: Bearer <token>" cuando hay sesión iniciada.
                .requestInterceptor((peticion, cuerpo, ejecucion) -> {
                    if (sesion.estaAutenticado()) {
                        peticion.getHeaders().setBearerAuth(sesion.getToken());
                    }
                    try {
                        return ejecucion.execute(peticion, cuerpo);
                    } catch (IOException e) {
                        // El gateway está apagado o no respondió a tiempo.
                        throw new ServicioNoDisponibleException("El gateway no responde", e);
                    }
                })
                // Traduce los errores del gateway: 401 cierra la sesión, 5xx es «no disponible» y el resto llega al formulario.
                .defaultStatusHandler(HttpStatusCode::isError, (peticion, respuesta) -> {
                    int codigo = respuesta.getStatusCode().value();
                    ErrorApiDTO error = leerError(jsonMapper, respuesta.getBody(), codigo);
                    if (codigo == 401) {
                        throw new NoAutorizadoException(error.detalle());
                    }
                    if (codigo >= 500) {
                        throw new ServicioNoDisponibleException(error.detalle(), null);
                    }
                    throw new ApiException(codigo, error.error(), error.detalle());
                })
                .build();
    }

    // Lee el JSON de error común {"error", "detalle"}. Si el cuerpo no tiene ese formato, arma uno genérico.
    private static ErrorApiDTO leerError(JsonMapper jsonMapper, InputStream cuerpo, int codigo) {
        try {
            ErrorApiDTO error = jsonMapper.readValue(cuerpo, ErrorApiDTO.class);
            if (error != null && error.detalle() != null) {
                return error;
            }
        } catch (RuntimeException e) {
            // Cuerpo vacío o que no es JSON: se usa el mensaje genérico de abajo.
        }
        return new ErrorApiDTO("HTTP_" + codigo, "El servidor respondió con el código " + codigo + ".");
    }
}
