package mx.vacapp.users.internal.domain.model.exceptions;

/**
 * Excepción lanzada cuando se intenta autenticar con una cuenta inactiva.
 * 
 * <p>Esta excepción se utiliza cuando:
 * <ul>
 *   <li>El usuario ha sido desactivado por un administrador</li>
 *   <li>La cuenta está marcada con estado 'inactive' en la base de datos</li>
 *   <li>El usuario ha sido dado de baja pero sus datos se conservan por auditoría</li>
 * </ul>
 * 
 * <p>A diferencia de {@link AccountLockedException}, una cuenta inactiva requiere
 * intervención administrativa para ser reactivada y no se desbloquea automáticamente.
 */
public class InactiveAccountException extends RuntimeException {
    
    /**
     * Constructor con mensaje de error.
     * 
     * @param message mensaje descriptivo del error
     */
    public InactiveAccountException(String message) {
        super(message);
    }
    
    /**
     * Constructor con mensaje de error y causa raíz.
     * 
     * @param message mensaje descriptivo del error
     * @param cause excepción que causó este error
     */
    public InactiveAccountException(String message, Throwable cause) {
        super(message, cause);
    }
}
