package pe.edu.unu.apuestas.catalogo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unu.apuestas.catalogo.entity.Equipo;

/** Acceso a la tabla equipo. */
public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    Optional<Equipo> findByNombre(String nombre);

    List<Equipo> findAllByOrderByNombreAsc();

    List<Equipo> findByLigaIdOrderByNombreAsc(Long ligaId);
}
