package pe.edu.unu.apuestas.web.dto;

import java.time.Instant;

/** PartidoDTO de ms-catalogo. La fecha llega en UTC y se muestra en hora de Lima. */
public record PartidoDTO(Long id, LigaDTO liga, String temporada, Instant fechaHora, EquipoDTO local, EquipoDTO visitante,
        ArbitroDTO arbitro, Integer golesLocal, Integer golesVisitante, String estado) {

    // "Arsenal vs Chelsea", igual que la descripción que guarda ms-apuestas.
    public String nombre() {
        return local.nombre() + " vs " + visitante.nombre();
    }
}
