package com.cdg.notificationservice.common;

import com.cdg.notificationservice.notification.NotificationException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class Errors {
    public record ApiError(Instant timestamp, int status, String code, String message,
            Map<String, String> fieldErrors, String correlationId) {}
    public static ApiError error(int status, String code, String message, Map<String, String> fields, HttpServletRequest request) {
        return new ApiError(Instant.now(), status, code, message, fields,
                (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE));
    }
    @ExceptionHandler(NotificationException.class) ResponseEntity<ApiError> notification(NotificationException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.status()).body(error(exception.status().value(),
                exception.code(), exception.getMessage(), Map.of(), request));
    }
    @ExceptionHandler(AccessDeniedException.class) ResponseEntity<ApiError> forbidden(HttpServletRequest request) {
        return ResponseEntity.status(403).body(error(403, "FORBIDDEN", "Access denied", Map.of(), request));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<ApiError> malformed(HttpServletRequest request) {
        return ResponseEntity.badRequest().body(error(400, "MALFORMED_REQUEST", "Malformed request body", Map.of(), request));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class) ResponseEntity<ApiError> invalidParameter(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(error(400, "VALIDATION_ERROR", "Invalid request parameter",
                Map.of(exception.getName(), "Invalid value"), request));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ApiError> invalid(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(field -> fields.put(field.getField(), field.getDefaultMessage()));
        return ResponseEntity.badRequest().body(error(400, "VALIDATION_ERROR", "Request validation failed", fields, request));
    }
}
