package mx.vacapp.geography.internal.domain.model.exceptions;

/**
 * Excepción lanzada cuando no se encuentra una entidad geográfica solicitada.
 * 
 * Esta excepción se usa cuando se busca un rancho, sección o potrero por ID
 * y no existe en la base de datos o no pertenece al tenant del usuario.
 */
public class EntityNotFoundException extends RuntimeException {
    
    /**
     * Constructor con mensaje descriptivo.
     * 
     * @param message mensaje explicativo del error
     */
    public EntityNotFoundException(String message) {
        super(message);
    }
    
    /**
     * Constructor con mensaje y causa raíz.
     * 
     * @param message mensaje explicativo del error
     * @param cause excepción que causó este error
     */
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
