package mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos;

/**
 * Detalle de error para respuestas de validación o errores de negocio.
 * 
 * Según la especificación OpenAPI, cada error debe incluir:
 * - field: nombre del campo que causó el error (o vacío si no aplica)
 * - message: mensaje descriptivo del error
 * 
 * @see ErrorResponse
 */
public record ErrorDetail(
    String field,
    String message
) {
    /**
     * Crea un ErrorDetail para errores de validación de campos.
     * 
     * @param field nombre del campo con error
     * @param message mensaje de validación
     * @return ErrorDetail con el campo y mensaje proporcionados
     */
    public static ErrorDetail validationError(String field, String message) {
        return new ErrorDetail(field, message);
    }
    
    /**
     * Crea un ErrorDetail para errores de negocio (sin campo específico).
     * 
     * @param message mensaje de error de negocio
     * @return ErrorDetail con field vacío y el mensaje proporcionado
     */
    public static ErrorDetail businessError(String message) {
        return new ErrorDetail("", message);
    }
}