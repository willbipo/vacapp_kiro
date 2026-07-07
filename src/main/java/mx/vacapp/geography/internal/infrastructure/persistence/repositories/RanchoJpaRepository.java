package mx.vacapp.geography.internal.infrastructure.persistence.repositories;

import mx.vacapp.geography.internal.domain.model.GeographicStatus;
import mx.vacapp.geography.internal.infrastructure.persistence.entities.RanchoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad RanchoEntity.
 */
public interface RanchoJpaRepository extends JpaRepository<RanchoEntity, UUID> {
    
    boolean existsByNombreIgnoreCaseAndTenantId(String nombre, UUID tenantId);
    
    Optional<RanchoEntity> findByRanchoIdAndTenantId(UUID ranchoId, UUID tenantId);
    
    Page<RanchoEntity> findByTenantId(UUID tenantId, Pageable pageable);
    
    List<RanchoEntity> findByTenantIdAndStatus(UUID tenantId, GeographicStatus status);
    
    long countByTenantId(UUID tenantId);
    
    @Query("SELECT COALESCE(SUM(s.superficie), 0) FROM SeccionEntity s " +
           "WHERE s.ranchoId = :ranchoId AND s.status = 'ACTIVE'")
    java.math.BigDecimal sumSuperficieSeccionesByRanchoId(@Param("ranchoId") UUID ranchoId);
    
    @Query("SELECT COALESCE(SUM(p.superficie), 0) FROM PotreroEntity p " +
           "WHERE p.ranchoId = :ranchoId AND p.seccionId IS NULL AND p.status = 'ACTIVE'")
    java.math.BigDecimal sumSuperficiePotrerosDirectosByRanchoId(@Param("ranchoId") UUID ranchoId);
    
    @Query("SELECT COUNT(s) FROM SeccionEntity s " +
           "WHERE s.ranchoId = :ranchoId AND s.status = 'ACTIVE'")
    long countSeccionesActivasByRanchoId(@Param("ranchoId") UUID ranchoId);
    
    @Query("SELECT COUNT(p) FROM PotreroEntity p " +
           "WHERE p.ranchoId = :ranchoId AND p.status = 'ACTIVE'")
    long countPotrerosActivosByRanchoId(@Param("ranchoId") UUID ranchoId);
    
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END FROM SeccionEntity s " +
           "WHERE s.ranchoId = :ranchoId AND s.status = 'ACTIVE'")
    boolean hasActiveSecciones(@Param("ranchoId") UUID ranchoId);
    
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END FROM PotreroEntity p " +
           "WHERE p.ranchoId = :ranchoId AND p.status = 'ACTIVE'")
    boolean hasActivePotreros(@Param("ranchoId") UUID ranchoId);
}
