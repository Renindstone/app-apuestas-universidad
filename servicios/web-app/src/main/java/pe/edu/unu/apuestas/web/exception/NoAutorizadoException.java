package pe.edu.unu.apuestas.web.exception;

/**
 * El gateway respondió 401: el token falta o venció. La sesión se borra y se vuelve a /login (RN-WEB-02).
 */
public class NoAutorizadoException extends RuntimeException {

    public NoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
