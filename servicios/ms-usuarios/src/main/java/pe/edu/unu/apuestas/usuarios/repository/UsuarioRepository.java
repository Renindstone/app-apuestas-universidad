package pe.edu.unu.apuestas.usuarios.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.unu.apuestas.usuarios.entity.Usuario;

/** Acceso a la tabla usuario. Spring Data arma las consultas a partir del nombre de cada método. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByEmail(String email);
}
