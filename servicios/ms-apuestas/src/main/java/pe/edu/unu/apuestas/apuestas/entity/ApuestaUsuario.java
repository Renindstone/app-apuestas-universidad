package pe.edu.unu.apuestas.apuestas.entity;

import java.math.BigDecimal;
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

/**
 * Tabla apuesta_usuario: una apuesta que el usuario hizo por su cuenta y registró para medir su rendimiento.
 * El sistema no mueve dinero (RN-08). resultado y ganancia quedan en null hasta que se liquida.
 */
@Entity
@Table(name = "apuesta_usuario", indexes = @Index(name = "IX_ApuIdUsuario", columnList = "ApuIdUsuario"))
public class ApuestaUsuario implements Apostable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ApuIdApuesta")
    private Long id;

    // El usuario del token (X-Usuario-Id), nunca un dato del JSON (RN-14).
    @Column(name = "ApuIdUsuario", nullable = false)
    private Long usuarioId;

    @Column(name = "ApuIdPartido", nullable = false)
    private Long partidoId;

    // "Arsenal vs Chelsea", para el historial sin consultar a ms-catalogo (RN-APU-05).
    @Column(name = "ApuDescripcion", length = 130, nullable = false)
    private String descripcion;

    @Column(name = "ApuMercado", length = 15, nullable = false)
    private String mercado;

    @Column(name = "ApuSeleccion", length = 10, nullable = false)
    private String seleccion;

    @Column(name = "ApuLinea", precision = 4, scale = 2)
    private BigDecimal linea;

    @Column(name = "ApuCasa", length = 10, nullable = false)
    private String casa;

    @Column(name = "ApuCuota", precision = 6, scale = 2, nullable = false)
    private BigDecimal cuota;

    @Column(name = "ApuMonto", precision = 10, scale = 2, nullable = false)
    private BigDecimal monto;

    // Cuándo la registró.
    @Column(name = "ApuFecha", nullable = false)
    private Instant fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "ApuResultado", length = 10)
    private Resultado resultado;

    // monto × (cuota − 1) si ganó, −monto si perdió (RN-07).
    @Column(name = "ApuGanancia", precision = 10, scale = 2)
    private BigDecimal ganancia;

    protected ApuestaUsuario() {
        // Lo usa JPA.
    }

    // Una apuesta recién registrada: pendiente, sin resultado ni ganancia.
    public ApuestaUsuario(Long usuarioId, Long partidoId, String descripcion, String mercado, String seleccion, BigDecimal linea,
            String casa, BigDecimal cuota, BigDecimal monto) {
        this.usuarioId = usuarioId;
        this.partidoId = partidoId;
        this.descripcion = descripcion;
        this.mercado = mercado;
        this.seleccion = seleccion;
        this.linea = linea;
        this.casa = casa;
        this.cuota = cuota;
        this.monto = monto;
        this.fecha = Instant.now();
    }

    // El partido terminó: guarda el resultado y lo que ganó o perdió.
    public void liquidar(Resultado resultado, BigDecimal ganancia) {
        this.resultado = resultado;
        this.ganancia = ganancia;
    }

    public Long getId() {
        return id;
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

    public BigDecimal getMonto() {
        return monto;
    }

    public Instant getFecha() {
        return fecha;
    }

    public Resultado getResultado() {
        return resultado;
    }

    public BigDecimal getGanancia() {
        return ganancia;
    }
}
