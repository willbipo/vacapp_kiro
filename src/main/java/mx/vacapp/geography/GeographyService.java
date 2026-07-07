package mx.vacapp.geography;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * API pública del módulo de geografía.
 * Único punto de entrada para otros módulos de Vacapp.
 * 
 * Este servicio proporciona operaciones para validar y consultar
 * la estructura geográfica (ranchos, secciones, potreros) desde
 * otros módulos del sistema.
 */
public interface GeographyService {
    
    /**
     * Valida si un potrero existe y está activo.
     * 
     * @param potreroId UUID del potrero
     * @return true si el potrero existe y está activo
     */
    boolean isPotreroActive(UUID potreroId);
    
    /**
     * Obtiene el tenant_id de un rancho.
     * 
     * @param ranchoId UUID del rancho
     * @return Optional con el tenant_id, o empty si no existe
     */
    Optional<UUID> getRanchoTenantId(UUID ranchoId);
    
    /**
     * Verifica si un potrero tiene espacio disponible para asignar ganado.
     * 
     * @param potreroId UUID del potrero
     * @param cantidadGanado cantidad de animales a asignar
     * @return true si hay capacidad
     */
    boolean hasCapacity(UUID potreroId, int cantidadGanado);
    
    /**
     * Obtiene la superficie de un potrero en metros cuadrados.
     * 
     * @param potreroId UUID del potrero
     * @return Optional con la superficie en metros cuadrados
     */
    Optional<BigDecimal> getPotreroSurface(UUID potreroId);
}
