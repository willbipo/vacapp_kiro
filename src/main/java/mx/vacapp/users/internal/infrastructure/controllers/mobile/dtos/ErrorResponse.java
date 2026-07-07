package mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos;

import java.time.Instant;
import java.util.List;

/**
 * DTO de respuesta para errores.
 * Contiene información estructurada sobre el error ocurrido.
 * 
 * Según la especificación OpenAPI, el formato debe ser:
 * {
 *   "errors": [
 *     {
 *       "field": "fieldName",
 *       "message": "errorMessage"
 *     }
 *   ]
 * }
 */
public record ErrorResponse(
    List<ErrorDetail> errors
) {
    /**
     * Crea un ErrorResponse con un solo error de validación.
     * 
     * @param field nombre del campo con error
     * @param message mensaje de validación
     * @return ErrorResponse con un solo ErrorDetail
     */
    public static ErrorResponse validationError(String field, String message) {
        return new ErrorResponse(List.of(ErrorDetail.validationError(field, message)));
    }
    
    /**
     * Crea un ErrorResponse con un solo error de negocio.
     * 
     * @param message mensaje de error de negocio
     * @return ErrorResponse con un solo ErrorDetail (field vacío)
     */
    public static ErrorResponse businessError(String message) {
        return new ErrorResponse(List.of(ErrorDetail.businessError(message)));
    }
    
    /**
     * Crea un ErrorResponse con múltiples errores de validación.
     * 
     * @param errorDetails lista de ErrorDetail
     * @return ErrorResponse con todos los errores
     */
    public static ErrorResponse multipleErrors(List<ErrorDetail> errorDetails) {
        return new ErrorResponse(errorDetails);
    }
}
