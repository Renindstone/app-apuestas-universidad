package pe.edu.unu.apuestas.catalogo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import pe.edu.unu.apuestas.catalogo.entity.Equipo;
import pe.edu.unu.apuestas.catalogo.entity.Partido;

/**
 * Acceso a la tabla partido. Los filtros opcionales se arman con Specification (ver PartidoSpecs).
 * @EntityGraph trae liga, equipos y árbitro en la misma consulta, sin una consulta extra por partido.
 */
public interface PartidoRepository extends JpaRepository<Partido, Long>, JpaSpecificationExecutor<Partido> {

    @Override
    @EntityGraph(attributePaths = { "liga", "local", "visitante", "arbitro" })
    List<Partido> findAll(Specification<Partido> filtro, Sort orden);

    @Override
    @EntityGraph(attributePaths = { "liga", "local", "visitante", "arbitro" })
    Page<Partido> findAll(Specification<Partido> filtro, Pageable pagina);

    @Override
    @EntityGraph(attributePaths = { "liga", "local", "visitante", "arbitro" })
    Optional<Partido> findById(Long id);

    // Para el upsert: el cruce local–visitante de una temporada (RN-CAT-02).
    Optional<Partido> findByLocalAndVisitanteAndTemporada(Equipo local, Equipo visitante, String temporada);
}
