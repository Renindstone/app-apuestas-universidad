package pe.edu.unu.apuestas.web.dto;

import java.io.Serializable;

/** Resultado de evaluar una cuota. veredicto: CON_VALOR, MARGINAL o SIN_VALOR. */
public record EvaluacionDTO(double probabilidad, double cuotaJusta, double valorEsperado, double stakePct, String veredicto)
        implements Serializable {
}
