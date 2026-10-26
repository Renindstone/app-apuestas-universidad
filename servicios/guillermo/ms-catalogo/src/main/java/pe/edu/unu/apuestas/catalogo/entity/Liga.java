package pe.edu.unu.apuestas.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Tabla liga. Solo existen E0, SP1 y D1, y se insertan al arrancar (RN-CAT-01).
 */
@Entity
@Table(name = "liga", uniqueConstraints = @UniqueConstraint(name = "UQ_LigCodigo", columnNames = "LigCodigo"))
public class Liga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LigIdLiga")
    private Long id;

    // Código de football-data: E0, SP1 o D1.
    @Column(name = "LigCodigo", length = 5, nullable = false)
    private String codigo;

    @Column(name = "LigNombre", length = 50, nullable = false)
    private String nombre;

    @Column(name = "LigPais", length = 50, nullable = false)
    private String pais;

    protected Liga() {
        // Lo usa JPA.
    }

    public Liga(String codigo, String nombre, String pais) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.pais = pais;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPais() {
        return pais;
    }
}
