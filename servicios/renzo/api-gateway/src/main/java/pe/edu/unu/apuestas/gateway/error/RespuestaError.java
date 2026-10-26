package pe.edu.unu.apuestas.gateway.error;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Escribe una respuesta de error con el formato común de todos los servicios: {"error": "CODIGO", "detalle": "mensaje"}.
 */
public final class RespuestaError {

    private RespuestaError() {
    }

    // Corta la petición aquí: pone el código HTTP y el JSON, y no la reenvía a ningún servicio.
    public static Mono<Void> escribir(ServerWebExchange exchange, HttpStatus estado, String codigo, String detalle) {
        ServerHttpResponse respuesta = exchange.getResponse();
        respuesta.setStatusCode(estado);
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"error\":\"" + escapar(codigo) + "\",\"detalle\":\"" + escapar(detalle) + "\"}";
        DataBuffer cuerpo = respuesta.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return respuesta.writeWith(Mono.just(cuerpo));
    }

    // Escapa comillas, barras y saltos de línea para que el JSON quede válido.
    private static String escapar(String texto) {
        return texto == null ? "" : texto.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }
}
