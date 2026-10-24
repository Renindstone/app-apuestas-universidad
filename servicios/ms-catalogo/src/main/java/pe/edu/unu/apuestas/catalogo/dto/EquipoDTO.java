package pe.edu.unu.apuestas.catalogo.dto;

import pe.edu.unu.apuestas.catalogo.entity.Equipo;

/** Un equipo en GET /equipos. */
public record EquipoDTO(Long id, String nombre, Long ligaId) {

    public static EquipoDTO de(Equipo e) {
        return new EquipoDTO(e.getId(), e.getNombre(), e.getLiga().getId());
    }
}
