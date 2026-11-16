package pe.edu.unu.apuestas.predicciones.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.ServletException;
import pe.edu.unu.apuestas.predicciones.dto.ErrorDTO;

/**
 * Convierte cualquier error en el JSON común {"error", "detalle"} con el código HTTP correcto.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    // Errores de negocio: 404, 409 o 503 según la excepción.
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorDTO> deNegocio(ApiException e) {
        return responder(e.getEstado(), e.getCodigo(), e.getMessage());
    }

    // 400: un parámetro con formato equivocado, por ejemplo /partidos/abc.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDTO> parametro(MethodArgumentTypeMismatchException e) {
        return responder(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO", "El parámetro «" + e.getName() + "» no tiene un valor válido.");
    }

    // 404, 405, 415…: errores de Spring MVC que ya traen su código (ruta que no existe, método equivocado…).
    // Sin esto caerían en el manejador de Exception y responderían 500.
    @ExceptionHandler(ServletException.class)
    public ResponseEntity<ErrorDTO> deSpring(ServletException e) {
        HttpStatusCode estado = e instanceof ErrorResponse respuesta ? respuesta.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(estado).body(new ErrorDTO("PETICION_INVALIDA", e.getMessage()));
    }

    // 500: error no previsto. Se registra en el log y al cliente no se le muestran detalles técnicos.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> inesperado(Exception e) {
        log.error("Error no controlado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Ocurrió un error inesperado en ms-predicciones.");
    }

    private static ResponseEntity<ErrorDTO> responder(HttpStatus estado, String codigo, String detalle) {
        return ResponseEntity.status(estado).body(new ErrorDTO(codigo, detalle));
    }
}
