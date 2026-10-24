package pe.edu.unu.apuestas.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Tabla arbitro. Solo llega en la Premier League; se crea sola la primera vez que aparece (RN-CAT-03).
 */
@Entity
@Table(name = "arbitro", uniqueConstraints = @UniqueConstraint(name = "UQ_ArbNombre", columnNames = "ArbNombre"))
public class Arbitro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ArbIdArbitro")
    private Long id;

    @Column(name = "ArbNombre", length = 60, nullable = false)
    private String nombre;

    protected Arbitro() {
        // Lo usa JPA.
    }

    public Arbitro(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
