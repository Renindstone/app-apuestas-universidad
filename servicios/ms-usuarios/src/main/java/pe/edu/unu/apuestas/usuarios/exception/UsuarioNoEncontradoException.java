package pe.edu.unu.apuestas.usuarios.exception;

import org.springframework.http.HttpStatus;

/** 404: el usuario del token ya no existe. */
public class UsuarioNoEncontradoException extends ApiException {

    public UsuarioNoEncontradoException(Long id) {
        super(HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO", "No existe el usuario " + id);
    }
}
