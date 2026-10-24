package pe.edu.unu.apuestas.usuarios.exception;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio con su código HTTP y su código corto. ManejadorErrores lo convierte en {"error", "detalle"}.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus estado;
    private final String codigo;

    public ApiException(HttpStatus estado, String codigo, String detalle) {
        super(detalle);
        this.estado = estado;
        this.codigo = codigo;
    }

    public HttpStatus getEstado() {
        return estado;
    }

    public String getCodigo() {
        return codigo;
    }
}
