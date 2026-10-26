package pe.edu.unu.apuestas.estadisticas.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unu.apuestas.estadisticas.dto.EstadisticasPartidoDTO;
import pe.edu.unu.apuestas.estadisticas.dto.EstadisticasRequest;
import pe.edu.unu.apuestas.estadisticas.dto.PerfilArbitroDTO;
import pe.edu.unu.apuestas.estadisticas.dto.PromediosEquipoDTO;
import pe.edu.unu.apuestas.estadisticas.dto.PromediosLigaDTO;
import pe.edu.unu.apuestas.estadisticas.entity.Condicion;
import pe.edu.unu.apuestas.estadisticas.entity.EstadisticaPartido;
import pe.edu.unu.apuestas.estadisticas.exception.ApiException;
import pe.edu.unu.apuestas.estadisticas.repository.Agregado;
import pe.edu.unu.apuestas.estadisticas.repository.EstadisticaRepository;
import pe.edu.unu.apuestas.estadisticas.repository.Totales;

/**
 * Guarda las estadísticas que manda ms-ingesta y calcula los promedios de equipo, liga y árbitro.
 */
@Service
public class EstadisticasService {

    private final EstadisticaRepository filas;

    public EstadisticasService(EstadisticaRepository filas) {
        this.filas = filas;
    }

    // POST /interno/estadisticas: crea o actualiza las dos filas del partido, una por equipo (RN-EST-01).
    @Transactional
    public void guardar(EstadisticasRequest partido) {
        if (partido.local().equipoId().equals(partido.visitante().equipoId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDACION", "El local y el visitante no pueden ser el mismo equipo.");
        }
        guardarLado(partido, partido.local(), partido.visitante().equipoId(), Condicion.LOCAL);
        guardarLado(partido, partido.visitante(), partido.local().equipoId(), Condicion.VISITANTE);
    }

    // Si la fila (partido, equipo) ya existe se actualiza; si no, se crea. Así reenviar un partido no lo duplica.
    private void guardarLado(EstadisticasRequest partido, EstadisticasRequest.Lado propio, Long rivalId, Condicion condicion) {
        EstadisticaPartido fila = filas.findByPartidoIdAndEquipoId(partido.partidoId(), propio.equipoId())
                .orElseGet(() -> new EstadisticaPartido(partido.partidoId(), propio.equipoId()));
        fila.actualizar(partido, propio, rivalId, condicion);
        filas.save(fila);
    }

    /**
     * GET /equipos/{id}/promedios: toma los últimos N partidos del equipo en esa condición (RN-EST-03) y promedia
     * sus filas (a favor) y las de sus rivales en esos mismos partidos (en contra, RN-EST-02).
     */
    @Transactional(readOnly = true)
    public PromediosEquipoDTO promediosDeEquipo(Long equipoId, Condicion condicion, int ultimos) {
        if (ultimos < 1 || ultimos > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDACION", "«ultimos» debe estar entre 1 y 100.");
        }
        PageRequest pagina = PageRequest.of(0, ultimos);
        List<EstadisticaPartido> recientes = condicion == Condicion.TODOS
                ? filas.findByEquipoIdOrderByFechaDesc(equipoId, pagina)
                : filas.findByEquipoIdAndCondicionOrderByFechaDesc(equipoId, condicion, pagina);
        if (recientes.isEmpty()) {
            // RN-EST-04: sin partidos, 0 y los promedios en null, nunca en cero.
            return new PromediosEquipoDTO(equipoId, condicion, 0, null, null);
        }
        List<Long> partidos = recientes.stream().map(EstadisticaPartido::getPartidoId).toList();
        return new PromediosEquipoDTO(equipoId, condicion, partidos.size(),
                filas.aFavor(equipoId, partidos).promedios(), filas.enContra(equipoId, partidos).promedios());
    }

    // GET /ligas/{id}/promedios: la media de la liga como local y como visitante. Sin temporada, toma las 2 cargadas.
    @Transactional(readOnly = true)
    public PromediosLigaDTO promediosDeLiga(Long ligaId, String temporada) {
        Agregado local = filas.deLiga(ligaId, Condicion.LOCAL, temporada);
        Agregado visitante = filas.deLiga(ligaId, Condicion.VISITANTE, temporada);
        // Cada partido tiene una sola fila de local: contarlas es contar los partidos.
        return new PromediosLigaDTO(ligaId, temporada, local.filas(), local.promedios(), visitante.promedios());
    }

    // GET /arbitros/{id}/perfil: tarjetas y faltas por partido sumando a los dos equipos, y la media de su liga (RN-EST-05).
    @Transactional(readOnly = true)
    public PerfilArbitroDTO perfilDeArbitro(Long arbitroId) {
        Totales arbitro = filas.totalesDeArbitro(arbitroId);
        Double mediaLiga = filas.findFirstByArbitroId(arbitroId)
                .map(fila -> filas.totalesDeLiga(fila.getLigaId()).tarjetasPorPartido())
                .orElse(null);
        return new PerfilArbitroDTO(arbitroId, arbitro.partidos(), arbitro.tarjetasPorPartido(), arbitro.faltasPorPartido(), mediaLiga);
    }

    // GET /partidos/{id}/estadisticas: las dos filas del partido. 404 si aún no se jugó (RN-EST-06).
    @Transactional(readOnly = true)
    public EstadisticasPartidoDTO estadisticasDePartido(Long partidoId) {
        Map<Condicion, EstadisticasRequest.Lado> lados = filas.findByPartidoId(partidoId).stream()
                .collect(Collectors.toMap(EstadisticaPartido::getCondicion, EstadisticaPartido::lado));
        if (lados.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PARTIDO_SIN_ESTADISTICAS",
                    "El partido " + partidoId + " aún no se jugó o no tiene estadísticas.");
        }
        return new EstadisticasPartidoDTO(partidoId, lados.get(Condicion.LOCAL), lados.get(Condicion.VISITANTE));
    }
}
