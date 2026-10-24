package pe.edu.unu.apuestas.web.exception;

/**
 * Error 4xx del gateway con el formato común {"error": "CODIGO", "detalle": "mensaje"}.
 * El detalle está pensado para mostrarse tal cual en el formulario.
 */
public class ApiException extends RuntimeException {

    private final int estado;
    private final String codigo;

    public ApiException(int estado, String codigo, String detalle) {
        super(detalle);
        this.estado = estado;
        this.codigo = codigo;
    }

    public int getEstado() {
        return estado;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDetalle() {
        return getMessage();
    }
}
