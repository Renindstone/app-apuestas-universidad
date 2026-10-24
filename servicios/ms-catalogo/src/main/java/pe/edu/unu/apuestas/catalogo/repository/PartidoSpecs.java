package pe.edu.unu.apuestas.catalogo.repository;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

import pe.edu.unu.apuestas.catalogo.entity.EstadoPartido;
import pe.edu.unu.apuestas.catalogo.entity.Partido;

/**
 * Filtros de partidos que se combinan con and(). Cada filtro solo se agrega si el parámetro llegó.
 */
public final class PartidoSpecs {

    private PartidoSpecs() {
    }

    public static Specification<Partido> enLiga(Long ligaId) {
        return (raiz, consulta, cb) -> cb.equal(raiz.get("liga").get("id"), ligaId);
    }

    public static Specification<Partido> conEstado(EstadoPartido estado) {
        return (raiz, consulta, cb) -> cb.equal(raiz.get("estado"), estado);
    }

    // Desde este instante, incluido.
    public static Specification<Partido> desde(Instant inicio) {
        return (raiz, consulta, cb) -> cb.greaterThanOrEqualTo(raiz.get("fechaHora"), inicio);
    }

    // Hasta este instante, sin incluirlo.
    public static Specification<Partido> antesDe(Instant fin) {
        return (raiz, consulta, cb) -> cb.lessThan(raiz.get("fechaHora"), fin);
    }

    // Partidos en los que el equipo jugó de local o de visitante.
    public static Specification<Partido> deEquipo(Long equipoId) {
        return (raiz, consulta, cb) -> cb.or(
                cb.equal(raiz.get("local").get("id"), equipoId),
                cb.equal(raiz.get("visitante").get("id"), equipoId));
    }
}
