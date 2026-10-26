package pe.edu.unu.apuestas.catalogo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unu.apuestas.catalogo.entity.Liga;

/** Acceso a la tabla liga. */
public interface LigaRepository extends JpaRepository<Liga, Long> {

    Optional<Liga> findByCodigo(String codigo);
}
