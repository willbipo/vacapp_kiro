package mx.vacapp.cattle.internal.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que representa un registro de auditoría de ganado en la base de datos.
 * 
 * Esta entidad NO se usa en la capa de dominio. La transformación entre
 * CattleAuditEntity y CattleAudit (dominio) se realiza mediante AuditMapper.
 * 
 * Registra todas las operaciones realizadas sobre animales:
 * - CREATE: Creación de un nuevo animal
 * - UPDATE: Actualización de datos del animal
 * - CHANGE_STATUS: Cambio de estado (Activa, Vendida, Muerta, etc.)
 * - MOVE_PASTURE: Movimiento entre potreros
 * - DELETE: Eliminación del animal
 * 
 * Los campos oldValues y newValues almacenan datos en formato JSON para
 * permitir auditoría detallada de cambios.
 */
@Entity
@Table(name = "cattle_audit", 
    indexes = {
        @Index(name = "idx_cattle_audit_animal_id", columnList = "animal_id"),
        @Index(name = "idx_cattle_audit_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_cattle_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_cattle_audit_operation_type", columnList = "operation_type")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CattleAuditEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_id", updatable = false, nullable = false)
    private UUID auditId;
    
    @Column(name = "animal_id", nullable = false)
    private UUID animalId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private OperationType operationType;
    
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    
    @Column(name = "modified_by", nullable = false)
    private UUID modifiedBy;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * Valores antiguos del animal antes del cambio.
     * Almacenado en formato JSON.
     * NULL en operaciones CREATE.
     */
    @Column(name = "old_values", columnDefinition = "JSON")
    private String oldValues;
    
    /**
     * Valores nuevos del animal después del cambio.
     * Almacenado en formato JSON.
     * NULL en operaciones DELETE.
     */
    @Column(name = "new_values", columnDefinition = "JSON")
    private String newValues;
    
    /**
     * Razón opcional del cambio.
     * Especialmente útil para cambios críticos como ventas o muertes.
     */
    @Column(name = "reason", length = 500)
    private String reason;
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
    
    /**
     * Enum que representa los tipos de operaciones auditadas.
     */
    public enum OperationType {
        CREATE("create"),
        UPDATE("update"),
        CHANGE_STATUS("change_status"),
        MOVE_PASTURE("move_pasture"),
        DELETE("delete");
        
        private final String value;
        
        OperationType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
}
