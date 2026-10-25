package pe.edu.unu.apuestas.ingesta.dto;

import pe.edu.unu.apuestas.ingesta.entity.EstadoEjecucion;

/** Respuesta 202 de POST /ejecutar: {"id": 7, "estado": "EN_CURSO"}. */
public record EjecucionIniciadaDTO(Long id, EstadoEjecucion estado) {
}
