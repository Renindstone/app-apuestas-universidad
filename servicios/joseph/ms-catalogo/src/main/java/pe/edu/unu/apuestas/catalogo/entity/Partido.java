package pe.edu.unu.apuestas.catalogo.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Tabla partido. Un cruce local–visitante ocurre una sola vez por temporada (RN-CAT-02): por eso la restricción
 * UNIQUE y por eso el alta es un upsert. La fecha y hora se guardan en UTC.
 */
@Entity
@Table(name = "partido",
        uniqueConstraints = @UniqueConstraint(name = "UQ_ParCruceTemporada",
                columnNames = { "ParIdEquipoLocal", "ParIdEquipoVisitante", "ParTemporada" }),
        indexes = @Index(name = "IX_ParFechaHora", columnList = "ParFechaHora"))
public class Partido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ParIdPartido")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ParIdLiga", nullable = false, foreignKey = @ForeignKey(name = "FK_ParIdLiga"))
    private Liga liga;

    // Temporada al estilo de football-data: "2627" es 2026-27.
    @Column(name = "ParTemporada", length = 4, nullable = false)
    private String temporada;

    @Column(name = "ParFechaHora", nullable = false)
    private Instant fechaHora;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ParIdEquipoLocal", nullable = false, foreignKey = @ForeignKey(name = "FK_ParIdEquipoLocal"))
    private Equipo local;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ParIdEquipoVisitante", nullable = false, foreignKey = @ForeignKey(name = "FK_ParIdEquipoVisitante"))
    private Equipo visitante;

    // Vacío cuando no se conoce: siempre en LaLiga y la Bundesliga, y en los partidos futuros (RN-CAT-05).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ParIdArbitro", foreignKey = @ForeignKey(name = "FK_ParIdArbitro"))
    private Arbitro arbitro;

    @Column(name = "ParGolesLocal")
    private Integer golesLocal;

    @Column(name = "ParGolesVisitante")
    private Integer golesVisitante;

    @Enumerated(EnumType.STRING)
    @Column(name = "ParEstado", length = 12, nullable = false)
    private EstadoPartido estado;

    protected Partido() {
        // Lo usa JPA.
    }

    // Un partido nuevo siempre nace PROGRAMADO.
    public Partido(Liga liga, String temporada, Equipo local, Equipo visitante) {
        this.liga = liga;
        this.temporada = temporada;
        this.local = local;
        this.visitante = visitante;
        this.estado = EstadoPartido.PROGRAMADO;
    }

    /**
     * Aplica los datos de una fila nueva de la ingesta. La fecha se actualiza siempre (puede reprogramarse);
     * el árbitro y los goles, solo si la fila los trae. Con goles pasa a FINALIZADO, y un partido FINALIZADO
     * nunca vuelve a PROGRAMADO, aunque después llegue una fila sin goles (RN-CAT-04).
     */
    public void actualizar(Instant fechaHora, Arbitro arbitro, Integer golesLocal, Integer golesVisitante) {
        this.fechaHora = fechaHora;
        if (arbitro != null) {
            this.arbitro = arbitro;
        }
        if (golesLocal != null && golesVisitante != null) {
            this.golesLocal = golesLocal;
            this.golesVisitante = golesVisitante;
            this.estado = EstadoPartido.FINALIZADO;
        }
    }

    public Long getId() {
        return id;
    }

    public Liga getLiga() {
        return liga;
    }

    public String getTemporada() {
        return temporada;
    }

    public Instant getFechaHora() {
        return fechaHora;
    }

    public Equipo getLocal() {
        return local;
    }

    public Equipo getVisitante() {
        return visitante;
    }

    public Arbitro getArbitro() {
        return arbitro;
    }

    public Integer getGolesLocal() {
        return golesLocal;
    }

    public Integer getGolesVisitante() {
        return golesVisitante;
    }

    public EstadoPartido getEstado() {
        return estado;
    }
}
