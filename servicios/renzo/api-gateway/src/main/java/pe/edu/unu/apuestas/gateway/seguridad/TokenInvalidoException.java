package pe.edu.unu.apuestas.gateway.seguridad;

/**
 * El token falta, está mal firmado o venció. El filtro responde 401 con este mensaje.
 */
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException(String mensaje) {
        super(mensaje);
    }
}
