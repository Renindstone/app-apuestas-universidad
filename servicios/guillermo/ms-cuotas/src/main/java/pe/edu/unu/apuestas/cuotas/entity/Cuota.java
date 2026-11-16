package pe.edu.unu.apuestas.cuotas.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.CheckConstraint;
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
 * Tabla cuota: lo que paga una casa por una selección de un mercado de un partido. partidoId es el ID de ms-catalogo:
 * no hay clave foránea porque esa base es de otro servicio. La misma cuota se actualiza en lugar de duplicarse (RN-CUO-04).
 */
@Entity
@Table(name = "cuota",
        indexes = @Index(name = "IX_CuoIdPartido", columnList = "CuoIdPartido"),
        // RN-CUO-01: toda cuota es mayor que 1. El nombre va entre comillas porque PostgreSQL lo guarda con mayúsculas.
        check = @CheckConstraint(name = "CK_CuoValor", constraint = "\"CuoValor\" > 1"))
public class Cuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CuoIdCuota")
    private Long id;

    @Column(name = "CuoIdPartido", nullable = false)
    private Long partidoId;

    // bet365, pinnacle o betano, en minúsculas (RN-10).
    @Column(name = "CuoCasa", length = 10, nullable = false)
    private String casa;

    // 1X2, GOLES, AMBOS_MARCAN, TARJETAS o CORNERS (RN-09).
    @Column(name = "CuoMercado", length = 15, nullable = false)
    private String mercado;

    @Column(name = "CuoSeleccion", length = 10, nullable = false)
    private String seleccion;

    // Vacía en los mercados sin línea (1X2 y AMBOS_MARCAN).
    @Column(name = "CuoLinea", precision = 4, scale = 2)
    private BigDecimal linea;

    @Column(name = "CuoValor", precision = 6, scale = 2, nullable = false)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "CuoTipo", length = 10, nullable = false)
    private TipoCuota tipo;

    // Quién la cargó, solo en las MANUAL (RN-CUO-02).
    @Column(name = "CuoIdUsuario")
    private Long usuarioId;

    @Column(name = "CuoCapturadaEn", nullable = false)
    private Instant capturadaEn;

    protected Cuota() {
        // Lo usa JPA.
    }

    // Una cuota nueva: el valor llega con actualizar().
    public Cuota(Long partidoId, String casa, TipoCuota tipo, String mercado, String seleccion, BigDecimal linea) {
        this.partidoId = partidoId;
        this.casa = casa;
        this.tipo = tipo;
        this.mercado = mercado;
        this.seleccion = seleccion;
        this.linea = linea;
    }

    // Guarda el valor recibido. capturadaEn pasa a ser este momento: así la consulta sabe cuál es la más reciente.
    public void actualizar(BigDecimal valor, Long usuarioId) {
        this.valor = valor;
        this.usuarioId = usuarioId;
        this.capturadaEn = Instant.now();
    }

    // Casa, mercado, selección y línea: la consulta muestra una sola cuota por clave (RN-CUO-04). 2.5 y 2.50 son la misma línea.
    public String clave() {
        return casa + "|" + mercado + "|" + seleccion + "|" + (linea == null ? "" : linea.stripTrailingZeros().toPlainString());
    }

    public String getCasa() {
        return casa;
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

    public BigDecimal getValor() {
        return valor;
    }

    public TipoCuota getTipo() {
        return tipo;
    }

    public Instant getCapturadaEn() {
        return capturadaEn;
    }
}
