package mx.vacapp.geography.internal.domain.model.exceptions;

/**
 * Excepción lanzada cuando se intenta archivar un potrero que tiene
 * ganado asignado (cattleCount > 0).
 * 
 * Un potrero solo puede archivarse si no tiene ganado. El usuario debe
 * trasladar el ganado a otro potrero antes de archivar.
 */
public class CattleAssignedException extends RuntimeException {
    
    /**
     * Constructor con mensaje descriptivo.
     * 
     * @param message mensaje explicativo del error
     */
    public CattleAssignedException(String message) {
        super(message);
    }
    
    /**
     * Constructor con mensaje y causa raíz.
     * 
     * @param message mensaje explicativo del error
     * @param cause excepción que causó este error
     */
    public CattleAssignedException(String message, Throwable cause) {
        super(message, cause);
    }
}
