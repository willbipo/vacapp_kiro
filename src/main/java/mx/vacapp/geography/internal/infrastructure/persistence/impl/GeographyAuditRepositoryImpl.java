package mx.vacapp.geography.internal.infrastructure.persistence.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.domain.repository.GeographyAuditRepository;
import mx.vacapp.geography.internal.infrastructure.persistence.entities.GeographyAuditEntity;
import mx.vacapp.geography.internal.infrastructure.persistence.repositories.GeographyAuditJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del repositorio de auditoría geográfica.
 * Adaptador que conecta el puerto de dominio con JPA.
 * 
 * Responsabilidades:
 * - Registrar eventos de auditoría (CREATE, UPDATE, ARCHIVE)
 * - Consultar historial de cambios
 * - Aplicar filtros multi-tenant automáticamente
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class GeographyAuditRepositoryImpl implements GeographyAuditRepository {
    
    private final GeographyAuditJpaRepository jpaRepository;
    
    @Override
    public void logAuditEvent(
            String entityType,
            UUID entityId,
            String operationType,
            String oldValues,
            String newValues,
            UUID modifiedBy,
            UUID tenantId,
            String reason) {
        
        log.debug("Registrando evento de auditoría: tipo={}, operación={}, entidad={}, tenant={}", 
                  entityType, operationType, entityId, tenantId);
        
        GeographyAuditEntity auditEntity = GeographyAuditEntity.builder()
                .auditId(UUID.randomUUID())
                .entityType(entityType)
                .entityId(entityId)
                .operationType(operationType)
                .timestamp(Instant.now())
                .modifiedBy(modifiedBy)
                .tenantId(tenantId)
                .oldValues(oldValues)
                .newValues(newValues)
                .reason(reason)
                .build();
        
        jpaRepository.save(auditEntity);
        
        log.info("Evento de auditoría registrado: ID={}, tipo={}, operación={}", 
                 auditEntity.getAuditId(), entityType, operationType);
    }
    
    @Override
    public List<AuditEvent> findByEntityTypeAndEntityIdAndTenantId(
            String entityType,
            UUID entityId,
            UUID tenantId) {
        
        log.debug("Consultando historial de auditoría: tipo={}, entidad={}, tenant={}", 
                  entityType, entityId, tenantId);
        
        List<GeographyAuditEntity> entities = jpaRepository
                .findByEntityTypeAndEntityIdAndTenantIdOrderByTimestampDesc(
                        entityType, entityId, tenantId);
        
        log.debug("Encontrados {} eventos de auditoría", entities.size());
        
        return entities.stream()
                .map(this::toAuditEvent)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AuditEvent> findByEntityTypeAndTenantId(
            String entityType,
            UUID tenantId,
            Instant startDate,
            Instant endDate,
            int page,
            int size) {
        
        log.debug("Consultando historial de auditoría por tipo: tipo={}, tenant={}, page={}, size={}", 
                  entityType, tenantId, page, size);
        
        PageRequest pageable = PageRequest.of(page, size);
        List<GeographyAuditEntity> entities = jpaRepository
                .findByEntityTypeAndTenantIdWithDateRange(
                        entityType, tenantId, startDate, endDate, pageable);
        
        log.debug("Encontrados {} eventos de auditoría", entities.size());
        
        return entities.stream()
                .map(this::toAuditEvent)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AuditEvent> findByTenantId(
            UUID tenantId,
            Instant startDate,
            Instant endDate,
            int page,
            int size) {
        
        log.debug("Consultando historial de auditoría: tenant={}, page={}, size={}", 
                  tenantId, page, size);
        
        PageRequest pageable = PageRequest.of(page, size);
        List<GeographyAuditEntity> entities = jpaRepository
                .findByTenantIdWithDateRange(tenantId, startDate, endDate, pageable);
        
        log.debug("Encontrados {} eventos de auditoría", entities.size());
        
        return entities.stream()
                .map(this::toAuditEvent)
                .collect(Collectors.toList());
    }
    
    /**
     * Convierte GeographyAuditEntity a AuditEvent record.
     */
    private AuditEvent toAuditEvent(GeographyAuditEntity entity) {
        return new AuditEvent(
                entity.getAuditId(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getOperationType(),
                entity.getTimestamp(),
                entity.getModifiedBy(),
                entity.getTenantId(),
                entity.getOldValues(),
                entity.getNewValues(),
                entity.getReason()
        );
    }
}
