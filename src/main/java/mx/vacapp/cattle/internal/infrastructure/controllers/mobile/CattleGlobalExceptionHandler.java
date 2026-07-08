package mx.vacapp.cattle.internal.infrastructure.controllers.mobile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.model.exceptions.DuplicateAreteException;
import mx.vacapp.cattle.internal.domain.model.exceptions.InvalidGenealogyException;
import mx.vacapp.cattle.internal.domain.model.exceptions.InvalidPastureException;
import mx.vacapp.cattle.internal.domain.model.exceptions.SoldOrDeadAnimalException;
import mx.vacapp.cattle.internal.infrastructure.controllers.mobile.dtos.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para controladores REST del módulo cattle-inventory.
 * 
 * <p>Captura todas las excepciones de dominio y errores de validación del módulo
 * de inventario de ganado, mapeándolas a respuestas HTTP semánticamente correctas
 * con formato estandarizado.</p>
 * 
 * <p>Este handler implementa el patrón establecido en el proyecto para manejo
 * de errores consistente, retornando siempre objetos {@link ErrorResponse} con
 * información detallada del error.</p>
 * 
 * <p>Mapeo de excepciones a códigos HTTP:</p>
 * <ul>
 *   <li>400 Bad Request: InvalidPastureException, InvalidGenealogyException,
 *       SoldOrDeadAnimalException, validaciones Bean Validation</li>
 *   <li>404 Not Found: AnimalNotFoundException</li>
 *   <li>409 Conflict: DuplicateAreteException</li>
 *   <li>500 Internal Server Error: Excepciones no capturadas</li>
 * </ul>
 * 
 * @see ErrorResponse
 * @see mx.vacapp.cattle.internal.domain.model.exceptions
 */
@RestControllerAdvice(basePackages = "mx.vacapp.cattle.internal.infrastructure.controllers")
@Slf4j
public class CattleGlobalExceptionHandler {
    
    /**
     * Maneja InvalidPastureException (400 Bad Request).
     * 
     * <p>Se lanza cuando se intenta asignar un animal a un potrero que no existe
     * o está inactivo. Ver Requirement 3.2 del spec.</p>
     * 
     * <p>Mensaje típico: "Potrero no existe o está inactivo"</p>
     */
    @ExceptionHandler(InvalidPastureException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasture(
            InvalidPastureException ex, 
            HttpServletRequest request) {
        
        log.warn("Invalid pasture error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Maneja InvalidGenealogyException (400 Bad Request).
     * 
     * <p>Se lanza cuando hay errores en las relaciones genealógicas:</p>
     * <ul>
     *   <li>Madre no es hembra</li>
     *   <li>Padre no es macho</li>
     *   <li>Animal es su propia madre/padre</li>
     *   <li>Fecha de nacimiento anterior a la madre</li>
     * </ul>
     * 
     * <p>Ver Requirement 2 del spec.</p>
     */
    @ExceptionHandler(InvalidGenealogyException.class)
    public ResponseEntity<ErrorResponse> handleInvalidGenealogy(
            InvalidGenealogyException ex, 
            HttpServletRequest request) {
        
        log.warn("Invalid genealogy error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Maneja SoldOrDeadAnimalException (400 Bad Request).
     * 
     * <p>Se lanza cuando se intenta modificar un animal que ya fue vendido o murió.
     * Los animales con status Vendida o Muerta son inmutables y no pueden ser
     * actualizados, movidos de potrero, o tener nuevos registros de peso/salud.</p>
     * 
     * <p>Ver Requirement 16.1 del spec.</p>
     */
    @ExceptionHandler(SoldOrDeadAnimalException.class)
    public ResponseEntity<ErrorResponse> handleSoldOrDeadAnimal(
            SoldOrDeadAnimalException ex, 
            HttpServletRequest request) {
        
        log.warn("Sold or dead animal modification attempt: {} - Path: {}", 
                ex.getMessage(), request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Maneja AnimalNotFoundException (404 Not Found).
     * 
     * <p>Se lanza cuando se solicita un animal que no existe o no pertenece
     * al tenant actual (filtro multi-tenant).</p>
     */
    @ExceptionHandler(AnimalNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAnimalNotFound(
            AnimalNotFoundException ex, 
            HttpServletRequest request) {
        
        log.warn("Animal not found: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    /**
     * Maneja DuplicateAreteException (409 Conflict).
     * 
     * <p>Se lanza cuando se intenta registrar un animal con un arete que ya existe
     * en el sistema. El arete debe ser único a nivel global (no solo por tenant)
     * según Requirement 1.2 del spec.</p>
     * 
     * <p>Mensaje típico: "Arete ya registrado en el sistema"</p>
     */
    @ExceptionHandler(DuplicateAreteException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateArete(
            DuplicateAreteException ex, 
            HttpServletRequest request) {
        
        log.warn("Duplicate arete conflict: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    /**
     * Maneja MethodArgumentNotValidException (400 Bad Request).
     * 
     * <p>Se lanza cuando falla la validación Bean Validation (@Valid) en DTOs
     * de Request. Retorna errores detallados por campo para feedback claro
     * al cliente sobre qué campos tienen problemas y por qué.</p>
     * 
     * <p>Ver Requirement 11.6 del spec.</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, 
            HttpServletRequest request) {
        
        log.warn("Validation failed: {} errors - Path: {}", 
                ex.getBindingResult().getErrorCount(), 
                request.getRequestURI());
        
        List<String> validationDetails = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
        
        ErrorResponse error = ErrorResponse.withDetails(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Error de validación en los datos de entrada",
            request.getRequestURI(),
            validationDetails
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Maneja ConstraintViolationException (400 Bad Request).
     * 
     * <p>Se lanza cuando falla la validación de restricciones de Bean Validation
     * a nivel de método o parámetros. Similar a MethodArgumentNotValidException
     * pero ocurre en diferentes contextos de validación.</p>
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, 
            HttpServletRequest request) {
        
        log.warn("Constraint violation: {} violations - Path: {}", 
                ex.getConstraintViolations().size(), 
                request.getRequestURI());
        
        List<String> violationDetails = ex.getConstraintViolations()
                .stream()
                .map(violation -> extractFieldName(violation) + ": " + violation.getMessage())
                .collect(Collectors.toList());
        
        ErrorResponse error = ErrorResponse.withDetails(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Error de validación en restricciones",
            request.getRequestURI(),
            violationDetails
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Maneja IllegalArgumentException (400 Bad Request).
     * 
     * <p>Se lanza cuando se proporcionan argumentos inválidos a métodos
     * de dominio o casos de uso.</p>
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, 
            HttpServletRequest request) {
        
        log.warn("Illegal argument: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Maneja excepciones genéricas no capturadas (500 Internal Server Error).
     * 
     * <p>Este handler es el último recurso para excepciones inesperadas.
     * Registra el error completo en logs pero NO expone detalles internos
     * al cliente por seguridad.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, 
            HttpServletRequest request) {
        
        log.error("Unexpected error: {} - Path: {}", ex.getMessage(), request.getRequestURI(), ex);
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "Ha ocurrido un error inesperado. Por favor contacte al administrador.",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    /**
     * Extrae el nombre del campo de una violación de restricción.
     * 
     * <p>Maneja property paths complejos extrayendo solo el último segmento
     * (el nombre del campo real).</p>
     * 
     * @param violation violación de constraint
     * @return nombre del campo extraído
     */
    private String extractFieldName(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        String[] parts = propertyPath.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : propertyPath;
    }
}
