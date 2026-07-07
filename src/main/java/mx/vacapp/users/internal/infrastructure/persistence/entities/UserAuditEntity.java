package mx.vacapp.users.internal.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que representa la tabla users_audit en la base de datos.
 * <p>
 * Esta clase registra todas las operaciones sobre usuarios (CREATE, UPDATE, DEACTIVATE)
 * para cumplir con requisitos de auditoría y trazabilidad según Requirement 12.
 * </p>
 * <p>
 * Cada operación que modifica un usuario genera automáticamente un registro de auditoría
 * con los valores anteriores y nuevos en formato JSON, permitiendo reconstruir el historial
 * completo de cambios.
 * </p>
 * <p>
 * Los registros de auditoría son inmutables y se retienen por mínimo 730 días (2 años)
 * antes de permitir purga manual por super_admin.
 * </p>
 * <p>
 * Esta entidad es parte de la capa de infraestructura y NO debe ser expuesta fuera del módulo.
 * </p>
 */
@Entity
@Table(name = "users_audit")
public class UserAuditEntity {
    
    /**
     * Identificador único del registro de auditoría.
     * <p>
     * Clave primaria de la tabla. Se genera mediante UUID para cada operación auditada.
     * </p>
     */
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "audit_id", nullable = false, updatable = false, length = 36)
    private UUID auditId;
    
    /**
     * Identificador del usuario que fue modificado.
     * <p>
     * Referencia al user_id de la tabla users. Este campo permite rastrear todos
     * los cambios históricos de un usuario específico.
     * </p>
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", nullable = false, updatable = false, length = 36)
    private UUID userId;
    
    /**
     * Timestamp UTC de cuándo ocurrió la operación.
     * <p>
     * Registra el momento exacto en que se realizó la modificación sobre el usuario.
     * Permite reconstruir el orden cronológico de cambios.
     * </p>
     */
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;
    
    /**
     * UUID del usuario que realizó la operación.
     * <p>
     * Identifica quién ejecutó la acción (CREATE, UPDATE, DEACTIVATE).
     * Para operaciones de creación, este valor es igual a created_by del usuario.
     * Para actualizaciones, identifica al administrador responsable del cambio.
     * </p>
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "modified_by", nullable = false, updatable = false, length = 36)
    private UUID modifiedBy;
    
    /**
     * Tipo de operación realizada.
     * <p>
     * Valores válidos: CREATE, UPDATE, DEACTIVATE.
     * - CREATE: Registro inicial del usuario
     * - UPDATE: Modificación de campos (name, email, role, etc.)
     * - DEACTIVATE: Cambio de status a 'inactive'
     * </p>
     */
    @Column(name = "operation_type", nullable = false, length = 20, updatable = false)
    private String operationType;
    
    /**
     * Valores anteriores en formato JSON.
     * <p>
     * Snapshot de los campos del usuario ANTES de la operación.
     * Para operaciones CREATE, este campo es NULL.
     * Para UPDATE y DEACTIVATE, contiene JSON con los valores previos.
     * </p>
     * <p>
     * Ejemplo: {"email":"old@example.com","name":"Old Name","role":"worker"}
     * </p>
     */
    @Column(name = "old_values", nullable = true, columnDefinition = "TEXT", updatable = false)
    private String oldValues;
    
    /**
     * Valores nuevos en formato JSON.
     * <p>
     * Snapshot de los campos del usuario DESPUÉS de la operación.
     * Contiene JSON con todos los valores del usuario tras aplicar el cambio.
     * </p>
     * <p>
     * Ejemplo: {"email":"new@example.com","name":"New Name","role":"manager"}
     * </p>
     */
    @Column(name = "new_values", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String newValues;
    
    // Constructor sin argumentos requerido por JPA
    public UserAuditEntity() {
    }
    
    // Constructor con todos los argumentos para facilitar creación
    public UserAuditEntity(UUID auditId, UUID userId, Instant timestamp, UUID modifiedBy, 
                          String operationType, String oldValues, String newValues) {
        this.auditId = auditId;
        this.userId = userId;
        this.timestamp = timestamp;
        this.modifiedBy = modifiedBy;
        this.operationType = operationType;
        this.oldValues = oldValues;
        this.newValues = newValues;
    }
    
    // Getters y Setters
    public UUID getAuditId() {
        return auditId;
    }
    
    public void setAuditId(UUID auditId) {
        this.auditId = auditId;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public UUID getModifiedBy() {
        return modifiedBy;
    }
    
    public void setModifiedBy(UUID modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
    
    public String getOperationType() {
        return operationType;
    }
    
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
    
    public String getOldValues() {
        return oldValues;
    }
    
    public void setOldValues(String oldValues) {
        this.oldValues = oldValues;
    }
    
    public String getNewValues() {
        return newValues;
    }
    
    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }
}
