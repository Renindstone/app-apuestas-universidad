package pe.edu.unu.apuestas.catalogo.dto;

import pe.edu.unu.apuestas.catalogo.entity.Liga;

/** Una liga en GET /ligas. */
public record LigaDTO(Long id, String codigo, String nombre, String pais) {

    public static LigaDTO de(Liga l) {
        return new LigaDTO(l.getId(), l.getCodigo(), l.getNombre(), l.getPais());
    }
}
