package pe.edu.unu.apuestas.catalogo.exception;

import org.springframework.http.HttpStatus;

/** 404: el partido o el equipo pedido no existe. */
public class NoEncontradoException extends ApiException {

    public NoEncontradoException(String codigo, String detalle) {
        super(HttpStatus.NOT_FOUND, codigo, detalle);
    }
}
