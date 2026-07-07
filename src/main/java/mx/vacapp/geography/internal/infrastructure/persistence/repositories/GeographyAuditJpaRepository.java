package mx.vacapp.geography.internal.infrastructure.persistence.repositories;

import mx.vacapp.geography.internal.infrastructure.persistence.entities.GeographyAuditEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad GeographyAuditEntity.
 */
public interface GeographyAuditJpaRepository extends JpaRepository<GeographyAuditEntity, UUID> {
    
    List<GeographyAuditEntity> findByEntityTypeAndEntityIdAndTenantIdOrderByTimestampDesc(
        String entityType, UUID entityId, UUID tenantId
    );
    
    @Query("SELECT a FROM GeographyAuditEntity a " +
           "WHERE a.entityType = :entityType AND a.tenantId = :tenantId " +
           "AND (:startDate IS NULL OR a.timestamp >= :startDate) " +
           "AND (:endDate IS NULL OR a.timestamp <= :endDate) " +
           "ORDER BY a.timestamp DESC")
    List<GeographyAuditEntity> findByEntityTypeAndTenantIdWithDateRange(
        @Param("entityType") String entityType,
        @Param("tenantId") UUID tenantId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );
    
    @Query("SELECT a FROM GeographyAuditEntity a " +
           "WHERE a.tenantId = :tenantId " +
           "AND (:startDate IS NULL OR a.timestamp >= :startDate) " +
           "AND (:endDate IS NULL OR a.timestamp <= :endDate) " +
           "ORDER BY a.timestamp DESC")
    List<GeographyAuditEntity> findByTenantIdWithDateRange(
        @Param("tenantId") UUID tenantId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );
}
