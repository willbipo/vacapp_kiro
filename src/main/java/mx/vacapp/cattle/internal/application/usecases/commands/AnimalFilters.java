package mx.vacapp.cattle.internal.application.usecases.commands;

import mx.vacapp.cattle.internal.domain.model.CattleStatus;
import mx.vacapp.cattle.internal.domain.model.CattleType;

import java.util.UUID;

/**
 * Record que representa filtros opcionales para listar animales.
 * 
 * <p>Todos los campos son opcionales (pueden ser null).
 * Si un filtro es null, no se aplica ese filtro en la consulta.</p>
 * 
 * <h2>Campos de Filtro:</h2>
 * <ul>
 *   <li><b>status:</b> Filtra animales por estado (ACTIVA, VENDIDA, MUERTA, etc.)</li>
 *   <li><b>tipo:</b> Filtra animales por tipo comercial (VENTA, CRIA, ENGORDA, etc.)</li>
 *   <li><b>ranchoId:</b> Filtra animales por rancho específico</li>
 *   <li><b>potreroId:</b> Filtra animales que están actualmente en un potrero específico</li>
 * </ul>
 * 
 * <h2>Uso:</h2>
 * <pre>{@code
 * // Sin filtros (todos los animales del tenant)
 * AnimalFilters filters = new AnimalFilters(null, null, null, null);
 * 
 * // Filtrar solo por status
 * AnimalFilters filters = new AnimalFilters(CattleStatus.ACTIVA, null, null, null);
 * 
 * // Filtrar por status y tipo
 * AnimalFilters filters = new AnimalFilters(CattleStatus.ACTIVA, CattleType.VIENTRE, null, null);
 * 
 * // Filtrar animales en un potrero específico
 * AnimalFilters filters = new AnimalFilters(null, null, null, potreroId);
 * }</pre>
 * 
 * @param status filtro opcional por estado del animal
 * @param tipo filtro opcional por tipo comercial del animal
 * @param ranchoId filtro opcional por rancho
 * @param potreroId filtro opcional por potrero actual (ubicación)
 * 
 * @see ListAnimalsUseCase
 * @see CattleStatus
 * @see CattleType
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
public record AnimalFilters(
    CattleStatus status,
    CattleType tipo,
    UUID ranchoId,
    UUID potreroId
) {
    
    /**
     * Crea un AnimalFilters sin ningún filtro aplicado.
     * 
     * @return AnimalFilters con todos los campos en null
     */
    public static AnimalFilters empty() {
        return new AnimalFilters(null, null, null, null);
    }
    
    /**
     * Verifica si hay algún filtro aplicado.
     * 
     * @return true si al menos un filtro no es null, false si todos son null
     */
    public boolean hasAnyFilter() {
        return status != null || tipo != null || ranchoId != null || potreroId != null;
    }
    
    /**
     * Verifica si se está filtrando por potrero.
     * 
     * @return true si potreroId no es null
     */
    public boolean hasPotreroFilter() {
        return potreroId != null;
    }
    
    /**
     * Verifica si se está filtrando por rancho.
     * 
     * @return true si ranchoId no es null
     */
    public boolean hasRanchoFilter() {
        return ranchoId != null;
    }
}
