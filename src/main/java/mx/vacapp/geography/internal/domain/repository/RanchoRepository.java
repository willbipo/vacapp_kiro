package mx.vacapp.geography.internal.domain.repository;

import mx.vacapp.geography.internal.domain.model.Rancho;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida (interfaz) para persistencia de ranchos.
 * Define operaciones de acceso a datos sin acoplar con detalles de implementación.
 * 
 * La implementación debe aplicar filtrado por tenant_id automáticamente.
 */
public interface RanchoRepository {
    
    /**
     * Guarda un rancho (crear o actualizar).
     * 
     * @param rancho entidad de dominio a persistir
     * @return rancho persistido
     */
    Rancho save(Rancho rancho);
    
    /**
     * Busca un rancho por ID.
     * 
     * @param ranchoId UUID del rancho
     * @return Optional con el rancho si existe y pertenece al tenant actual
     */
    Optional<Rancho> findById(UUID ranchoId);
    
    /**
     * Verifica si existe un rancho con el nombre dado en el tenant actual.
     * 
     * @param nombre nombre a verificar (case-insensitive)
     * @param tenantId ID del tenant
     * @return true si existe
     */
    boolean existsByNombreAndTenantId(String nombre, UUID tenantId);
    
    /**
     * Lista todos los ranchos del tenant actual con paginación.
     * 
     * @param tenantId ID del tenant
     * @param page número de página (0-based)
     * @param size tamaño de página
     * @return lista de ranchos
     */
    List<Rancho> findByTenantId(UUID tenantId, int page, int size);
    
    /**
     * Cuenta el total de ranchos del tenant actual.
     * 
     * @param tenantId ID del tenant
     * @return cantidad total de ranchos
     */
    long countByTenantId(UUID tenantId);
    
    /**
     * Lista ranchos activos del tenant actual.
     * 
     * @param tenantId ID del tenant
     * @return lista de ranchos con status ACTIVE
     */
    List<Rancho> findActiveByTenantId(UUID tenantId);
    
    /**
     * Calcula la suma de superficies de todas las secciones activas de un rancho.
     * 
     * @param ranchoId UUID del rancho
     * @return suma de superficies, o 0 si no hay secciones
     */
    BigDecimal sumSuperficieSeccionesByRanchoId(UUID ranchoId);
    
    /**
     * Calcula la suma de superficies de todos los potreros activos vinculados
     * directamente a un rancho (sin sección).
     * 
     * @param ranchoId UUID del rancho
     * @return suma de superficies, o 0 si no hay potreros directos
     */
    BigDecimal sumSuperficiePotrerosDirectosByRanchoId(UUID ranchoId);
    
    /**
     * Cuenta las secciones activas de un rancho.
     * 
     * @param ranchoId UUID del rancho
     * @return cantidad de secciones activas
     */
    long countSeccionesActivasByRanchoId(UUID ranchoId);
    
    /**
     * Cuenta los potreros activos de un rancho (directos o en secciones).
     * 
     * @param ranchoId UUID del rancho
     * @return cantidad de potreros activos
     */
    long countPotrerosActivosByRanchoId(UUID ranchoId);
    
    /**
     * Verifica si un rancho tiene secciones activas.
     * 
     * @param ranchoId UUID del rancho
     * @return true si tiene al menos una sección activa
     */
    boolean hasActiveSecciones(UUID ranchoId);
    
    /**
     * Verifica si un rancho tiene potreros activos.
     * 
     * @param ranchoId UUID del rancho
     * @return true si tiene al menos un potrero activo
     */
    boolean hasActivePotreros(UUID ranchoId);
}
