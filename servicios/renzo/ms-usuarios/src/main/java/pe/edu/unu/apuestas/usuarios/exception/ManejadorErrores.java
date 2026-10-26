package pe.edu.unu.apuestas.usuarios.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import pe.edu.unu.apuestas.usuarios.dto.ErrorDTO;

/**
 * Convierte cualquier error en el JSON común {"error", "detalle"} con el código HTTP correcto.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    // Errores de negocio: 400, 401, 404 o 409 según la excepción.
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorDTO> deNegocio(ApiException e) {
        return responder(e.getEstado(), e.getCodigo(), e.getMessage());
    }

    // 400: falla una anotación de validación (@NotBlank, @Size, @Email…). Se devuelve el primer mensaje.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> validacion(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage()).findFirst().orElse("Hay un campo no válido.");
        return responder(HttpStatus.BAD_REQUEST, "VALIDACION", detalle);
    }

    // 400: el cuerpo no es un JSON válido o un campo tiene el tipo equivocado.
    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ErrorDTO> formato(Exception e) {
        return responder(HttpStatus.BAD_REQUEST, "FORMATO_INVALIDO", "La petición tiene un formato no válido.");
    }

    // 401: falta X-Usuario-Id. Solo pasa si alguien llama al servicio sin pasar por el gateway.
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorDTO> sinUsuario(MissingRequestHeaderException e) {
        return responder(HttpStatus.UNAUTHORIZED, "NO_AUTORIZADO", "Falta la cabecera " + e.getHeaderName() + ": llama a través del gateway.");
    }

    // 409: dos registros simultáneos con el mismo usuario o correo; la restricción UNIQUE de la tabla lo frena.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDTO> duplicado(DataIntegrityViolationException e) {
        return responder(HttpStatus.CONFLICT, "USUARIO_DUPLICADO", "El nombre de usuario o el correo ya están en uso");
    }

    // 500: error no previsto. Se registra en el log y al cliente no se le muestran detalles técnicos.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> inesperado(Exception e) {
        log.error("Error no controlado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Ocurrió un error inesperado en ms-usuarios.");
    }

    private static ResponseEntity<ErrorDTO> responder(HttpStatus estado, String codigo, String detalle) {
        return ResponseEntity.status(estado).body(new ErrorDTO(codigo, detalle));
    }
}
