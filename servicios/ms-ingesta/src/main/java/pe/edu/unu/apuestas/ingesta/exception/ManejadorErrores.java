package pe.edu.unu.apuestas.ingesta.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import pe.edu.unu.apuestas.ingesta.dto.ErrorDTO;

/**
 * Convierte cualquier error en el JSON común {"error", "detalle"} con el código HTTP correcto.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    // Errores de negocio: 400 (límite fuera de rango) o 409 (ya hay una ingesta en curso).
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorDTO> deNegocio(ApiException e) {
        return responder(e.getEstado(), e.getCodigo(), e.getMessage());
    }

    // 400: tipo distinto de HISTORICO o PROXIMOS, o un límite que no es un número.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDTO> parametro(MethodArgumentTypeMismatchException e) {
        return responder(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO",
                "El parámetro «" + e.getName() + "» no tiene un valor válido. El tipo es HISTORICO o PROXIMOS.");
    }

    // 400: falta ?tipo=.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorDTO> faltaParametro(MissingServletRequestParameterException e) {
        return responder(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO", "Falta el parámetro «" + e.getParameterName() + "».");
    }

    // 500: error no previsto. Se registra en el log y al cliente no se le muestran detalles técnicos.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> inesperado(Exception e) {
        log.error("Error no controlado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Ocurrió un error inesperado en ms-ingesta.");
    }

    private static ResponseEntity<ErrorDTO> responder(HttpStatus estado, String codigo, String detalle) {
        return ResponseEntity.status(estado).body(new ErrorDTO(codigo, detalle));
    }
}
