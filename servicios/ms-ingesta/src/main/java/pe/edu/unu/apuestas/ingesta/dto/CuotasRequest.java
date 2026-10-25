package pe.edu.unu.apuestas.ingesta.dto;

import java.util.List;

/** Cuerpo de POST ms-cuotas /interno/cuotas: las cuotas de una casa para un partido. */
public record CuotasRequest(Long partidoId, String casa, String tipo, List<CuotaDTO> cuotas) {
}
