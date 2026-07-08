package mx.vacapp.cattle.internal.infrastructure.persistence.repositories;

import mx.vacapp.cattle.internal.infrastructure.persistence.entities.AnimalEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad AnimalEntity.
 * 
 * Este repositorio proporciona operaciones CRUD básicas a través de JpaRepository
 * y métodos de consulta personalizados siguiendo las convenciones de Spring Data JPA.
 * 
 * Todos los métodos de consulta incluyen tenantId para garantizar el aislamiento
 * multi-tenant y prevenir acceso no autorizado a datos de otros tenants.
 * 
 * El arete es único a nivel global (no por tenant) para prevenir duplicados
 * en futuras integraciones y cumplir con regulaciones de identificación única.
 */
public interface AnimalJpaRepository extends JpaRepository<AnimalEntity, UUID> {
    
    /**
     * Busca un animal por arete y tenant.
     * 
     * El arete es único a nivel global, pero se filtra por tenant para garantizar
     * que solo se retornen animales del tenant del usuario autenticado.
     * 
     * @param arete identificador único del animal (case-sensitive, debe estar en mayúsculas)
     * @param tenantId UUID del tenant propietario
     * @return Optional con el animal si existe y pertenece al tenant, empty en caso contrario
     */
    Optional<AnimalEntity> findByAreteAndTenantId(String arete, UUID tenantId);
    
    /**
     * Verifica si existe un animal con el arete especificado a nivel GLOBAL.
     * 
     * Método optimizado para validaciones de unicidad global que no requieren
     * cargar la entidad completa. Útil para validaciones previas a la creación.
     * 
     * El arete debe ser único en todo el sistema, no solo por tenant.
     * 
     * @param arete identificador único del animal
     * @return true si existe un animal con ese arete en cualquier tenant, false en caso contrario
     */
    boolean existsByArete(String arete);
    
    /**
     * Verifica si existe un animal con el arete especificado en el tenant.
     * 
     * Método optimizado para validaciones de unicidad que no requieren
     * cargar la entidad completa. Útil para validaciones previas a la creación.
     * 
     * @param arete identificador único del animal
     * @param tenantId UUID del tenant propietario
     * @return true si existe un animal con ese arete en el tenant, false en caso contrario
     */
    boolean existsByAreteAndTenantId(String arete, UUID tenantId);
    
    /**
     * Lista animales por tenant y status con paginación.
     * 
     * Permite filtrar el inventario por estado (Activa, Vendida, Muerta, etc.)
     * para obtener vistas específicas del ganado. Por ejemplo:
     * - Active_Inventory: status IN (Activa, Preñada, En Reposo)
     * - Historical_Inventory: status IN (Vendida, Muerta)
     * 
     * @param tenantId UUID del tenant propietario
     * @param status estado del animal (Activa, Vendida, Muerta, Prestada, Preñada, En Reposo)
     * @param pageable configuración de paginación (page, size, sort)
     * @return lista paginada de animales que coinciden con el filtro
     */
    List<AnimalEntity> findByTenantIdAndStatus(UUID tenantId, AnimalEntity.CattleStatusEnum status, Pageable pageable);
    
    /**
     * Lista animales por rancho y tenant con paginación.
     * 
     * Permite obtener todo el inventario de un rancho específico.
     * Esencial para la vista de inventario completo filtrada por ubicación.
     * 
     * @param ranchoId UUID del rancho
     * @param tenantId UUID del tenant propietario
     * @param pageable configuración de paginación (page, size, sort)
     * @return lista paginada de animales del rancho
     */
    List<AnimalEntity> findByRanchoIdAndTenantId(UUID ranchoId, UUID tenantId, Pageable pageable);
    
    /**
     * Cuenta la cantidad total de animales de un tenant.
     * 
     * Método optimizado que ejecuta una consulta COUNT sin cargar entidades.
     * Útil para estadísticas y dashboards que muestran totales de inventario.
     * 
     * @param tenantId UUID del tenant propietario
     * @return cantidad total de animales del tenant (incluyendo todos los status)
     */
    long countByTenantId(UUID tenantId);
    
    /**
     * Busca animales cuyo arete contenga la cadena de búsqueda especificada.
     * Búsqueda case-insensitive (arete ya está en mayúsculas en DB, se busca con UPPER).
     * Filtra por tenant_id y ordena por arete ASC.
     * 
     * @param tenantId UUID del tenant propietario
     * @param areteQuery cadena de búsqueda (debe convertirse a mayúsculas antes de llamar)
     * @return lista de animales que coinciden con el patrón de búsqueda ordenada por arete
     */
    List<AnimalEntity> findByTenantIdAndAreteContainingIgnoreCaseOrderByAreteAsc(UUID tenantId, String areteQuery);
}
