package mx.vacapp.cattle.internal.domain.model.exceptions;

/**
 * Excepción lanzada cuando se intenta registrar un animal con un arete
 * que ya existe en el sistema a nivel global.
 * 
 * El arete debe ser único a nivel global en Vacapp para prevenir duplicados.
 */
public class DuplicateAreteException extends CattleDomainException {
    
    /**
     * Constructor con mensaje de error.
     * 
     * @param message mensaje descriptivo del error
     */
    public DuplicateAreteException(String message) {
        super(message);
    }
}
