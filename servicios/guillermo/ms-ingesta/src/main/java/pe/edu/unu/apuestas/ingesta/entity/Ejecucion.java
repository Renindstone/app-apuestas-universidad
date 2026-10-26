package pe.edu.unu.apuestas.ingesta.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tabla ejecucion_ingesta: una fila por cada archivo descargado, con su tipo, fuente, inicio, fin, filas,
 * estado y error (RN-ING-06).
 */
@Entity
@Table(name = "ejecucion_ingesta")
public class Ejecucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EjeIdEjecucion")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "EjeTipo", length = 10, nullable = false)
    private TipoIngesta tipo;

    // URL del CSV, por ejemplo https://football-data.co.uk/mmz4281/2627/E0.csv
    @Column(name = "EjeFuente", length = 200, nullable = false)
    private String fuente;

    @Column(name = "EjeInicio", nullable = false)
    private Instant inicio;

    @Column(name = "EjeFin")
    private Instant fin;

    @Enumerated(EnumType.STRING)
    @Column(name = "EjeEstado", length = 10, nullable = false)
    private EstadoEjecucion estado;

    // Filas que pasaron por los tres servicios sin error.
    @Column(name = "EjeFilas", nullable = false)
    private int filas;

    @Column(name = "EjeError", columnDefinition = "text")
    private String error;

    protected Ejecucion() {
        // Lo usa JPA.
    }

    // Una ejecución nace EN_CURSO, en cuanto se lanza la ingesta.
    public Ejecucion(TipoIngesta tipo, String fuente) {
        this.tipo = tipo;
        this.fuente = fuente;
        this.inicio = Instant.now();
        this.estado = EstadoEjecucion.EN_CURSO;
    }

    // El archivo empieza a procesarse: el inicio pasa a ser este momento y no el de la cola.
    public void iniciar() {
        this.inicio = Instant.now();
    }

    // Termina en OK. error queda con el resumen de las filas que fallaron, o null si no falló ninguna.
    public void terminar(int filas, String error) {
        this.filas = filas;
        this.error = error;
        this.estado = EstadoEjecucion.OK;
        this.fin = Instant.now();
    }

    // Termina en ERROR: la fuente no respondió, el archivo no se pudo leer o el servicio se reinició a mitad.
    public void fallar(String error) {
        this.error = error;
        this.estado = EstadoEjecucion.ERROR;
        this.fin = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public TipoIngesta getTipo() {
        return tipo;
    }

    public String getFuente() {
        return fuente;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFin() {
        return fin;
    }

    public EstadoEjecucion getEstado() {
        return estado;
    }

    public int getFilas() {
        return filas;
    }

    public String getError() {
        return error;
    }
}
