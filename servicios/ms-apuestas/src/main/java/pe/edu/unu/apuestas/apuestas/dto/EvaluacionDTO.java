package pe.edu.unu.apuestas.apuestas.dto;

/** Resultado de evaluar una cuota (RN-03 a RN-05). veredicto: CON_VALOR, MARGINAL o SIN_VALOR. */
public record EvaluacionDTO(double probabilidad, double cuotaJusta, double valorEsperado, double stakePct, String veredicto) {
}
