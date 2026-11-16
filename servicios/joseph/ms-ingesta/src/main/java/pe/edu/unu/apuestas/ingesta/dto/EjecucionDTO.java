package pe.edu.unu.apuestas.ingesta.dto;

import java.time.Instant;

import pe.edu.unu.apuestas.ingesta.entity.EstadoEjecucion;
import pe.edu.unu.apuestas.ingesta.entity.Ejecucion;
import pe.edu.unu.apuestas.ingesta.entity.TipoIngesta;

/** Una fila de GET /ejecuciones. */
public record EjecucionDTO(Long id, TipoIngesta tipo, String fuente, Instant inicio, Instant fin, EstadoEjecucion estado,
        int filas, String error) {

    public static EjecucionDTO de(Ejecucion e) {
        return new EjecucionDTO(e.getId(), e.getTipo(), e.getFuente(), e.getInicio(), e.getFin(), e.getEstado(), e.getFilas(),
                e.getError());
    }
}
