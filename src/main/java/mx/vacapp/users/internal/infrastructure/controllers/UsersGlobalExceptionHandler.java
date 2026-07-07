package mx.vacapp.users.internal.infrastructure.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import mx.vacapp.users.internal.domain.model.exceptions.AccountLockedException;
import mx.vacapp.users.internal.domain.model.exceptions.InactiveAccountException;
import mx.vacapp.users.internal.domain.model.exceptions.InvalidCredentialsException;
import mx.vacapp.users.internal.domain.model.exceptions.UserAlreadyExistsException;
import mx.vacapp.users.internal.domain.model.exceptions.UserNotFoundException;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.ErrorDetail;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para el módulo de usuarios.
 * 
 * Proporciona mapeo centralizado de excepciones a códigos HTTP apropiados
 * y respuestas JSON consistentes según la especificación OpenAPI.
 * 
 * @see ErrorResponse
 * @see ErrorDetail
 */
@RestControllerAdvice(basePackages = "mx.vacapp.users.internal.infrastructure.controllers")
public class UsersGlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(UsersGlobalExceptionHandler.class);
    
    // =========================================================================
    // Excepciones de Dominio
    // =========================================================================
    
    /**
     * Maneja InvalidCredentialsException (credenciales inválidas).
     * Mapea a HTTP 401 Unauthorized.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidCredentialsException(InvalidCredentialsException ex) {
        log.warn("Credenciales inválidas: {}", ex.getMessage());
        return ErrorResponse.businessError(ex.getMessage());
    }
    
    /**
     * Maneja UserNotFoundException (usuario no encontrado).
     * Mapea a HTTP 404 Not Found.
     */
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUserNotFoundException(UserNotFoundException ex) {
        log.warn("Usuario no encontrado: {}", ex.getMessage());
        return ErrorResponse.businessError(ex.getMessage());
    }
    
    /**
     * Maneja UserAlreadyExistsException (email ya registrado).
     * Mapea a HTTP 409 Conflict.
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.warn("Usuario ya existe: {}", ex.getMessage());
        return ErrorResponse.businessError(ex.getMessage());
    }
    
    /**
     * Maneja AccountLockedException (cuenta bloqueada).
     * Mapea a HTTP 403 Forbidden.
     */
    @ExceptionHandler(AccountLockedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccountLockedException(AccountLockedException ex) {
        log.warn("Cuenta bloqueada: {}", ex.getMessage());
        return ErrorResponse.businessError(ex.getMessage());
    }
    
    /**
     * Maneja InactiveAccountException (cuenta inactiva).
     * Mapea a HTTP 403 Forbidden.
     */
    @ExceptionHandler(InactiveAccountException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleInactiveAccountException(InactiveAccountException ex) {
        log.warn("Cuenta inactiva: {}", ex.getMessage());
        return ErrorResponse.businessError(ex.getMessage());
    }
    
    // =========================================================================
    // Excepciones de Spring Security
    // =========================================================================
    
    /**
     * Maneja AccessDeniedException (acceso denegado por Spring Security).
     * Mapea a HTTP 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return ErrorResponse.businessError("Acceso denegado. No tiene permisos suficientes para realizar esta operación.");
    }
    
    /**
     * Maneja AuthenticationException (fallo de autenticación JWT).
     * Mapea a HTTP 401 Unauthorized.
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(AuthenticationException ex) {
        log.warn("Fallo de autenticación: {}", ex.getMessage());
        
        // Mensajes específicos para diferentes tipos de AuthenticationException
        if (ex instanceof BadCredentialsException) {
            return ErrorResponse.businessError("Credenciales inválidas");
        } else if (ex instanceof AuthenticationCredentialsNotFoundException) {
            return ErrorResponse.businessError("Autenticación requerida");
        } else {
            return ErrorResponse.businessError("Token JWT inválido o expirado");
        }
    }
    
    // =========================================================================
    // Excepciones de Validación
    // =========================================================================
    
    /**
     * Maneja MethodArgumentNotValidException (falla en validación @Valid).
     * Mapea a HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        List<ErrorDetail> errorDetails = ex.getBindingResult().getFieldErrors().stream()
            .map(this::mapFieldErrorToErrorDetail)
            .collect(Collectors.toList());
        
        log.warn("Validación fallida: {} errores", errorDetails.size());
        return ErrorResponse.multipleErrors(errorDetails);
    }
    
    /**
     * Maneja ConstraintViolationException (validación de parámetros @Validated).
     * Mapea a HTTP 400 Bad Request.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolationException(ConstraintViolationException ex) {
        List<ErrorDetail> errorDetails = ex.getConstraintViolations().stream()
            .map(this::mapConstraintViolationToErrorDetail)
            .collect(Collectors.toList());
        
        log.warn("Violación de restricciones: {} errores", errorDetails.size());
        return ErrorResponse.multipleErrors(errorDetails);
    }
    
    // =========================================================================
    // Excepciones de Argumentos/Estado
    // =========================================================================
    
    /**
     * Maneja IllegalArgumentException (argumentos inválidos).
     * Mapea a HTTP 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Argumento inválido: {}", ex.getMessage());
        return ErrorResponse.businessError(ex.getMessage());
    }
    
    /**
     * Maneja IllegalStateException (estado inválido).
     * Mapea a HTTP 400 Bad Request.
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalStateException(IllegalStateException ex) {
        log.warn("Estado inválido: {}", ex.getMessage());
        return ErrorResponse.businessError(ex.getMessage());
    }
    
    // =========================================================================
    // Excepciones Genéricas
    // =========================================================================
    
    /**
     * Maneja cualquier excepción no manejada específicamente.
     * Mapea a HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Error interno no manejado en {} {}: {}", 
            request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        
        // Por seguridad, no exponer detalles internos en producción
        return ErrorResponse.businessError("Error interno del servidor. Por favor, contacte al administrador.");
    }
    
    // =========================================================================
    // Métodos Auxiliares
    // =========================================================================
    
    /**
     * Mapea un FieldError de Spring a un ErrorDetail.
     */
    private ErrorDetail mapFieldErrorToErrorDetail(FieldError fieldError) {
        String fieldName = fieldError.getField();
        String errorMessage = fieldError.getDefaultMessage();
        
        // Sanitizar mensajes que puedan contener información sensible
        if (fieldName.toLowerCase().contains("password")) {
            errorMessage = sanitizePasswordErrorMessage(errorMessage);
        }
        
        return ErrorDetail.validationError(fieldName, errorMessage);
    }
    
    /**
     * Mapea un ConstraintViolation a un ErrorDetail.
     */
    private ErrorDetail mapConstraintViolationToErrorDetail(ConstraintViolation<?> violation) {
        String fieldName = violation.getPropertyPath().toString();
        String errorMessage = violation.getMessage();
        
        // Sanitizar mensajes que puedan contener información sensible
        if (fieldName.toLowerCase().contains("password")) {
            errorMessage = sanitizePasswordErrorMessage(errorMessage);
        }
        
        return ErrorDetail.validationError(fieldName, errorMessage);
    }
    
    /**
     * Sanitiza mensajes de error relacionados con contraseñas.
     * Reemplaza valores reales con [REDACTED] para proteger información sensible.
     */
    private String sanitizePasswordErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return "Error de validación";
        }
        
        // Reemplazar cualquier valor de contraseña que pueda aparecer en el mensaje
        return errorMessage.replaceAll("(?i)password[^\\s]*", "[REDACTED]");
    }
}