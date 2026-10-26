package pe.edu.unu.apuestas.usuarios.exception;

import org.springframework.http.HttpStatus;

/** 409: el nombre de usuario o el correo ya existen (RN-USU-01). */
public class UsuarioDuplicadoException extends ApiException {

    public UsuarioDuplicadoException(String detalle) {
        super(HttpStatus.CONFLICT, "USUARIO_DUPLICADO", detalle);
    }
}
