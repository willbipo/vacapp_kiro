package mx.vacapp.geography.internal.domain.repository;

import mx.vacapp.geography.internal.domain.model.Seccion;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida (interfaz) para persistencia de secciones.
 * Define operaciones de acceso a datos sin acoplar con detalles de implementación.
 * 
 * La implementación debe aplicar filtrado por tenant_id automáticamente.
 */
public interface SeccionRepository {
    
    /**
     * Guarda una sección (crear o actualizar).
     * 
     * @param seccion entidad de dominio a persistir
     * @return sección persistida
     */
    Seccion save(Seccion seccion);
    
    /**
     * Busca una sección por ID.
     * 
     * @param seccionId UUID de la sección
     * @return Optional con la sección si existe y pertenece al tenant actual
     */
    Optional<Seccion> findById(UUID seccionId);
    
    /**
     * Verifica si existe una sección con el nombre dado en un rancho específico.
     * 
     * @param nombre nombre a verificar (case-insensitive)
     * @param ranchoId UUID del rancho padre
     * @param tenantId ID del tenant
     * @return true si existe
     */
    boolean existsByNombreAndRanchoIdAndTenantId(String nombre, UUID ranchoId, UUID tenantId);
    
    /**
     * Lista todas las secciones de un rancho.
     * 
     * @param ranchoId UUID del rancho
     * @param tenantId ID del tenant
     * @return lista de secciones
     */
    List<Seccion> findByRanchoIdAndTenantId(UUID ranchoId, UUID tenantId);
    
    /**
     * Lista todas las secciones del tenant actual con paginación.
     * 
     * @param tenantId ID del tenant
     * @param page número de página (0-based)
     * @param size tamaño de página
     * @return lista de secciones
     */
    List<Seccion> findByTenantId(UUID tenantId, int page, int size);
    
    /**
     * Cuenta el total de secciones del tenant actual.
     * 
     * @param tenantId ID del tenant
     * @return cantidad total de secciones
     */
    long countByTenantId(UUID tenantId);
    
    /**
     * Lista secciones activas de un rancho.
     * 
     * @param ranchoId UUID del rancho
     * @param tenantId ID del tenant
     * @return lista de secciones con status ACTIVE
     */
    List<Seccion> findActiveByRanchoIdAndTenantId(UUID ranchoId, UUID tenantId);
    
    /**
     * Calcula la suma de superficies de todos los potreros activos de una sección.
     * 
     * @param seccionId UUID de la sección
     * @return suma de superficies, o 0 si no hay potreros
     */
    BigDecimal sumSuperficiePotrerosSeccionId(UUID seccionId);
    
    /**
     * Cuenta los potreros activos de una sección.
     * 
     * @param seccionId UUID de la sección
     * @return cantidad de potreros activos
     */
    long countPotrerosActivosBySeccionId(UUID seccionId);
    
    /**
     * Verifica si una sección tiene potreros activos.
     * 
     * @param seccionId UUID de la sección
     * @return true si tiene al menos un potrero activo
     */
    boolean hasActivePotreros(UUID seccionId);
}
