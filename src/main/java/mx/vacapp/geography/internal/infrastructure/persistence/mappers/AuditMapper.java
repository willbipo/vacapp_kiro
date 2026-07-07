package mx.vacapp.geography.internal.infrastructure.persistence.mappers;

import lombok.RequiredArgsConstructor;
import mx.vacapp.geography.internal.infrastructure.persistence.entities.GeographyAuditEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Mapper para crear entidades de auditoría GeographyAuditEntity.
 * 
 * Responsabilidad: Construcción de registros de auditoría para operaciones
 * CREATE, UPDATE, ARCHIVE sobre entidades geográficas.
 */
@Component
@RequiredArgsConstructor
public class AuditMapper {
    
    /**
     * Crea un registro de auditoría para operación CREATE.
     * 
     * @param entityType tipo de entidad (RANCHO, SECCION, POTRERO)
     * @param entityId UUID de la entidad creada
     * @param tenantId UUID del tenant
     * @param modifiedBy UUID del usuario que crea
     * @param newValuesJson JSON con los datos de la entidad creada
     * @return entidad de auditoría lista para persistir
     */
    public GeographyAuditEntity createAuditForCreation(
            String entityType,
            UUID entityId,
            UUID tenantId,
            UUID modifiedBy,
            String newValuesJson) {
        
        return GeographyAuditEntity.builder()
            .auditId(UUID.randomUUID())
            .entityType(entityType)
            .entityId(entityId)
            .operationType("CREATE")
            .timestamp(Instant.now())
            .modifiedBy(modifiedBy)
            .tenantId(tenantId)
            .oldValues(null)  // No hay valores anteriores en creación
            .newValues(newValuesJson)
            .reason(null)
            .build();
    }
    
    /**
     * Crea un registro de auditoría para operación UPDATE.
     * 
     * @param entityType tipo de entidad (RANCHO, SECCION, POTRERO)
     * @param entityId UUID de la entidad actualizada
     * @param tenantId UUID del tenant
     * @param modifiedBy UUID del usuario que actualiza
     * @param oldValuesJson JSON con valores anteriores de campos modificados
     * @param newValuesJson JSON con valores nuevos de campos modificados
     * @return entidad de auditoría lista para persistir
     */
    public GeographyAuditEntity createAuditForUpdate(
            String entityType,
            UUID entityId,
            UUID tenantId,
            UUID modifiedBy,
            String oldValuesJson,
            String newValuesJson) {
        
        return GeographyAuditEntity.builder()
            .auditId(UUID.randomUUID())
            .entityType(entityType)
            .entityId(entityId)
            .operationType("UPDATE")
            .timestamp(Instant.now())
            .modifiedBy(modifiedBy)
            .tenantId(tenantId)
            .oldValues(oldValuesJson)
            .newValues(newValuesJson)
            .reason(null)
            .build();
    }
    
    /**
     * Crea un registro de auditoría para operación ARCHIVE.
     * 
     * @param entityType tipo de entidad (RANCHO, SECCION, POTRERO)
     * @param entityId UUID de la entidad archivada
     * @param tenantId UUID del tenant
     * @param modifiedBy UUID del usuario que archiva
     * @param reason razón opcional del archivado
     * @return entidad de auditoría lista para persistir
     */
    public GeographyAuditEntity createAuditForArchive(
            String entityType,
            UUID entityId,
            UUID tenantId,
            UUID modifiedBy,
            String reason) {
        
        return GeographyAuditEntity.builder()
            .auditId(UUID.randomUUID())
            .entityType(entityType)
            .entityId(entityId)
            .operationType("ARCHIVE")
            .timestamp(Instant.now())
            .modifiedBy(modifiedBy)
            .tenantId(tenantId)
            .oldValues("{\"status\":\"ACTIVE\"}")
            .newValues("{\"status\":\"ARCHIVED\"}")
            .reason(reason)
            .build();
    }
}
