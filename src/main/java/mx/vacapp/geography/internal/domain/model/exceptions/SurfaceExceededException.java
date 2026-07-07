package mx.vacapp.geography.internal.domain.model.exceptions;

/**
 * Excepción lanzada cuando la suma de superficies de entidades hijas
 * excede la superficie total del contenedor padre.
 * 
 * Esta es una validación CRÍTICA del módulo de geografía.
 * 
 * Ejemplos:
 * - Suma de superficies de secciones > superficie_total del rancho
 * - Suma de superficies de potreros > superficie_total del rancho (sin secciones)
 * - Suma de superficies de potreros > superficie de la sección
 */
public class SurfaceExceededException extends RuntimeException {
    
    /**
     * Constructor con mensaje descriptivo.
     * 
     * @param message mensaje explicativo del error
     */
    public SurfaceExceededException(String message) {
        super(message);
    }
    
    /**
     * Constructor con mensaje y causa raíz.
     * 
     * @param message mensaje explicativo del error
     * @param cause excepción que causó este error
     */
    public SurfaceExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
