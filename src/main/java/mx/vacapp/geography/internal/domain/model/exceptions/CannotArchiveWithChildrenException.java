package mx.vacapp.geography.internal.domain.model.exceptions;

/**
 * Excepción lanzada cuando se intenta archivar una entidad geográfica
 * que tiene entidades hijas activas.
 * 
 * Reglas de archivado:
 * - Un rancho NO puede archivarse si tiene secciones o potreros activos
 * - Una sección NO puede archivarse si tiene potreros activos
 * - Un potrero NO puede archivarse si tiene ganado asignado (ver CattleAssignedException)
 */
public class CannotArchiveWithChildrenException extends RuntimeException {
    
    /**
     * Constructor con mensaje descriptivo.
     * 
     * @param message mensaje explicativo del error
     */
    public CannotArchiveWithChildrenException(String message) {
        super(message);
    }
    
    /**
     * Constructor con mensaje y causa raíz.
     * 
     * @param message mensaje explicativo del error
     * @param cause excepción que causó este error
     */
    public CannotArchiveWithChildrenException(String message, Throwable cause) {
        super(message, cause);
    }
}
