package pe.edu.unu.apuestas.gateway.error;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Convierte los errores del gateway en el JSON común (RNF-04): nunca un 500 genérico.
 * Por ejemplo, si ms-catalogo está apagado: 503 {"error":"SERVICIO_NO_DISPONIBLE","detalle":"ms-catalogo no responde."}.
 */
@Component
@Order(-2)
public class ManejadorErroresGateway implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(error);
        }
        // El id de la ruta es el nombre del servicio (ver RutasConfig).
        Route ruta = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String servicio = ruta == null ? "El servicio" : ruta.getId();

        if (tieneCausa(error, ConnectException.class)) {
            return RespuestaError.escribir(exchange, HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE", servicio + " no responde.");
        }
        if (tieneCausa(error, TimeoutException.class)) {
            return RespuestaError.escribir(exchange, HttpStatus.GATEWAY_TIMEOUT, "SERVICIO_NO_DISPONIBLE", servicio + " tardó demasiado en responder.");
        }
        if (error instanceof ResponseStatusException e) {
            HttpStatus estado = HttpStatus.resolve(e.getStatusCode().value());
            if (estado == HttpStatus.NOT_FOUND && ruta == null) {
                return RespuestaError.escribir(exchange, estado, "RUTA_NO_ENCONTRADA", "No existe la ruta " + exchange.getRequest().getPath() + ".");
            }
            if (estado == HttpStatus.SERVICE_UNAVAILABLE) {
                // Eureka no tiene ninguna instancia registrada del servicio.
                return RespuestaError.escribir(exchange, estado, "SERVICIO_NO_DISPONIBLE", servicio + " no está disponible.");
            }
            if (estado == HttpStatus.GATEWAY_TIMEOUT) {
                return RespuestaError.escribir(exchange, estado, "SERVICIO_NO_DISPONIBLE", servicio + " tardó demasiado en responder.");
            }
            if (estado != null && estado.is4xxClientError()) {
                return RespuestaError.escribir(exchange, estado, "PETICION_INVALIDA", e.getReason() == null ? "Petición no válida." : e.getReason());
            }
        }
        return RespuestaError.escribir(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Error inesperado en el gateway.");
    }

    // Busca un tipo de excepción en toda la cadena de causas (Netty envuelve la ConnectException).
    private static boolean tieneCausa(Throwable error, Class<? extends Throwable> tipo) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (tipo.isInstance(t)) {
                return true;
            }
        }
        return false;
    }
}
