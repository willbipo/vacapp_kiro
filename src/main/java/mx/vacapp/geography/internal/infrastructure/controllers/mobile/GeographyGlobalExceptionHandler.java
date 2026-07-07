package mx.vacapp.geography.internal.infrastructure.controllers.mobile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.domain.model.exceptions.CannotArchiveWithChildrenException;
import mx.vacapp.geography.internal.domain.model.exceptions.CattleAssignedException;
import mx.vacapp.geography.internal.domain.model.exceptions.DuplicateNameException;
import mx.vacapp.geography.internal.domain.model.exceptions.EntityNotFoundException;
import mx.vacapp.geography.internal.domain.model.exceptions.SurfaceExceededException;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para controladores REST.
 * 
 * Captura excepciones de dominio y errores de validación, retornando
 * respuestas HTTP semánticamente correctas con formato consistente.
 */
@RestControllerAdvice
@Slf4j
public class GeographyGlobalExceptionHandler {
    
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    
    /**
     * Maneja EntityNotFoundException (404 Not Found).
     * Se lanza cuando no se encuentra un recurso solicitado.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex, 
            HttpServletRequest request) {
        
        log.warn("Entity not found: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse error = new ErrorResponse(
            formatNow(),
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    /**
     * Maneja DuplicateNameException (409 Conflict).
     * Se lanza cuando se intenta crear un recurso con nombre duplicado.
     */
    @ExceptionHandler(DuplicateNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateName(
            DuplicateNameException ex, 
            HttpServletRequest request) {
        
        log.warn("Duplicate name conflict: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse error = new ErrorResponse(
            formatNow(),
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage(),
            request.getRequestURI(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    /**
     * Maneja SurfaceExceededException (400 Bad Request).
     * Se lanza cuando la superficie de una entidad excede el límite permitido.
     */
    @ExceptionHandler(SurfaceExceededException.class)
    public ResponseEntity<ErrorResponse> handleSurfaceExceeded(
            SurfaceExceededException ex, 
            HttpServletRequest request) {
        
        log.warn("Surface exceeded: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse error = new ErrorResponse(
            formatNow(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Maneja CannotArchiveWithChildrenException (400 Bad Request).
     * Se lanza cuando se intenta archivar una entidad que tiene hijos activos.
     */
    @ExceptionHandler(CannotArchiveWithChildrenException.class)
    public ResponseEntity<ErrorResponse> handleCannotArchiveWithChildren(
            CannotArchiveWithChildrenException ex, 
            HttpServletRequest request) {
        
        log.warn("Cannot archive with children: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse error = new ErrorResponse(
            formatNow(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Maneja CattleAssignedException (400 Bad Request).
     * Se lanza cuando se intenta archivar un potrero que tiene ganado asignado.
     */
    @ExceptionHandler(CattleAssignedException.class)
    public ResponseEntity<ErrorResponse> handleCattleAssigned(
            CattleAssignedException ex, 
            HttpServletRequest request) {
        
        log.warn("Cattle assigned: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse error = new ErrorResponse(
            formatNow(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Maneja MethodArgumentNotValidException (400 Bad Request).
     * Se lanza cuando falla la validación Bean Validation en @Valid.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, 
            HttpServletRequest request) {
        
        log.warn("Validation failed: {} errors - Path: {}", 
                ex.getBindingResult().getErrorCount(), 
                request.getRequestURI());
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.FieldError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .collect(Collectors.toList());
        
        ErrorResponse error = new ErrorResponse(
            formatNow(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Error de validación en los datos de entrada",
            request.getRequestURI(),
            fieldErrors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Maneja ConstraintViolationException (400 Bad Request).
     * Se lanza cuando falla la validación de restricciones de Bean Validation.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, 
            HttpServletRequest request) {
        
        log.warn("Constraint violation: {} violations - Path: {}", 
                ex.getConstraintViolations().size(), 
                request.getRequestURI());
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> new ErrorResponse.FieldError(
                        extractFieldName(violation),
                        violation.getMessage()
                ))
                .collect(Collectors.toList());
        
        ErrorResponse error = new ErrorResponse(
            formatNow(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Error de validación en restricciones",
            request.getRequestURI(),
            fieldErrors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Maneja excepciones genéricas no capturadas (500 Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, 
            HttpServletRequest request) {
        
        log.error("Unexpected error: {} - Path: {}", ex.getMessage(), request.getRequestURI(), ex);
        
        ErrorResponse error = new ErrorResponse(
            formatNow(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "Ha ocurrido un error inesperado. Por favor contacte al administrador.",
            request.getRequestURI(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    /**
     * Formatea el timestamp actual como ISO 8601 UTC.
     */
    private String formatNow() {
        return Instant.now().atOffset(ZoneOffset.UTC).format(ISO_FORMATTER);
    }
    
    /**
     * Extrae el nombre del campo de una violación de restricción.
     */
    private String extractFieldName(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        String[] parts = propertyPath.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : propertyPath;
    }
}
