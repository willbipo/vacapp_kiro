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
 * Entidad JPA que representa la tabla users en la base de datos.
 * <p>
 * Esta clase es la representación de persistencia correspondiente al modelo de dominio User.java.
 * Mapea la tabla users con sus columnas en formato snake_case según las convenciones de la base de datos.
 * </p>
 * <p>
 * Esta entidad es parte de la capa de infraestructura y NO debe ser expuesta fuera del módulo.
 * La transformación entre UserEntity y User del dominio se realiza mediante UserMapper.
 * </p>
 * <p>
 * Los roles y estados se almacenan como String en la base de datos para facilitar la portabilidad
 * y evitar dependencias de tipos enum específicos del proveedor de base de datos.
 * </p>
 */
@Entity
@Table(name = "users")
public class UserEntity {
    
    /**
     * Identificador único del usuario.
     * <p>
     * Clave primaria de la tabla. Se genera mediante UUID para garantizar unicidad global
     * en un entorno distribuido multitenant.
     * </p>
     */
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", nullable = false, updatable = false, length = 36)
    private UUID userId;
    
    /**
     * Correo electrónico del usuario.
     * <p>
     * Debe ser único dentro del tenant (constraint en base de datos).
     * Se normaliza a minúsculas antes de persistir.
     * </p>
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    
    /**
     * Nombre completo del usuario.
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    /**
     * Número de teléfono del usuario.
     */
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;
    
    /**
     * Hash BCrypt de la contraseña del usuario.
     * <p>
     * Nunca se almacena la contraseña en texto plano. Se usa BCrypt con factor de trabajo 10-12.
     * Este campo nunca debe ser incluido en logs ni respuestas HTTP.
     * </p>
     */
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;
    
    /**
     * Rol del usuario en el sistema.
     * <p>
     * Valores posibles: super_admin, support, admin, manager, veterinarian, worker.
     * Se almacena como String para compatibilidad y flexibilidad.
     * </p>
     */
    @Column(name = "role", nullable = false, length = 20)
    private String role;
    
    /**
     * Estado actual del usuario.
     * <p>
     * Valores posibles: active, inactive, locked.
     * Se almacena como String para compatibilidad.
     * </p>
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    
    /**
     * Identificador del tenant al que pertenece el usuario.
     * <p>
     * Es NULL para usuarios SaaS (super_admin, support).
     * Es obligatorio para usuarios de negocio (admin, manager, veterinarian, worker).
     * </p>
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tenant_id", nullable = true, length = 36)
    private UUID tenantId;
    
    /**
     * Timestamp UTC de creación del registro.
     * <p>
     * Se establece automáticamente al crear el usuario y nunca debe modificarse.
     * </p>
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * Timestamp UTC de la última actualización del registro.
     * <p>
     * Se actualiza automáticamente cada vez que se modifica el usuario.
     * </p>
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * UUID del usuario que creó este registro.
     * <p>
     * Para trazabilidad y auditoría. Nunca debe modificarse después de la creación.
     * </p>
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "created_by", nullable = false, updatable = false, length = 36)
    private UUID createdBy;
    
    /**
     * UUID del usuario que realizó la última actualización.
     * <p>
     * Para trazabilidad y auditoría. Se actualiza con cada modificación.
     * </p>
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "updated_by", nullable = false, length = 36)
    private UUID updatedBy;
    
    /**
     * Constructor por defecto requerido por JPA.
     */
    public UserEntity() {
    }
    
    /**
     * Constructor con todos los campos.
     */
    public UserEntity(UUID userId, String email, String name, String phone, String passwordHash,
                      String role, String status, UUID tenantId, Instant createdAt, Instant updatedAt,
                      UUID createdBy, UUID updatedBy) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.tenantId = tenantId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }
    
    // Getters y Setters
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public UUID getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
    
    public UUID getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }
}
