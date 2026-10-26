package pe.edu.unu.apuestas.estadisticas.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import pe.edu.unu.apuestas.estadisticas.dto.EstadisticasRequest;

/**
 * Tabla estadistica_partido: lo que hizo un equipo en un partido. Cada partido son dos filas, una por equipo, y en
 * cada una el rival es el otro (RN-EST-01). Así los promedios a favor salen de EpaIdEquipo y los en contra de
 * EpaIdRival, con un AVG y sin joins. Los IDs son los de ms-catalogo: no hay claves foráneas porque esa base es de otro servicio.
 */
@Entity
@Table(name = "estadistica_partido",
        uniqueConstraints = @UniqueConstraint(name = "UQ_EpaPartidoEquipo", columnNames = { "EpaIdPartido", "EpaIdEquipo" }),
        indexes = {
                @Index(name = "IX_EpaIdEquipo", columnList = "EpaIdEquipo"),
                @Index(name = "IX_EpaIdRival", columnList = "EpaIdRival"),
                @Index(name = "IX_EpaIdArbitro", columnList = "EpaIdArbitro") })
public class EstadisticaPartido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EpaIdEstadistica")
    private Long id;

    @Column(name = "EpaIdPartido", nullable = false)
    private Long partidoId;

    @Column(name = "EpaIdLiga", nullable = false)
    private Long ligaId;

    // Temporada al estilo de football-data: "2627" es 2026-27.
    @Column(name = "EpaTemporada", length = 4, nullable = false)
    private String temporada;

    // Fecha y hora del partido en UTC. Ordena los "últimos N partidos".
    @Column(name = "EpaFecha", nullable = false)
    private Instant fecha;

    @Column(name = "EpaIdEquipo", nullable = false)
    private Long equipoId;

    @Column(name = "EpaIdRival", nullable = false)
    private Long rivalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "EpaCondicion", length = 10, nullable = false)
    private Condicion condicion;

    // Vacío si no se conoce: football-data solo trae el árbitro en la Premier.
    @Column(name = "EpaIdArbitro")
    private Long arbitroId;

    @Column(name = "EpaGoles", nullable = false)
    private int goles;

    @Column(name = "EpaTiros", nullable = false)
    private int tiros;

    @Column(name = "EpaTirosPuerta", nullable = false)
    private int tirosPuerta;

    @Column(name = "EpaCorners", nullable = false)
    private int corners;

    @Column(name = "EpaFaltas", nullable = false)
    private int faltas;

    @Column(name = "EpaAmarillas", nullable = false)
    private int amarillas;

    @Column(name = "EpaRojas", nullable = false)
    private int rojas;

    protected EstadisticaPartido() {
        // Lo usa JPA.
    }

    // La fila de un equipo en un partido. El resto de los datos llega con actualizar().
    public EstadisticaPartido(Long partidoId, Long equipoId) {
        this.partidoId = partidoId;
        this.equipoId = equipoId;
    }

    // Copia los datos del partido y los del lado de este equipo. Sirve para crear la fila y para actualizarla (RN-EST-01).
    public void actualizar(EstadisticasRequest partido, EstadisticasRequest.Lado propio, Long rivalId, Condicion condicion) {
        this.ligaId = partido.ligaId();
        this.temporada = partido.temporada();
        this.fecha = partido.fecha();
        this.arbitroId = partido.arbitroId();
        this.rivalId = rivalId;
        this.condicion = condicion;
        this.goles = propio.goles();
        this.tiros = propio.tiros();
        this.tirosPuerta = propio.tirosPuerta();
        this.corners = propio.corners();
        this.faltas = propio.faltas();
        this.amarillas = propio.amarillas();
        this.rojas = propio.rojas();
    }

    // La fila en el mismo formato en que llegó (GET /partidos/{id}/estadisticas).
    public EstadisticasRequest.Lado lado() {
        return new EstadisticasRequest.Lado(equipoId, goles, tiros, tirosPuerta, corners, faltas, amarillas, rojas);
    }

    public Long getPartidoId() {
        return partidoId;
    }

    public Long getLigaId() {
        return ligaId;
    }

    public Condicion getCondicion() {
        return condicion;
    }
}
