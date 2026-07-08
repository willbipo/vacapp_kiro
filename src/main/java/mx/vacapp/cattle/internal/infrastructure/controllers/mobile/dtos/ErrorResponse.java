package mx.vacapp.cattle.internal.infrastructure.controllers.mobile.dtos;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO Response para errores HTTP del módulo cattle-inventory.
 * 
 * <p>Java Record que proporciona un formato estandarizado para todas las respuestas
 * de error HTTP, asegurando consistencia en la API REST del módulo de ganado.</p>
 * 
 * <p>Incluye soporte opcional para detalles de validación adicionales mediante
 * el campo details, útil para errores de validación complejos.</p>
 * 
 * <p>Campos:
 * <ul>
 *   <li><b>timestamp</b>: Fecha y hora local del error</li>
 *   <li><b>status</b>: Código HTTP numérico (400, 404, 409, etc.)</li>
 *   <li><b>error</b>: Frase de razón HTTP (Bad Request, Not Found, etc.)</li>
 *   <li><b>message</b>: Mensaje descriptivo del error en español</li>
 *   <li><b>path</b>: Ruta del endpoint donde ocurrió el error</li>
 *   <li><b>details</b>: Lista opcional de mensajes de validación adicionales</li>
 * </ul>
 * </p>
 * 
 * <p>Métodos factory estáticos disponibles:
 * <ul>
 *   <li>{@link #of(int, String, String, String)} - Error sin detalles</li>
 *   <li>{@link #withDetails(int, String, String, String, List)} - Error con detalles</li>
 * </ul>
 * </p>
 */
public record ErrorResponse(
    LocalDateTime timestamp,  // Timestamp local del error
    Integer status,           // Código HTTP (400, 404, 409, etc.)
    String error,             // HTTP status reason phrase (Bad Request, Not Found, etc.)
    String message,           // Mensaje descriptivo del error en español
    String path,              // Path del endpoint donde ocurrió el error
    List<String> details      // Lista opcional de detalles de validación
) {
    
    /**
     * Factory method para crear ErrorResponse sin detalles de validación.
     * 
     * <p>Utiliza LocalDateTime.now() para generar el timestamp automáticamente.</p>
     * 
     * @param status Código HTTP (ej: 400, 404, 409)
     * @param error Frase de razón HTTP (ej: "Bad Request", "Not Found")
     * @param message Mensaje descriptivo del error en español
     * @param path Ruta del endpoint (ej: "/api/v1/cattle")
     * @return ErrorResponse con details = null
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(
            LocalDateTime.now(),
            status,
            error,
            message,
            path,
            null
        );
    }
    
    /**
     * Factory method para crear ErrorResponse con detalles de validación.
     * 
     * <p>Utiliza LocalDateTime.now() para generar el timestamp automáticamente.</p>
     * 
     * @param status Código HTTP (ej: 400)
     * @param error Frase de razón HTTP (ej: "Bad Request")
     * @param message Mensaje descriptivo del error en español
     * @param path Ruta del endpoint (ej: "/api/v1/cattle")
     * @param details Lista de mensajes de validación específicos
     * @return ErrorResponse con details poblado
     */
    public static ErrorResponse withDetails(int status, String error, String message, 
                                           String path, List<String> details) {
        return new ErrorResponse(
            LocalDateTime.now(),
            status,
            error,
            message,
            path,
            details
        );
    }
}
