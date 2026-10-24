package pe.edu.unu.apuestas.catalogo.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unu.apuestas.catalogo.dto.EquipoDTO;
import pe.edu.unu.apuestas.catalogo.dto.LigaDTO;
import pe.edu.unu.apuestas.catalogo.dto.PartidoDTO;
import pe.edu.unu.apuestas.catalogo.entity.EstadoPartido;
import pe.edu.unu.apuestas.catalogo.entity.Partido;
import pe.edu.unu.apuestas.catalogo.exception.NoEncontradoException;
import pe.edu.unu.apuestas.catalogo.exception.ValidacionException;
import pe.edu.unu.apuestas.catalogo.repository.EquipoRepository;
import pe.edu.unu.apuestas.catalogo.repository.LigaRepository;
import pe.edu.unu.apuestas.catalogo.repository.PartidoRepository;
import pe.edu.unu.apuestas.catalogo.repository.PartidoSpecs;

/**
 * Consultas del catálogo: ligas, equipos y partidos con filtros opcionales.
 */
@Service
@Transactional(readOnly = true)
public class CatalogoService {

    public static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Sort POR_FECHA = Sort.by("fechaHora").ascending().and(Sort.by("id"));

    private final LigaRepository ligas;
    private final EquipoRepository equipos;
    private final PartidoRepository partidos;

    public CatalogoService(LigaRepository ligas, EquipoRepository equipos, PartidoRepository partidos) {
        this.ligas = ligas;
        this.equipos = equipos;
        this.partidos = partidos;
    }

    // Un día de Lima empieza a las 00:00 de Lima, que son las 05:00 UTC (RN-CAT-06).
    public static Instant inicioDelDia(LocalDate fecha) {
        return fecha.atStartOfDay(LIMA).toInstant();
    }

    public List<LigaDTO> ligas() {
        return ligas.findAll(Sort.by("id")).stream().map(LigaDTO::de).toList();
    }

    // Equipos ordenados por nombre; ligaId es opcional.
    public List<EquipoDTO> equipos(Long ligaId) {
        return (ligaId == null ? equipos.findAllByOrderByNombreAsc() : equipos.findByLigaIdOrderByNombreAsc(ligaId))
                .stream().map(EquipoDTO::de).toList();
    }

    /**
     * Partidos ordenados por fecha. Todos los filtros son opcionales:
     * fecha = un día de Lima; desde/hasta = un rango de días de Lima (los dos incluidos); ligaId; estado.
     */
    public List<PartidoDTO> partidos(LocalDate fecha, LocalDate desde, LocalDate hasta, Long ligaId, EstadoPartido estado) {
        if (fecha != null) {
            desde = fecha;
            hasta = fecha;
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ValidacionException("La fecha «desde» no puede ser posterior a «hasta».");
        }
        Specification<Partido> filtro = Specification.unrestricted();
        if (desde != null) {
            filtro = filtro.and(PartidoSpecs.desde(inicioDelDia(desde)));
        }
        if (hasta != null) {
            filtro = filtro.and(PartidoSpecs.antesDe(inicioDelDia(hasta.plusDays(1))));
        }
        if (ligaId != null) {
            filtro = filtro.and(PartidoSpecs.enLiga(ligaId));
        }
        if (estado != null) {
            filtro = filtro.and(PartidoSpecs.conEstado(estado));
        }
        return partidos.findAll(filtro, POR_FECHA).stream().map(PartidoDTO::de).toList();
    }

    // Un partido por su ID, o 404.
    public PartidoDTO partido(Long id) {
        return partidos.findById(id).map(PartidoDTO::de)
                .orElseThrow(() -> new NoEncontradoException("PARTIDO_NO_ENCONTRADO", "No existe el partido " + id));
    }

    // Últimos partidos de un equipo (de local o de visitante), del más reciente al más antiguo.
    public List<PartidoDTO> partidosDeEquipo(Long equipoId, EstadoPartido estado, int limite) {
        if (!equipos.existsById(equipoId)) {
            throw new NoEncontradoException("EQUIPO_NO_ENCONTRADO", "No existe el equipo " + equipoId);
        }
        if (limite < 1 || limite > 100) {
            throw new ValidacionException("El límite debe estar entre 1 y 100.");
        }
        Specification<Partido> filtro = PartidoSpecs.deEquipo(equipoId);
        if (estado != null) {
            filtro = filtro.and(PartidoSpecs.conEstado(estado));
        }
        Sort recientes = Sort.by("fechaHora").descending().and(Sort.by("id").descending());
        return partidos.findAll(filtro, PageRequest.of(0, limite, recientes)).stream().map(PartidoDTO::de).toList();
    }
}
