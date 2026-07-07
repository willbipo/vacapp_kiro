package mx.vacapp.users.internal.domain.model.exceptions;

/**
 * Excepción lanzada cuando las credenciales proporcionadas (email o contraseña) son inválidas.
 * 
 * <p>Esta excepción se utiliza durante el proceso de autenticación cuando:
 * <ul>
 *   <li>El email no existe en el sistema</li>
 *   <li>La contraseña no coincide con el hash almacenado</li>
 * </ul>
 * 
 * <p>Por razones de seguridad, no se debe especificar cuál de los dos campos es incorrecto.
 */
public class InvalidCredentialsException extends RuntimeException {
    
    /**
     * Constructor con mensaje de error.
     * 
     * @param message mensaje descriptivo del error
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }
    
    /**
     * Constructor con mensaje de error y causa raíz.
     * 
     * @param message mensaje descriptivo del error
     * @param cause excepción que causó este error
     */
    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
