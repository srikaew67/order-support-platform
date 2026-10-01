package com.cdg.ordersupport.common;
import java.time.Instant; import java.util.*; import org.springframework.http.*; import org.springframework.web.bind.*; import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class GlobalExceptionHandler {
 @ExceptionHandler(ApiException.class) ResponseEntity<ApiError> api(ApiException e){return ResponseEntity.status(e.status()).body(new ApiError(Instant.now(),e.status().value(),e.code(),e.getMessage(),Map.of()));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ApiError> validation(MethodArgumentNotValidException e){Map<String,String> fields=new HashMap<>();e.getBindingResult().getFieldErrors().forEach(f->fields.put(f.getField(),f.getDefaultMessage()));return ResponseEntity.badRequest().body(new ApiError(Instant.now(),400,"VALIDATION_ERROR","Request validation failed",fields));}
}
