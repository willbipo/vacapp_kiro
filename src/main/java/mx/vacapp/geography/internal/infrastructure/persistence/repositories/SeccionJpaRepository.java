package mx.vacapp.geography.internal.infrastructure.persistence.repositories;

import mx.vacapp.geography.internal.domain.model.GeographicStatus;
import mx.vacapp.geography.internal.infrastructure.persistence.entities.SeccionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad SeccionEntity.
 */
public interface SeccionJpaRepository extends JpaRepository<SeccionEntity, UUID> {
    
    boolean existsByNombreIgnoreCaseAndRanchoIdAndTenantId(String nombre, UUID ranchoId, UUID tenantId);
    
    Optional<SeccionEntity> findBySeccionIdAndTenantId(UUID seccionId, UUID tenantId);
    
    List<SeccionEntity> findByRanchoIdAndTenantId(UUID ranchoId, UUID tenantId);
    
    List<SeccionEntity> findByRanchoIdAndTenantIdAndStatus(UUID ranchoId, UUID tenantId, GeographicStatus status);
    
    Page<SeccionEntity> findByTenantId(UUID tenantId, Pageable pageable);
    
    long countByTenantId(UUID tenantId);
    
    @Query("SELECT COALESCE(SUM(p.superficie), 0) FROM PotreroEntity p " +
           "WHERE p.seccionId = :seccionId AND p.status = 'ACTIVE'")
    java.math.BigDecimal sumSuperficiePotrerosSeccionId(@Param("seccionId") UUID seccionId);
    
    @Query("SELECT COUNT(p) FROM PotreroEntity p " +
           "WHERE p.seccionId = :seccionId AND p.status = 'ACTIVE'")
    long countPotrerosActivosBySeccionId(@Param("seccionId") UUID seccionId);
    
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END FROM PotreroEntity p " +
           "WHERE p.seccionId = :seccionId AND p.status = 'ACTIVE'")
    boolean hasActivePotreros(@Param("seccionId") UUID seccionId);
}
