package mx.vacapp.geography.internal.domain.repository;

import mx.vacapp.geography.internal.domain.model.Potrero;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida (interfaz) para persistencia de potreros.
 * Define operaciones de acceso a datos sin acoplar con detalles de implementación.
 * 
 * La implementación debe aplicar filtrado por tenant_id automáticamente.
 */
public interface PotreroRepository {
    
    /**
     * Guarda un potrero (crear o actualizar).
     * 
     * @param potrero entidad de dominio a persistir
     * @return potrero persistido
     */
    Potrero save(Potrero potrero);
    
    /**
     * Busca un potrero por ID.
     * 
     * @param potreroId UUID del potrero
     * @return Optional con el potrero si existe y pertenece al tenant actual
     */
    Optional<Potrero> findById(UUID potreroId);
    
    /**
     * Verifica si existe un potrero con el nombre dado en un rancho específico.
     * 
     * @param nombre nombre a verificar (case-insensitive)
     * @param ranchoId UUID del rancho
     * @param tenantId ID del tenant
     * @return true si existe
     */
    boolean existsByNombreAndRanchoIdAndTenantId(String nombre, UUID ranchoId, UUID tenantId);
    
    /**
     * Verifica si existe un potrero con el nombre dado en una sección específica.
     * 
     * @param nombre nombre a verificar (case-insensitive)
     * @param seccionId UUID de la sección
     * @param tenantId ID del tenant
     * @return true si existe
     */
    boolean existsByNombreAndSeccionIdAndTenantId(String nombre, UUID seccionId, UUID tenantId);
    
    /**
     * Lista todos los potreros de un rancho (directos o en secciones).
     * 
     * @param ranchoId UUID del rancho
     * @param tenantId ID del tenant
     * @return lista de potreros
     */
    List<Potrero> findByRanchoIdAndTenantId(UUID ranchoId, UUID tenantId);
    
    /**
     * Lista todos los potreros de una sección.
     * 
     * @param seccionId UUID de la sección
     * @param tenantId ID del tenant
     * @return lista de potreros
     */
    List<Potrero> findBySeccionIdAndTenantId(UUID seccionId, UUID tenantId);
    
    /**
     * Lista todos los potreros del tenant actual con paginación.
     * 
     * @param tenantId ID del tenant
     * @param page número de página (0-based)
     * @param size tamaño de página
     * @return lista de potreros
     */
    List<Potrero> findByTenantId(UUID tenantId, int page, int size);
    
    /**
     * Cuenta el total de potreros del tenant actual.
     * 
     * @param tenantId ID del tenant
     * @return cantidad total de potreros
     */
    long countByTenantId(UUID tenantId);
    
    /**
     * Lista potreros activos de un rancho.
     * 
     * @param ranchoId UUID del rancho
     * @param tenantId ID del tenant
     * @return lista de potreros con status ACTIVE
     */
    List<Potrero> findActiveByRanchoIdAndTenantId(UUID ranchoId, UUID tenantId);
    
    /**
     * Lista potreros activos de una sección.
     * 
     * @param seccionId UUID de la sección
     * @param tenantId ID del tenant
     * @return lista de potreros con status ACTIVE
     */
    List<Potrero> findActiveBySeccionIdAndTenantId(UUID seccionId, UUID tenantId);
    
    /**
     * Lista potreros vinculados directamente a un rancho (sin sección).
     * 
     * @param ranchoId UUID del rancho
     * @param tenantId ID del tenant
     * @return lista de potreros con seccionId = null
     */
    List<Potrero> findDirectByRanchoIdAndTenantId(UUID ranchoId, UUID tenantId);
    
    /**
     * Lista potreros activos vinculados directamente a un rancho (sin sección).
     * 
     * @param ranchoId UUID del rancho
     * @param tenantId ID del tenant
     * @return lista de potreros activos con seccionId = null
     */
    List<Potrero> findActiveDirectByRanchoIdAndTenantId(UUID ranchoId, UUID tenantId);
}
