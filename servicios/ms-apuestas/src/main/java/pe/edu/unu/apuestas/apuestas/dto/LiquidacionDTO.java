package pe.edu.unu.apuestas.apuestas.dto;

/** POST /interno/liquidar: cuántos picks y apuestas se liquidaron y cuántos siguen pendientes. */
public record LiquidacionDTO(int picks, int apuestas, int pendientes) {
}
