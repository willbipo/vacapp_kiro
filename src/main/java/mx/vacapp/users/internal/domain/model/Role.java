package mx.vacapp.users.internal.domain.model;

/**
 * Roles del sistema en dos niveles: SaaS y Business.
 * <p>
 * Los roles SaaS (SUPER_ADMIN, SUPPORT) operan a nivel de plataforma sin restricción de tenant.
 * Los roles Business (ADMIN, MANAGER, VETERINARIAN, WORKER) operan dentro del contexto de un tenant específico.
 * </p>
 */
public enum Role {
    // SaaS Roles (sin tenant_id)
    SUPER_ADMIN("super_admin", true),
    SUPPORT("support", true),
    
    // Business Roles (con tenant_id)
    ADMIN("admin", false),
    MANAGER("manager", false),
    VETERINARIAN("veterinarian", false),
    WORKER("worker", false);
    
    private final String value;
    private final boolean saasRole;
    
    Role(String value, boolean saasRole) {
        this.value = value;
        this.saasRole = saasRole;
    }
    
    /**
     * Retorna el valor en string del rol (formato snake_case).
     *
     * @return el valor del rol
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Indica si el rol es un rol SaaS (nivel plataforma).
     *
     * @return true si es rol SaaS, false en caso contrario
     */
    public boolean isSaaSRole() {
        return saasRole;
    }
    
    /**
     * Indica si el rol es un rol Business (nivel tenant).
     *
     * @return true si es rol Business, false en caso contrario
     */
    public boolean isBusinessRole() {
        return !saasRole;
    }
    
    /**
     * Obtiene un rol a partir de su valor en string.
     *
     * @param value el valor del rol en formato snake_case
     * @return el rol correspondiente
     * @throws IllegalArgumentException si el valor no corresponde a ningún rol válido
     */
    public static Role fromString(String value) {
        for (Role role : Role.values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Rol inválido: " + value);
    }
}
