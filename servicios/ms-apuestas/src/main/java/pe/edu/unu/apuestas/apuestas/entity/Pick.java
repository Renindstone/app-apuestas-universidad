package pe.edu.unu.apuestas.apuestas.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Tabla pick: una candidata a apuesta del día. Se guardan todas las de una fecha, y solo la de mayor valor esperado
 * lleva PicEsApuestaDelDia = true (RN-APU-03). partidoId es el ID de ms-catalogo, sin clave foránea.
 */
@Entity
@Table(name = "pick", indexes = @Index(name = "IX_PicFecha", columnList = "PicFecha"))
public class Pick implements Apostable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PicIdPick")
    private Long id;

    // Día de Lima para el que se generó.
    @Column(name = "PicFecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "PicIdPartido", nullable = false)
    private Long partidoId;

    // "Arsenal vs Chelsea", para el historial sin consultar a ms-catalogo.
    @Column(name = "PicDescripcion", length = 130, nullable = false)
    private String descripcion;

    @Column(name = "PicMercado", length = 15, nullable = false)
    private String mercado;

    @Column(name = "PicSeleccion", length = 10, nullable = false)
    private String seleccion;

    @Column(name = "PicLinea", precision = 4, scale = 2)
    private BigDecimal linea;

    @Column(name = "PicCasa", length = 10, nullable = false)
    private String casa;

    @Column(name = "PicCuota", precision = 6, scale = 2, nullable = false)
    private BigDecimal cuota;

    // Probabilidad del modelo, valor esperado y stake sugerido en el momento de elegirla.
    @Column(name = "PicProbabilidad", nullable = false)
    private double probabilidad;

    @Column(name = "PicValorEsperado", nullable = false)
    private double valorEsperado;

    @Column(name = "PicStakePct", nullable = false)
    private double stakePct;

    @Column(name = "PicEsApuestaDelDia", nullable = false)
    private boolean esApuestaDelDia;

    @Enumerated(EnumType.STRING)
    @Column(name = "PicEstado", length = 10, nullable = false)
    private EstadoPick estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "PicResultado", length = 10)
    private Resultado resultado;

    protected Pick() {
        // Lo usa JPA.
    }

    // Una candidata nueva: nace ABIERTA y sin marcar como apuesta del día.
    public Pick(LocalDate fecha, Long partidoId, String descripcion, String mercado, String seleccion, BigDecimal linea, String casa,
            BigDecimal cuota, double probabilidad, double valorEsperado, double stakePct) {
        this.fecha = fecha;
        this.partidoId = partidoId;
        this.descripcion = descripcion;
        this.mercado = mercado;
        this.seleccion = seleccion;
        this.linea = linea;
        this.casa = casa;
        this.cuota = cuota;
        this.probabilidad = probabilidad;
        this.valorEsperado = valorEsperado;
        this.stakePct = stakePct;
        this.estado = EstadoPick.ABIERTO;
    }

    public void marcarComoApuestaDelDia() {
        this.esApuestaDelDia = true;
    }

    // El partido terminó: queda LIQUIDADO con su resultado.
    public void liquidar(Resultado resultado) {
        this.resultado = resultado;
        this.estado = EstadoPick.LIQUIDADO;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public Long getPartidoId() {
        return partidoId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getMercado() {
        return mercado;
    }

    public String getSeleccion() {
        return seleccion;
    }

    public BigDecimal getLinea() {
        return linea;
    }

    public String getCasa() {
        return casa;
    }

    public BigDecimal getCuota() {
        return cuota;
    }

    public double getProbabilidad() {
        return probabilidad;
    }

    public double getValorEsperado() {
        return valorEsperado;
    }

    public double getStakePct() {
        return stakePct;
    }

    public boolean isEsApuestaDelDia() {
        return esApuestaDelDia;
    }

    public EstadoPick getEstado() {
        return estado;
    }

    public Resultado getResultado() {
        return resultado;
    }
}
