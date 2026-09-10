package ar.edu.utn.tup.g04.teoricosencuestas.comun.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ExcepcionDeNegocio.class)
    public ResponseEntity<ErrorResponse> negocio(ExcepcionDeNegocio e) {
        return ResponseEntity.status(e.estado())
                .body(new ErrorResponse(e.clave().name(), e.campo(), e.clave().texto()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e) {
        FieldError primero = e.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String campo = primero == null ? null : primero.getField();
        String mensaje = primero == null ? "Request invalido" : primero.getDefaultMessage();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("REQUEST_INVALIDO", campo, mensaje));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> ilegal(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("REQUEST_INVALIDO", null, e.getMessage()));
    }
}
