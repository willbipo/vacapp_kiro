package mx.vacapp.users.internal.domain.model.exceptions;

/**
 * Excepción lanzada cuando se intenta acceder a un usuario que no existe.
 * 
 * <p>Esta excepción se utiliza en operaciones de lectura, actualización o eliminación cuando:
 * <ul>
 *   <li>El ID de usuario proporcionado no existe en la base de datos</li>
 *   <li>El usuario existe pero pertenece a otro tenant (aislamiento multi-tenant)</li>
 *   <li>Se busca un usuario por email que no está registrado</li>
 * </ul>
 */
public class UserNotFoundException extends RuntimeException {
    
    /**
     * Constructor con mensaje de error.
     * 
     * @param message mensaje descriptivo del error
     */
    public UserNotFoundException(String message) {
        super(message);
    }
    
    /**
     * Constructor con mensaje de error y causa raíz.
     * 
     * @param message mensaje descriptivo del error
     * @param cause excepción que causó este error
     */
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
