package mx.vacapp.users.internal.domain.model;

/**
 * Estados posibles de un usuario en el sistema.
 * <p>
 * Un usuario puede estar ACTIVE (activo y operacional), INACTIVE (desactivado por administrador),
 * o LOCKED (bloqueado temporalmente por razones de seguridad).
 * </p>
 */
public enum UserStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    LOCKED("locked");  // bloqueado temporalmente por seguridad
    
    private final String value;
    
    UserStatus(String value) {
        this.value = value;
    }
    
    /**
     * Retorna el valor en string del estado del usuario.
     *
     * @return el valor del estado
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Obtiene un estado de usuario a partir de su valor en string.
     *
     * @param value el valor del estado (active, inactive, locked)
     * @return el estado correspondiente
     * @throws IllegalArgumentException si el valor no corresponde a ningún estado válido
     */
    public static UserStatus fromString(String value) {
        for (UserStatus status : UserStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Estado inválido: " + value);
    }
}
