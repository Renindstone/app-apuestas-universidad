package pe.edu.unu.apuestas.cuotas.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unu.apuestas.cuotas.entity.Cuota;
import pe.edu.unu.apuestas.cuotas.entity.TipoCuota;

/**
 * Acceso a la tabla cuota.
 */
public interface CuotaRepository extends JpaRepository<Cuota, Long> {

    // Para el upsert: la misma cuota (partido, casa, tipo, mercado, selección y línea). Con linea null, Spring Data usa "is null".
    Optional<Cuota> findFirstByPartidoIdAndCasaAndTipoAndMercadoAndSeleccionAndLinea(Long partidoId, String casa, TipoCuota tipo,
            String mercado, String seleccion, BigDecimal linea);

    // Las cuotas de un partido, de la más reciente a la más antigua.
    List<Cuota> findByPartidoIdOrderByCapturadaEnDescIdDesc(Long partidoId);

    // Igual, pero de una sola casa.
    List<Cuota> findByPartidoIdAndCasaOrderByCapturadaEnDescIdDesc(Long partidoId, String casa);
}
