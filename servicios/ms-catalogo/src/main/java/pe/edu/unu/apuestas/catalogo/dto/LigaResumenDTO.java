package pe.edu.unu.apuestas.catalogo.dto;

import pe.edu.unu.apuestas.catalogo.entity.Liga;

/** La liga dentro de un PartidoDTO: {"id", "codigo", "nombre"}. */
public record LigaResumenDTO(Long id, String codigo, String nombre) {

    public static LigaResumenDTO de(Liga l) {
        return new LigaResumenDTO(l.getId(), l.getCodigo(), l.getNombre());
    }
}
