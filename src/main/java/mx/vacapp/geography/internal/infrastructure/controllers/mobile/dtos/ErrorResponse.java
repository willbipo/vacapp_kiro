package mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos;

import java.util.List;

/**
 * DTO Response para errores HTTP.
 * Formato estandarizado de respuestas de error.
 */
public record ErrorResponse(
    String timestamp,  // ISO 8601 UTC
    int status,
    String error,
    String message,
    String path,
    List<FieldError> errors  // Nullable - para errores de validación
) {
    /**
     * Error de validación en un campo específico.
     */
    public record FieldError(
        String field,
        String message
    ) {
    }
}
