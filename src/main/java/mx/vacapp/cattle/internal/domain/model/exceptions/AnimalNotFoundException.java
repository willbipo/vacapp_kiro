package mx.vacapp.cattle.internal.domain.model.exceptions;

/**
 * Excepción lanzada cuando se intenta acceder a un animal que no existe
 * o que no pertenece al tenant del usuario actual.
 * 
 * Esta excepción se usa en operaciones de lectura, actualización y eliminación
 * cuando el animal_id proporcionado no existe o no es accesible.
 */
public class AnimalNotFoundException extends CattleDomainException {
    
    /**
     * Constructor con mensaje de error.
     * 
     * @param message mensaje descriptivo del error
     */
    public AnimalNotFoundException(String message) {
        super(message);
    }
}
