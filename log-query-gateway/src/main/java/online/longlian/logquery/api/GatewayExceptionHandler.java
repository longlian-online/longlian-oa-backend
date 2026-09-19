package online.longlian.logquery.api;

import java.util.LinkedHashMap;
import java.util.Map;

import online.longlian.logquery.service.LogQueryException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GatewayExceptionHandler {

    @ExceptionHandler(LogQueryException.class)
    public ResponseEntity<Map<String, Object>> handleLogQueryException(LogQueryException exception) {
        return response(exception.getStatus(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("invalid request");
        return response(HttpStatus.BAD_REQUEST.value(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return response(HttpStatus.BAD_REQUEST.value(), "request body is invalid");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR.value(), "internal server error");
    }

    private static ResponseEntity<Map<String, Object>> response(int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", status);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
