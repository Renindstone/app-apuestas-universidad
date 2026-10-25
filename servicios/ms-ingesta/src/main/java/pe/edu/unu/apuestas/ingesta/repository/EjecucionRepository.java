package pe.edu.unu.apuestas.ingesta.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unu.apuestas.ingesta.entity.EstadoEjecucion;
import pe.edu.unu.apuestas.ingesta.entity.Ejecucion;

/**
 * Acceso a la tabla ejecucion_ingesta.
 */
public interface EjecucionRepository extends JpaRepository<Ejecucion, Long> {

    boolean existsByEstado(EstadoEjecucion estado);

    List<Ejecucion> findByEstado(EstadoEjecucion estado);
}
