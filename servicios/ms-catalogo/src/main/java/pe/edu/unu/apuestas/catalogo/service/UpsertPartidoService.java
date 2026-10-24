package pe.edu.unu.apuestas.catalogo.service;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unu.apuestas.catalogo.dto.PartidoDTO;
import pe.edu.unu.apuestas.catalogo.dto.PartidoUpsertRequest;
import pe.edu.unu.apuestas.catalogo.entity.Arbitro;
import pe.edu.unu.apuestas.catalogo.entity.Equipo;
import pe.edu.unu.apuestas.catalogo.entity.Liga;
import pe.edu.unu.apuestas.catalogo.entity.Partido;
import pe.edu.unu.apuestas.catalogo.exception.ValidacionException;
import pe.edu.unu.apuestas.catalogo.repository.ArbitroRepository;
import pe.edu.unu.apuestas.catalogo.repository.EquipoRepository;
import pe.edu.unu.apuestas.catalogo.repository.LigaRepository;
import pe.edu.unu.apuestas.catalogo.repository.PartidoRepository;

/**
 * Alta o actualización de un partido a partir de una fila de la ingesta (POST /interno/partidos).
 * Es idempotente: enviar la misma fila dos veces deja un solo partido (RN-CAT-02).
 */
@Service
public class UpsertPartidoService {

    private final LigaRepository ligas;
    private final EquipoRepository equipos;
    private final ArbitroRepository arbitros;
    private final PartidoRepository partidos;

    public UpsertPartidoService(LigaRepository ligas, EquipoRepository equipos, ArbitroRepository arbitros, PartidoRepository partidos) {
        this.ligas = ligas;
        this.equipos = equipos;
        this.arbitros = arbitros;
        this.partidos = partidos;
    }

    // Busca el cruce (local, visitante, temporada): si existe lo actualiza y si no lo crea. Devuelve el PartidoDTO con todos los IDs.
    @Transactional
    public PartidoDTO upsert(PartidoUpsertRequest fila) {
        if ((fila.golesLocal() == null) != (fila.golesVisitante() == null)) {
            throw new ValidacionException("Llegan los dos goles o ninguno.");
        }
        String nombreLocal = fila.local().trim();
        String nombreVisitante = fila.visitante().trim();
        if (nombreLocal.equalsIgnoreCase(nombreVisitante)) {
            throw new ValidacionException("El local y el visitante no pueden ser el mismo equipo.");
        }
        String codigo = fila.ligaCodigo().trim().toUpperCase(Locale.ROOT);
        Liga liga = ligas.findByCodigo(codigo)
                .orElseThrow(() -> new ValidacionException("La liga " + codigo + " no existe. Solo hay E0, SP1 y D1."));

        Equipo local = equipo(nombreLocal, liga);
        Equipo visitante = equipo(nombreVisitante, liga);
        Arbitro arbitro = fila.arbitro() == null || fila.arbitro().isBlank() ? null : arbitro(fila.arbitro().trim());

        Partido partido = partidos.findByLocalAndVisitanteAndTemporada(local, visitante, fila.temporada())
                .orElseGet(() -> new Partido(liga, fila.temporada(), local, visitante));
        partido.actualizar(fila.fechaHora(), arbitro, fila.golesLocal(), fila.golesVisitante());
        return PartidoDTO.de(partidos.save(partido));
    }

    // El equipo con ese nombre; si es la primera vez que aparece, se crea en la liga de la fila (RN-CAT-03).
    private Equipo equipo(String nombre, Liga liga) {
        return equipos.findByNombre(nombre).orElseGet(() -> equipos.save(new Equipo(nombre, liga)));
    }

    // El árbitro con ese nombre; si es la primera vez que aparece, se crea (RN-CAT-03).
    private Arbitro arbitro(String nombre) {
        return arbitros.findByNombre(nombre).orElseGet(() -> arbitros.save(new Arbitro(nombre)));
    }
}
