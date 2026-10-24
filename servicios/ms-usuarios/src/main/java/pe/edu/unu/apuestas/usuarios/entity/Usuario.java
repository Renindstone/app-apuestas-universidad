package pe.edu.unu.apuestas.usuarios.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Tabla usuario (sección 6.1 de la documentación). Las columnas llevan el prefijo Usu, como pide la cátedra.
 */
@Entity
// Las restricciones UNIQUE llevan nombre para reconocerlas en la base de datos (RN-USU-01).
@Table(name = "usuario", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_UsuNombreUsuario", columnNames = "UsuNombreUsuario"),
        @UniqueConstraint(name = "UQ_UsuEmail", columnNames = "UsuEmail") })
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UsuIdUsuario")
    private Long id;

    @Column(name = "UsuNombreUsuario", length = 30, nullable = false)
    private String nombreUsuario;

    // Se guarda siempre en minúsculas (RN-USU-01).
    @Column(name = "UsuEmail", length = 100, nullable = false)
    private String email;

    // Hash BCrypt de la contraseña; la contraseña nunca se guarda (RN-USU-02).
    @Column(name = "UsuClaveHash", length = 100, nullable = false)
    private String claveHash;

    // Hibernate crea el CHECK (UsuRol IN ('ADMIN', 'APOSTADOR')) a partir del enum.
    @Enumerated(EnumType.STRING)
    @Column(name = "UsuRol", length = 15, nullable = false)
    private Rol rol;

    // Monto que declara el usuario; el sistema no mueve dinero real (RN-USU-06).
    @ColumnDefault("0")
    @Column(name = "UsuBanca", precision = 12, scale = 2, nullable = false)
    private BigDecimal banca = BigDecimal.ZERO;

    @Column(name = "UsuFechaRegistro", nullable = false, updatable = false)
    private Instant fechaRegistro;

    protected Usuario() {
        // Lo usa JPA.
    }

    public Usuario(String nombreUsuario, String email, String claveHash, Rol rol) {
        this.nombreUsuario = nombreUsuario;
        this.email = email;
        this.claveHash = claveHash;
        this.rol = rol;
    }

    // La fecha de registro se pone sola al guardar por primera vez.
    @PrePersist
    void alCrear() {
        if (fechaRegistro == null) {
            fechaRegistro = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getEmail() {
        return email;
    }

    public String getClaveHash() {
        return claveHash;
    }

    public Rol getRol() {
        return rol;
    }

    public BigDecimal getBanca() {
        return banca;
    }

    public void setBanca(BigDecimal banca) {
        this.banca = banca;
    }

    public Instant getFechaRegistro() {
        return fechaRegistro;
    }
}
