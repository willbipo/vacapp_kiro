package mx.vacapp.users.internal.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad de dominio que representa un usuario del sistema.
 * <p>
 * Esta clase es un POJO puro sin anotaciones de Spring/JPA, siguiendo los principios
 * de Clean Architecture. La inmutabilidad se garantiza mediante campos final y el patrón Builder.
 * </p>
 * <p>
 * Los usuarios pueden ser de dos tipos:
 * <ul>
 *   <li>Usuarios SaaS: con tenantId null y roles SUPER_ADMIN o SUPPORT</li>
 *   <li>Usuarios de negocio: con tenantId específico y roles ADMIN, MANAGER, VETERINARIAN o WORKER</li>
 * </ul>
 * </p>
 */
public class User {
    private final UUID userId;
    private final String email;
    private final String name;
    private final String phone;
    private final String passwordHash;
    private final Role role;
    private final UserStatus status;
    private final UUID tenantId;  // null para usuarios SaaS
    private final Instant createdAt;
    private final Instant updatedAt;
    private final UUID createdBy;
    private final UUID updatedBy;
    
    /**
     * Constructor privado para forzar uso de factory methods y Builder.
     *
     * @param builder el builder con todos los datos del usuario
     */
    private User(Builder builder) {
        this.userId = builder.userId;
        this.email = builder.email;
        this.name = builder.name;
        this.phone = builder.phone;
        this.passwordHash = builder.passwordHash;
        this.role = builder.role;
        this.status = builder.status;
        this.tenantId = builder.tenantId;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
    }
    
    /**
     * Factory method para crear un nuevo usuario.
     * <p>
     * Genera automáticamente el userId, establece el estado como ACTIVE,
     * asigna WORKER como rol por defecto si no se especifica,
     * y normaliza el email a minúsculas.
     * </p>
     *
     * @param email el email del usuario (será normalizado a minúsculas)
     * @param name el nombre completo del usuario
     * @param phone el teléfono del usuario
     * @param passwordHash el hash BCrypt de la contraseña
     * @param role el rol del usuario (si es null, se asigna WORKER por defecto)
     * @param tenantId el ID del tenant (null para usuarios SaaS)
     * @param createdBy el UUID del usuario que crea este registro
     * @return una nueva instancia de User con estado ACTIVE
     */
    public static User create(String email, String name, String phone, 
                             String passwordHash, Role role, UUID tenantId, UUID createdBy) {
        Instant now = Instant.now();
        return new Builder()
            .userId(UUID.randomUUID())
            .email(email.toLowerCase())
            .name(name)
            .phone(phone)
            .passwordHash(passwordHash)
            .role(role != null ? role : Role.WORKER) // default role
            .status(UserStatus.ACTIVE)
            .tenantId(tenantId)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();
    }
    
    /**
     * Verifica si el usuario está en estado activo.
     *
     * @return true si el estado es ACTIVE, false en caso contrario
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }
    
    /**
     * Verifica si el usuario es un usuario SaaS (sin tenant asociado).
     *
     * @return true si tenantId es null, false en caso contrario
     */
    public boolean isSaaSUser() {
        return this.tenantId == null;
    }
    
    /**
     * Verifica si el usuario tiene un rol de nivel SaaS.
     *
     * @return true si el rol es SUPER_ADMIN o SUPPORT, false en caso contrario
     */
    public boolean hasSaaSRole() {
        return this.role == Role.SUPER_ADMIN || this.role == Role.SUPPORT;
    }
    
    /**
     * Crea una copia inmutable del usuario con estado INACTIVE.
     * <p>
     * Este método implementa la desactivación lógica del usuario
     * sin eliminar el registro de la base de datos.
     * </p>
     *
     * @param deactivatedBy el UUID del usuario que realiza la desactivación
     * @return una nueva instancia de User con estado INACTIVE
     */
    public User deactivate(UUID deactivatedBy) {
        return new Builder()
            .from(this)
            .status(UserStatus.INACTIVE)
            .updatedAt(Instant.now())
            .updatedBy(deactivatedBy)
            .build();
    }
    
    /**
     * Crea una copia inmutable del usuario con un nuevo rol asignado.
     *
     * @param newRole el nuevo rol a asignar
     * @param changedBy el UUID del usuario que realiza el cambio de rol
     * @return una nueva instancia de User con el rol actualizado
     */
    public User changeRole(Role newRole, UUID changedBy) {
        return new Builder()
            .from(this)
            .role(newRole)
            .updatedAt(Instant.now())
            .updatedBy(changedBy)
            .build();
    }
    
    // Getters (sin setters - inmutabilidad)
    
    public UUID getUserId() { 
        return userId; 
    }
    
    public String getEmail() { 
        return email; 
    }
    
    public String getName() { 
        return name; 
    }
    
    public String getPhone() { 
        return phone; 
    }
    
    public String getPasswordHash() { 
        return passwordHash; 
    }
    
    public Role getRole() { 
        return role; 
    }
    
    public UserStatus getStatus() { 
        return status; 
    }
    
    public UUID getTenantId() { 
        return tenantId; 
    }
    
    public Instant getCreatedAt() { 
        return createdAt; 
    }
    
    public Instant getUpdatedAt() { 
        return updatedAt; 
    }
    
    public UUID getCreatedBy() { 
        return createdBy; 
    }
    
    public UUID getUpdatedBy() { 
        return updatedBy; 
    }
    
    /**
     * Builder para construir instancias inmutables de User.
     * <p>
     * Soporta el método from() para crear copias modificadas de usuarios existentes,
     * implementando el patrón de inmutabilidad funcional.
     * </p>
     */
    public static class Builder {
        private UUID userId;
        private String email;
        private String name;
        private String phone;
        private String passwordHash;
        private Role role;
        private UserStatus status;
        private UUID tenantId;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID createdBy;
        private UUID updatedBy;
        
        public Builder userId(UUID userId) { 
            this.userId = userId; 
            return this; 
        }
        
        public Builder email(String email) { 
            this.email = email; 
            return this; 
        }
        
        public Builder name(String name) { 
            this.name = name; 
            return this; 
        }
        
        public Builder phone(String phone) { 
            this.phone = phone; 
            return this; 
        }
        
        public Builder passwordHash(String passwordHash) { 
            this.passwordHash = passwordHash; 
            return this; 
        }
        
        public Builder role(Role role) { 
            this.role = role; 
            return this; 
        }
        
        public Builder status(UserStatus status) { 
            this.status = status; 
            return this; 
        }
        
        public Builder tenantId(UUID tenantId) { 
            this.tenantId = tenantId; 
            return this; 
        }
        
        public Builder createdAt(Instant createdAt) { 
            this.createdAt = createdAt; 
            return this; 
        }
        
        public Builder updatedAt(Instant updatedAt) { 
            this.updatedAt = updatedAt; 
            return this; 
        }
        
        public Builder createdBy(UUID createdBy) { 
            this.createdBy = createdBy; 
            return this; 
        }
        
        public Builder updatedBy(UUID updatedBy) { 
            this.updatedBy = updatedBy; 
            return this; 
        }
        
        /**
         * Copia todos los campos de un usuario existente al builder.
         * <p>
         * Útil para crear copias inmutables con modificaciones parciales.
         * </p>
         *
         * @param user el usuario del cual copiar los datos
         * @return este builder con todos los campos copiados
         */
        public Builder from(User user) {
            this.userId = user.userId;
            this.email = user.email;
            this.name = user.name;
            this.phone = user.phone;
            this.passwordHash = user.passwordHash;
            this.role = user.role;
            this.status = user.status;
            this.tenantId = user.tenantId;
            this.createdAt = user.createdAt;
            this.updatedAt = user.updatedAt;
            this.createdBy = user.createdBy;
            this.updatedBy = user.updatedBy;
            return this;
        }
        
        /**
         * Construye la instancia inmutable de User.
         *
         * @return una nueva instancia de User
         */
        public User build() {
            return new User(this);
        }
    }
}
