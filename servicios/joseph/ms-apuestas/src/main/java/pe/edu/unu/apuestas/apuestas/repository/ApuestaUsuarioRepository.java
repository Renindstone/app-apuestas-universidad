package pe.edu.unu.apuestas.apuestas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unu.apuestas.apuestas.entity.ApuestaUsuario;

/**
 * Acceso a la tabla apuesta_usuario. Todas las consultas de un usuario filtran por su ID (RN-14).
 */
public interface ApuestaUsuarioRepository extends JpaRepository<ApuestaUsuario, Long> {

    // Todas las apuestas del usuario, de la más reciente a la más antigua.
    List<ApuestaUsuario> findByUsuarioIdOrderByFechaDescIdDesc(Long usuarioId);

    // Solo las pendientes (sin resultado).
    List<ApuestaUsuario> findByUsuarioIdAndResultadoIsNullOrderByFechaDescIdDesc(Long usuarioId);

    // Solo las liquidadas.
    List<ApuestaUsuario> findByUsuarioIdAndResultadoIsNotNullOrderByFechaDescIdDesc(Long usuarioId);

    // Para la liquidación: las pendientes de todos los usuarios.
    List<ApuestaUsuario> findByResultadoIsNull();
}
