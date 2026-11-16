package pe.edu.unu.apuestas.apuestas.dto;

import java.util.List;

/** Lo que responde ms-predicciones en GET /partidos/{id}. Solo están los campos que usa este servicio. */
public record PrediccionDTO(Long partidoId, String partido, Muestra muestra, List<Mercado> mercados) {

    /** Partidos de muestra del local en casa y del visitante fuera. */
    public record Muestra(long partidosLocal, long partidosVisitante) {
    }

    /** Probabilidad del modelo para una selección. linea en null si el mercado no lleva. */
    public record Mercado(String mercado, String seleccion, Double linea, double probabilidad, double cuotaJusta) {
    }
}
