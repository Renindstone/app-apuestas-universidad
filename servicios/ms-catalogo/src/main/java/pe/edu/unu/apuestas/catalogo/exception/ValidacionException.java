package pe.edu.unu.apuestas.catalogo.exception;

import org.springframework.http.HttpStatus;

/** 400: un dato no cumple una regla del catálogo (liga inexistente, goles incompletos, rango de fechas al revés…). */
public class ValidacionException extends ApiException {

    public ValidacionException(String detalle) {
        super(HttpStatus.BAD_REQUEST, "VALIDACION", detalle);
    }
}
