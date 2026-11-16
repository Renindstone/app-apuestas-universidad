package pe.edu.unu.apuestas.catalogo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import pe.edu.unu.apuestas.catalogo.dto.ErrorDTO;

/**
 * Convierte cualquier error en el JSON común {"error", "detalle"} con el código HTTP correcto.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    // Errores de negocio: 400 o 404 según la excepción.
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorDTO> deNegocio(ApiException e) {
        return responder(e.getEstado(), e.getCodigo(), e.getMessage());
    }

    // 400: falla una anotación de validación del cuerpo. Se devuelve el primer mensaje.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> validacion(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage()).findFirst().orElse("Hay un campo no válido.");
        return responder(HttpStatus.BAD_REQUEST, "VALIDACION", detalle);
    }

    // 400: un parámetro con formato equivocado, por ejemplo fecha=24-10-2026 o estado=JUGANDO.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDTO> parametro(MethodArgumentTypeMismatchException e) {
        return responder(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO",
                "El parámetro «" + e.getName() + "» no tiene un valor válido. Las fechas van como 2026-10-24.");
    }

    // 400: el cuerpo no es un JSON válido.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDTO> formato(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST, "FORMATO_INVALIDO", "El cuerpo de la petición no es un JSON válido.");
    }

    // 409: dos altas simultáneas del mismo equipo, árbitro o cruce; la restricción UNIQUE las frena.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDTO> duplicado(DataIntegrityViolationException e) {
        return responder(HttpStatus.CONFLICT, "DUPLICADO", "El registro ya existe. Vuelve a enviar la fila.");
    }

    // 500: error no previsto. Se registra en el log y al cliente no se le muestran detalles técnicos.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> inesperado(Exception e) {
        log.error("Error no controlado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Ocurrió un error inesperado en ms-catalogo.");
    }

    private static ResponseEntity<ErrorDTO> responder(HttpStatus estado, String codigo, String detalle) {
        return ResponseEntity.status(estado).body(new ErrorDTO(codigo, detalle));
    }
}
