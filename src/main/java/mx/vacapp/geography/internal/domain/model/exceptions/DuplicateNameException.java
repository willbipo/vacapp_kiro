package mx.vacapp.geography.internal.domain.model.exceptions;

/**
 * Excepción lanzada cuando se intenta crear una entidad geográfica con un nombre
 * que ya existe en el mismo contexto.
 * 
 * Ejemplos:
 * - Rancho con nombre duplicado en el mismo tenant
 * - Sección con nombre duplicado en el mismo rancho
 * - Potrero con nombre duplicado en el mismo rancho o sección
 */
public class DuplicateNameException extends RuntimeException {
    
    /**
     * Constructor con mensaje descriptivo.
     * 
     * @param message mensaje explicativo del error
     */
    public DuplicateNameException(String message) {
        super(message);
    }
    
    /**
     * Constructor con mensaje y causa raíz.
     * 
     * @param message mensaje explicativo del error
     * @param cause excepción que causó este error
     */
    public DuplicateNameException(String message, Throwable cause) {
        super(message, cause);
    }
}
