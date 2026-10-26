package pe.edu.unu.apuestas.apuestas.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unu.apuestas.apuestas.entity.EstadoPick;
import pe.edu.unu.apuestas.apuestas.entity.Pick;

/**
 * Acceso a la tabla pick.
 */
public interface PickRepository extends JpaRepository<Pick, Long> {

    // Las candidatas guardadas de un día.
    List<Pick> findByFecha(LocalDate fecha);

    // Para la liquidación: los picks que siguen abiertos.
    List<Pick> findByEstado(EstadoPick estado);

    // Para el historial: las apuestas del día liquidadas desde una fecha, de la más reciente a la más antigua.
    List<Pick> findByEsApuestaDelDiaTrueAndEstadoAndFechaGreaterThanEqualOrderByFechaDesc(EstadoPick estado, LocalDate desde);
}
