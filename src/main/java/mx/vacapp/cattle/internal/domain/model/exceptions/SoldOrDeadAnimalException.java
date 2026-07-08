package mx.vacapp.cattle.internal.domain.model.exceptions;

/**
 * Excepción lanzada cuando se intenta realizar una operación no permitida
 * en un animal con status Vendida o Muerta.
 * 
 * Operaciones prohibidas en animales vendidos o muertos:
 * - Cambiar de potrero (mover animal)
 * - Registrar nuevo peso
 * - Cambiar status a Preñada
 * - Actualizar información biológica
 * - Registrar eventos de salud
 */
public class SoldOrDeadAnimalException extends CattleDomainException {
    
    /**
     * Constructor con mensaje de error.
     * 
     * @param message mensaje descriptivo del error
     */
    public SoldOrDeadAnimalException(String message) {
        super(message);
    }
}
