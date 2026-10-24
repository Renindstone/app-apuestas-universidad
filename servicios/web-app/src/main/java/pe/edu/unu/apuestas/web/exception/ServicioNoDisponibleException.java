package pe.edu.unu.apuestas.web.exception;

/**
 * El gateway o un servicio no respondió (5xx, conexión rechazada o tiempo agotado).
 * Cada pantalla la atrapa por sección para mostrar un aviso en lugar de un error (RN-WEB-04).
 */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
