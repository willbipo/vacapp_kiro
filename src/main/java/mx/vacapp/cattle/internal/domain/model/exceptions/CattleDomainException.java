package mx.vacapp.cattle.internal.domain.model.exceptions;

/**
 * Excepción base para todas las excepciones de dominio del módulo cattle-inventory.
 * Todas las excepciones específicas del dominio deben extender esta clase.
 */
public class CattleDomainException extends RuntimeException {
    
    /**
     * Constructor con mensaje de error.
     * 
     * @param message mensaje descriptivo del error
     */
    public CattleDomainException(String message) {
        super(message);
    }
    
    /**
     * Constructor con mensaje de error y causa.
     * 
     * @param message mensaje descriptivo del error
     * @param cause excepción que causó este error
     */
    public CattleDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
