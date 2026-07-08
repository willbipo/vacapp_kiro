package mx.vacapp.cattle.internal.domain.model.exceptions;

/**
 * Excepción lanzada cuando se detecta una relación genealógica inválida.
 * 
 * Ejemplos:
 * - Madre no es hembra
 * - Padre no es macho
 * - Madre o padre no pertenecen al mismo rancho
 * - Animal es su propia madre o padre
 * - Fecha de nacimiento anterior a la de la madre
 * - Solo hembras pueden estar preñadas o parir
 */
public class InvalidGenealogyException extends CattleDomainException {
    
    /**
     * Constructor con mensaje de error.
     * 
     * @param message mensaje descriptivo del error
     */
    public InvalidGenealogyException(String message) {
        super(message);
    }
}
