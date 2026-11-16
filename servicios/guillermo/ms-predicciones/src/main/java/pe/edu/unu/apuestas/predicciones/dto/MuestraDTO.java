package pe.edu.unu.apuestas.predicciones.dto;

/** Partidos de muestra: los del local en casa y los del visitante fuera (como mucho 10 cada uno). */
public record MuestraDTO(long partidosLocal, long partidosVisitante) {
}
