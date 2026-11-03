package pe.edu.unu.apuestas.gateway.estado;

import java.util.List;

import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unu.apuestas.gateway.config.RutasConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * GET /estado: cuántas instancias de cada servicio hay registradas en Eureka en este momento.
 * La web lo lee para avisar en el pie de página qué servicio está apagado (0 instancias).
 * No necesita token y solo devuelve nombres y cantidades. Lo atiende el propio gateway, no una ruta.
 */
@RestController
public class EstadoController {

    /** Un servicio y cuántas instancias suyas están registradas. */
    public record EstadoServicio(String servicio, int instancias) {
    }

    private final ReactiveDiscoveryClient eureka;

    public EstadoController(ReactiveDiscoveryClient eureka) {
        this.eureka = eureka;
    }

    @GetMapping("/estado")
    public Mono<List<EstadoServicio>> estado() {
        return Flux.fromIterable(RutasConfig.servicios())
                .concatMap(nombre -> eureka.getInstances(nombre).count()
                        .map(cantidad -> new EstadoServicio(nombre, cantidad.intValue())))
                .collectList();
    }
}
