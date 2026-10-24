package pe.edu.unu.apuestas.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Tabla equipo. Se crea sola la primera vez que llega en una fila de la ingesta; su nombre es único (RN-CAT-03).
 */
@Entity
@Table(name = "equipo", uniqueConstraints = @UniqueConstraint(name = "UQ_EquNombre", columnNames = "EquNombre"))
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EquIdEquipo")
    private Long id;

    @Column(name = "EquNombre", length = 60, nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "EquIdLiga", nullable = false, foreignKey = @ForeignKey(name = "FK_EquIdLiga"))
    private Liga liga;

    protected Equipo() {
        // Lo usa JPA.
    }

    public Equipo(String nombre, Liga liga) {
        this.nombre = nombre;
        this.liga = liga;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Liga getLiga() {
        return liga;
    }
}
