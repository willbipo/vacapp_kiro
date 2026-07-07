package mx.vacapp.users;

import java.util.UUID;

/**
 * API pública del módulo Users (Spring Modulith).
 * <p>
 * Este es el único punto de entrada que otros módulos de Vacapp pueden utilizar
 * para consultar información de usuarios. Todo lo demás bajo {@code internal/}
 * permanece encapsulado y no debe ser importado fuera de este módulo.
 * </p>
 */
public interface UsersService {

    /**
     * Verifica si un usuario está activo (estado ACTIVE).
     *
     * @param userId UUID del usuario a consultar
     * @return true si el usuario existe y está activo, false en caso contrario
     */
    boolean isUserActive(UUID userId);

    /**
     * Obtiene el tenant_id de un usuario.
     *
     * @param userId UUID del usuario a consultar
     * @return el UUID del tenant, o null si el usuario es SaaS o no existe
     */
    UUID getUserTenantId(UUID userId);

    /**
     * Verifica si un usuario tiene un rol específico.
     *
     * @param userId UUID del usuario a consultar
     * @param role valor del rol en formato snake_case (ej: "admin", "worker")
     * @return true si el usuario existe y tiene ese rol, false en caso contrario
     */
    boolean hasRole(UUID userId, String role);
}
