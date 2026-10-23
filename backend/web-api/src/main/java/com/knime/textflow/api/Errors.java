package com.knime.textflow.api;
import com.knime.textflow.core.TransformException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class Errors {
    public record ApiError(String message, Integer step, String parameter) {}
    @ExceptionHandler(TransformException.class)
    ResponseEntity<ApiError> invalid(TransformException e) { return ResponseEntity.badRequest().body(new ApiError(e.getMessage(),e.getStep(),e.getParameter())); }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformed() { return ResponseEntity.badRequest().body(new ApiError("Malformed request. Expected input text and a pipeline array with parameter objects.",null,null)); }
}
