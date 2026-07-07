package mx.vacapp.geography.internal.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que representa un registro de auditoría de cambios geográficos.
 * 
 * Esta tabla almacena todos los cambios (CREATE, UPDATE, ARCHIVE) realizados
 * sobre entidades geográficas (Rancho, Sección, Potrero).
 */
@Entity
@Table(name = "geography_audit", indexes = {
    @Index(name = "idx_audit_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_audit_entity_type", columnList = "entity_type"),
    @Index(name = "idx_audit_entity_id", columnList = "entity_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_entity_tenant", columnList = "entity_type, entity_id, tenant_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeographyAuditEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_id", updatable = false, nullable = false)
    private UUID auditId;
    
    /**
     * Tipo de entidad afectada: RANCHO, SECCION, POTRERO
     */
    @Column(name = "entity_type", nullable = false, length = 20)
    private String entityType;
    
    /**
     * UUID de la entidad afectada
     */
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;
    
    /**
     * Tipo de operación: CREATE, UPDATE, ARCHIVE
     */
    @Column(name = "operation_type", nullable = false, length = 20)
    private String operationType;
    
    /**
     * Timestamp UTC del evento
     */
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    
    /**
     * UUID del usuario que realizó la operación
     */
    @Column(name = "modified_by", nullable = false)
    private UUID modifiedBy;
    
    /**
     * UUID del tenant propietario
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * JSON con valores anteriores (null para CREATE)
     * Solo incluye campos que cambiaron en UPDATE
     */
    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;
    
    /**
     * JSON con valores nuevos
     * Solo incluye campos que cambiaron en UPDATE
     */
    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;
    
    /**
     * Razón opcional del cambio (max 500 chars)
     */
    @Column(name = "reason", length = 500)
    private String reason;
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
