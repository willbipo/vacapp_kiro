package mx.vacapp.geography.internal.infrastructure.persistence.repositories;

import mx.vacapp.geography.internal.domain.model.GeographicStatus;
import mx.vacapp.geography.internal.infrastructure.persistence.entities.PotreroEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad PotreroEntity.
 */
public interface PotreroJpaRepository extends JpaRepository<PotreroEntity, UUID> {
    
    boolean existsByNombreIgnoreCaseAndRanchoIdAndTenantId(String nombre, UUID ranchoId, UUID tenantId);
    
    boolean existsByNombreIgnoreCaseAndSeccionIdAndTenantId(String nombre, UUID seccionId, UUID tenantId);
    
    Optional<PotreroEntity> findByPotreroIdAndTenantId(UUID potreroId, UUID tenantId);
    
    List<PotreroEntity> findByRanchoIdAndTenantId(UUID ranchoId, UUID tenantId);
    
    List<PotreroEntity> findBySeccionIdAndTenantId(UUID seccionId, UUID tenantId);
    
    List<PotreroEntity> findByRanchoIdAndTenantIdAndStatus(UUID ranchoId, UUID tenantId, GeographicStatus status);
    
    List<PotreroEntity> findBySeccionIdAndTenantIdAndStatus(UUID seccionId, UUID tenantId, GeographicStatus status);
    
    List<PotreroEntity> findByRanchoIdAndTenantIdAndSeccionIdIsNull(UUID ranchoId, UUID tenantId);
    
    List<PotreroEntity> findByRanchoIdAndTenantIdAndSeccionIdIsNullAndStatus(UUID ranchoId, UUID tenantId, GeographicStatus status);
    
    Page<PotreroEntity> findByTenantId(UUID tenantId, Pageable pageable);
    
    long countByTenantId(UUID tenantId);
}
