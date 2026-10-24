package pe.edu.unu.apuestas.web.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import jakarta.annotation.PreDestroy;
import pe.edu.unu.apuestas.web.exception.ApiException;
import pe.edu.unu.apuestas.web.exception.ServicioNoDisponibleException;

/**
 * Hace varias llamadas al gateway a la vez (por ejemplo, la predicción y las cuotas de cada partido),
 * para que la página cargue más rápido (RNF-06).
 */
@Component
public class EjecutorParalelo {

    // Hilos virtuales de Java 21: baratos, uno por llamada.
    private final ExecutorService hilos = Executors.newVirtualThreadPerTaskExecutor();

    // Lanza la llamada en otro hilo. Copia el contexto de la petición para que el token de la sesión siga disponible.
    // Si el servicio no responde o devuelve un error, el resultado es null y la vista muestra el aviso (RN-WEB-04).
    public <T> CompletableFuture<T> intentar(Supplier<T> llamada) {
        RequestAttributes contexto = RequestContextHolder.currentRequestAttributes();
        return CompletableFuture.supplyAsync(() -> {
            RequestContextHolder.setRequestAttributes(contexto);
            try {
                return llamada.get();
            } catch (ServicioNoDisponibleException | ApiException e) {
                return null;
            } finally {
                RequestContextHolder.resetRequestAttributes();
            }
        }, hilos);
    }

    @PreDestroy
    public void cerrar() {
        hilos.close();
    }
}
