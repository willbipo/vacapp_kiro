package mx.vacapp.cattle.internal.domain.model.exceptions;

/**
 * Excepción lanzada cuando se intenta asignar un animal a un potrero inválido.
 * 
 * Ejemplos:
 * - Potrero no existe en el sistema
 * - Potrero está inactivo o archivado
 * - Potrero no pertenece al mismo tenant
 * - Error al validar el potrero con el módulo geographic-control
 */
public class InvalidPastureException extends CattleDomainException {
    
    /**
     * Constructor con mensaje de error.
     * 
     * @param message mensaje descriptivo del error
     */
    public InvalidPastureException(String message) {
        super(message);
    }
}
