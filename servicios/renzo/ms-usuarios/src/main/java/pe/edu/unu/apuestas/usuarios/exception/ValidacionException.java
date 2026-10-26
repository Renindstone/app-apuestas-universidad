package pe.edu.unu.apuestas.usuarios.exception;

import org.springframework.http.HttpStatus;

/** 400: un dato no cumple una regla que no se puede expresar con anotaciones (por ejemplo, claves distintas). */
public class ValidacionException extends ApiException {

    public ValidacionException(String detalle) {
        super(HttpStatus.BAD_REQUEST, "VALIDACION", detalle);
    }
}
