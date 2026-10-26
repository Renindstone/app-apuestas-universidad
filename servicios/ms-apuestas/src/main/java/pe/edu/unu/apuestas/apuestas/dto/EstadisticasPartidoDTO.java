package pe.edu.unu.apuestas.apuestas.dto;

/** Lo que responde ms-estadisticas en GET /partidos/{id}/estadisticas. Solo están los campos que liquidan tarjetas y córners. */
public record EstadisticasPartidoDTO(Long partidoId, Lado local, Lado visitante) {

    /** Lo que hizo un equipo. */
    public record Lado(int corners, int amarillas, int rojas) {
    }

    // Tarjetas del partido: amarillas + rojas de los dos equipos (RN-09).
    public int tarjetas() {
        return local.amarillas() + local.rojas() + visitante.amarillas() + visitante.rojas();
    }

    public int corners() {
        return local.corners() + visitante.corners();
    }
}
