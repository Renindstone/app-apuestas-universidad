package pe.edu.unu.apuestas.estadisticas.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.unu.apuestas.estadisticas.entity.Condicion;
import pe.edu.unu.apuestas.estadisticas.entity.EstadisticaPartido;

/**
 * Acceso a la tabla estadistica_partido. Los promedios se calculan en la base de datos con AVG (sección 6.4).
 * Tarjetas = amarillas + rojas (RN-09).
 */
public interface EstadisticaRepository extends JpaRepository<EstadisticaPartido, Long> {

    String PROMEDIOS = "select new pe.edu.unu.apuestas.estadisticas.repository.Agregado(count(e), avg(e.goles), avg(e.tiros),"
            + " avg(e.tirosPuerta), avg(e.corners), avg(e.faltas), avg(e.amarillas + e.rojas)) from EstadisticaPartido e ";
    String TOTALES = "select new pe.edu.unu.apuestas.estadisticas.repository.Totales(count(distinct e.partidoId),"
            + " sum(e.amarillas + e.rojas), sum(e.faltas)) from EstadisticaPartido e ";

    // Para el upsert: la fila de un equipo en un partido (UNIQUE(EpaIdPartido, EpaIdEquipo)).
    Optional<EstadisticaPartido> findByPartidoIdAndEquipoId(Long partidoId, Long equipoId);

    // Las dos filas de un partido: ninguna si aún no se jugó.
    List<EstadisticaPartido> findByPartidoId(Long partidoId);

    // Los últimos N partidos del equipo, del más reciente al más antiguo y sin importar la temporada (RN-EST-03).
    List<EstadisticaPartido> findByEquipoIdOrderByFechaDesc(Long equipoId, Pageable pagina);

    // Igual, pero solo como local o solo como visitante.
    List<EstadisticaPartido> findByEquipoIdAndCondicionOrderByFechaDesc(Long equipoId, Condicion condicion, Pageable pagina);

    // A favor: las filas del propio equipo en esos partidos (RN-EST-02).
    @Query(PROMEDIOS + "where e.equipoId = :equipoId and e.partidoId in :partidos")
    Agregado aFavor(Long equipoId, Collection<Long> partidos);

    // En contra: las filas de sus rivales en esos mismos partidos (RN-EST-02).
    @Query(PROMEDIOS + "where e.rivalId = :equipoId and e.partidoId in :partidos")
    Agregado enContra(Long equipoId, Collection<Long> partidos);

    // Media de la liga por equipo y partido en una condición. temporada en null toma todas las cargadas.
    @Query(PROMEDIOS + "where e.ligaId = :ligaId and e.condicion = :condicion and (:temporada is null or e.temporada = :temporada)")
    Agregado deLiga(Long ligaId, Condicion condicion, String temporada);

    // Una fila cualquiera del árbitro, para saber de qué liga es.
    Optional<EstadisticaPartido> findFirstByArbitroId(Long arbitroId);

    // Tarjetas y faltas de los partidos que dirigió el árbitro.
    @Query(TOTALES + "where e.arbitroId = :arbitroId")
    Totales totalesDeArbitro(Long arbitroId);

    // Tarjetas y faltas de todos los partidos de la liga, para comparar al árbitro.
    @Query(TOTALES + "where e.ligaId = :ligaId")
    Totales totalesDeLiga(Long ligaId);
}
