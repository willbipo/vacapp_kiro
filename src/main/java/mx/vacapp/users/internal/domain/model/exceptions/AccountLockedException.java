package mx.vacapp.users.internal.domain.model.exceptions;

/**
 * Excepción lanzada cuando se intenta autenticar con una cuenta bloqueada temporalmente.
 * 
 * <p>Esta excepción se utiliza por razones de seguridad cuando:
 * <ul>
 *   <li>El usuario ha superado el número máximo de intentos fallidos de login (típicamente 5)</li>
 *   <li>La cuenta ha sido bloqueada manualmente por un administrador</li>
 *   <li>Se ha detectado actividad sospechosa en la cuenta</li>
 * </ul>
 * 
 * <p>El bloqueo puede ser temporal (15 minutos típicamente) o permanente hasta intervención administrativa.
 */
public class AccountLockedException extends RuntimeException {
    
    /**
     * Constructor con mensaje de error.
     * 
     * @param message mensaje descriptivo del error
     */
    public AccountLockedException(String message) {
        super(message);
    }
    
    /**
     * Constructor con mensaje de error y causa raíz.
     * 
     * @param message mensaje descriptivo del error
     * @param cause excepción que causó este error
     */
    public AccountLockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
