package pe.edu.unu.apuestas.catalogo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unu.apuestas.catalogo.entity.Arbitro;

/** Acceso a la tabla arbitro. */
public interface ArbitroRepository extends JpaRepository<Arbitro, Long> {

    Optional<Arbitro> findByNombre(String nombre);
}
