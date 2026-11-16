package pe.edu.unu.apuestas.apuestas.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Lo que responde ms-catalogo. Solo están los campos que usa este servicio. Los goles son null si aún no se jugó.
 * fechaHora (UTC) sirve para saber si un partido ya debió terminar y hay que pedir su resultado.
 */
public record PartidoDTO(Long id, Referencia local, Referencia visitante, Integer golesLocal, Integer golesVisitante, String estado,
        Instant fechaHora) {

    // Jackson arma el DTO con el constructor de todos los campos, no con el corto.
    @JsonCreator
    public PartidoDTO {
    }

    /** Sin la hora del partido (la usan las pruebas). */
    public PartidoDTO(Long id, Referencia local, Referencia visitante, Integer golesLocal, Integer golesVisitante, String estado) {
        this(id, local, visitante, golesLocal, golesVisitante, estado, null);
    }

    /** {"id": 11, "nombre": "Arsenal"}. */
    public record Referencia(Long id, String nombre) {
    }

    // "Arsenal vs Chelsea"
    public String descripcion() {
        return local.nombre() + " vs " + visitante.nombre();
    }
}
