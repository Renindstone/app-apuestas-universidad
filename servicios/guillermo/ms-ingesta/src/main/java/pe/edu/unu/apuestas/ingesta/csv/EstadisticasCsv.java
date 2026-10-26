package pe.edu.unu.apuestas.ingesta.csv;

/** Estadísticas de un equipo en una fila: HS, HST, HC, HF, HY y HR del local, o AS, AST, AC, AF, AY y AR del visitante. */
public record EstadisticasCsv(int tiros, int tirosPuerta, int corners, int faltas, int amarillas, int rojas) {
}
