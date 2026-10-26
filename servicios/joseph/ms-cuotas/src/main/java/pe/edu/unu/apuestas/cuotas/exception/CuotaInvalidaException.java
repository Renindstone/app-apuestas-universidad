package pe.edu.unu.apuestas.cuotas.exception;

import org.springframework.http.HttpStatus;

/** 400: una cuota que no es mayor que 1, o que no es mayor que 1.01 si la ingresa el usuario (RN-CUO-01, CB-04). */
public class CuotaInvalidaException extends ApiException {

    public CuotaInvalidaException(String detalle) {
        super(HttpStatus.BAD_REQUEST, "CUOTA_INVALIDA", detalle);
    }
}
