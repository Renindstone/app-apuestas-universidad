package pe.edu.unu.apuestas.catalogo.dto;

import java.time.Instant;

import pe.edu.unu.apuestas.catalogo.entity.EstadoPartido;
import pe.edu.unu.apuestas.catalogo.entity.Partido;

/**
 * PartidoDTO: el JSON de un partido que usan todos los servicios (contrato de ms-catalogo).
 * arbitro, golesLocal y golesVisitante son null cuando no se conocen.
 */
public record PartidoDTO(Long id, LigaResumenDTO liga, String temporada, Instant fechaHora, ReferenciaDTO local,
        ReferenciaDTO visitante, ReferenciaDTO arbitro, Integer golesLocal, Integer golesVisitante, EstadoPartido estado) {

    public static PartidoDTO de(Partido p) {
        return new PartidoDTO(p.getId(), LigaResumenDTO.de(p.getLiga()), p.getTemporada(), p.getFechaHora(),
                new ReferenciaDTO(p.getLocal().getId(), p.getLocal().getNombre()),
                new ReferenciaDTO(p.getVisitante().getId(), p.getVisitante().getNombre()),
                p.getArbitro() == null ? null : new ReferenciaDTO(p.getArbitro().getId(), p.getArbitro().getNombre()),
                p.getGolesLocal(), p.getGolesVisitante(), p.getEstado());
    }
}
