package pe.edu.unu.apuestas.web.vista;

import java.util.List;

/** Las filas de un mismo mercado (Resultado, Goles, Ambos marcan, Tarjetas o Córners). */
public record GrupoMercados(String nombre, List<FilaMercado> filas) {
}
