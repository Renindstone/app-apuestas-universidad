package pe.edu.unu.apuestas.apuestas.dto;

import java.math.BigDecimal;

import pe.edu.unu.apuestas.apuestas.entity.Pick;

/** La apuesta del día dentro de GET /apuesta-del-dia. */
public record PickDTO(Long id, Long partidoId, String partido, String mercado, String seleccion, BigDecimal linea, String casa,
        BigDecimal cuota, double probabilidad, double valorEsperado, double stakePct) {

    public static PickDTO de(Pick p) {
        return new PickDTO(p.getId(), p.getPartidoId(), p.getDescripcion(), p.getMercado(), p.getSeleccion(), p.getLinea(), p.getCasa(),
                p.getCuota(), p.getProbabilidad(), p.getValorEsperado(), p.getStakePct());
    }
}
