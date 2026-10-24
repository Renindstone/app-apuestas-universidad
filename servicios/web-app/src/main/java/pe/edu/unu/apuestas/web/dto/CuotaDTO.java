package pe.edu.unu.apuestas.web.dto;

import java.time.Instant;

/** Cuota más reciente de una casa para una selección. */
public record CuotaDTO(String casa, String mercado, String seleccion, Double linea, double valor, String tipo, Instant capturadaEn)
        implements Seleccion {
}
