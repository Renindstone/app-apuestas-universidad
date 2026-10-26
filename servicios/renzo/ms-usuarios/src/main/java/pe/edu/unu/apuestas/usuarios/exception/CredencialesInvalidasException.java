package pe.edu.unu.apuestas.usuarios.exception;

import org.springframework.http.HttpStatus;

/** 401 del login. El mensaje es el mismo si falla el usuario o la clave (RN-USU-04). */
public class CredencialesInvalidasException extends ApiException {

    public CredencialesInvalidasException() {
        super(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "Usuario o contraseña incorrectos");
    }
}
