package pe.edu.unu.apuestas.apuestas.dto;

/** Lo que responde ms-catalogo. Solo están los campos que usa este servicio. Los goles son null si aún no se jugó. */
public record PartidoDTO(Long id, Referencia local, Referencia visitante, Integer golesLocal, Integer golesVisitante, String estado) {

    /** {"id": 11, "nombre": "Arsenal"}. */
    public record Referencia(Long id, String nombre) {
    }

    // "Arsenal vs Chelsea"
    public String descripcion() {
        return local.nombre() + " vs " + visitante.nombre();
    }
}
