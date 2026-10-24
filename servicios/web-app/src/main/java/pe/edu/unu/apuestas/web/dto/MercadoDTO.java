package pe.edu.unu.apuestas.web.dto;

/** Probabilidad y cuota justa de una selección según el modelo. */
public record MercadoDTO(String mercado, String seleccion, Double linea, double probabilidad, double cuotaJusta) implements Seleccion {
}
