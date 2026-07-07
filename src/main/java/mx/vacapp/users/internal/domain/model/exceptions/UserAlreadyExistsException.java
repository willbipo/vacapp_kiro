package mx.vacapp.users.internal.domain.model.exceptions;

/**
 * Excepción lanzada cuando se intenta crear un usuario con un email que ya existe en el tenant.
 * 
 * <p>Esta excepción se utiliza durante el proceso de registro de usuarios cuando:
 * <ul>
 *   <li>El email proporcionado ya está registrado en la misma organización (tenant)</li>
 *   <li>Se viola la restricción de unicidad email+tenant_id</li>
 * </ul>
 * 
 * <p>Nota: El mismo email puede existir en diferentes tenants sin conflicto.
 */
public class UserAlreadyExistsException extends RuntimeException {
    
    /**
     * Constructor con mensaje de error.
     * 
     * @param message mensaje descriptivo del error
     */
    public UserAlreadyExistsException(String message) {
        super(message);
    }
    
    /**
     * Constructor con mensaje de error y causa raíz.
     * 
     * @param message mensaje descriptivo del error
     * @param cause excepción que causó este error
     */
    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
