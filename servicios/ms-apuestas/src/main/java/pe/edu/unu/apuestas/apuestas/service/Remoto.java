package pe.edu.unu.apuestas.apuestas.service;

import java.util.function.Supplier;

import org.springframework.http.HttpStatus;

import feign.FeignException;
import pe.edu.unu.apuestas.apuestas.exception.ApiException;

/**
 * Llamadas Feign a los otros servicios con los errores de siempre: 404 si el partido no existe, 409 si el modelo aún no
 * tiene datos y 503 si el servicio no responde (RNF-04). Nunca se inventa un dato que falta.
 */
final class Remoto {

    private Remoto() {
    }

    static <T> T llamar(String servicio, Long partidoId, Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (FeignException.NotFound e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PARTIDO_NO_ENCONTRADO", "No existe el partido " + partidoId + ".");
        } catch (FeignException.Conflict e) {
            throw new ApiException(HttpStatus.CONFLICT, "SIN_DATOS", "El modelo aún no tiene datos para el partido " + partidoId + ".");
        } catch (FeignException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE", servicio + " no responde.");
        }
    }
}
