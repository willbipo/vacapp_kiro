package mx.vacapp.users.internal.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utilidad para extraer información de autenticación desde el contexto de seguridad.
 * <p>
 * Esta clase proporciona métodos estáticos para obtener información del usuario
 * autenticado desde el SecurityContextHolder de Spring Security.
 * </p>
 * <p>
 * El contexto de seguridad es establecido por el JwtAuthenticationFilter después
 * de validar un token JWT. El principal del Authentication es el user_id como String.
 * </p>
 * <p>
 * <strong>Uso:</strong>
 * <pre>
 * {@code
 * // Obtener userId del usuario autenticado
 * UUID userId = SecurityUtils.getCurrentUserId();
 * 
 * // Obtener tenantId del contexto de seguridad
 * UUID tenantId = SecurityUtils.getCurrentTenantId();
 * }
 * </pre>
 * </p>
 *
 * @see SecurityContextHolder
 * @see Authentication
 * @see JwtAuthenticationFilter
 * @see TenantContext
 */
public class SecurityUtils {
    
    /**
     * Constructor privado para prevenir instanciación.
     * <p>
     * Esta es una clase de utilidad con solo métodos estáticos,
     * no debe ser instanciada directamente.
     * </p>
     */
    private SecurityUtils() {
        throw new UnsupportedOperationException("SecurityUtils es una clase de utilidad y no debe ser instanciada");
    }
    
    /**
     * Obtiene el UUID del usuario actualmente autenticado.
     * <p>
     * Extrae el user_id del objeto Authentication establecido en SecurityContextHolder
     * por el JwtAuthenticationFilter.
     * </p>
     * <p>
     * El principal del Authentication es una cadena con el UUID del usuario
     * (userId.toString()).
     * </p>
     *
     * @return el UUID del usuario autenticado, o null si no hay usuario autenticado
     * @throws IllegalStateException si hay autenticación pero el principal no es un UUID válido
     */
    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Verificar que hay autenticación y que el usuario está autenticado
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        // El principal debe ser el user_id como String
        Object principal = authentication.getPrincipal();
        if (principal == null) {
            return null;
        }
        
        try {
            // El principal es el user_id como String (ver JwtAuthenticationFilter)
            return UUID.fromString(principal.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("El principal de autenticación no es un UUID válido: " + principal, e);
        }
    }
    
    /**
     * Obtiene el tenant_id del usuario actualmente autenticado.
     * <p>
     * El tenant_id está almacenado en TenantContext (ThreadLocal) por el
     * JwtAuthenticationFilter. Para usuarios SaaS (super_admin, support),
     * puede ser null.
     * </p>
     * <p>
     * <strong>Importante:</strong> Este método solo funciona dentro del contexto
     * de un request HTTP procesado por el JwtAuthenticationFilter. Si se llama
     * fuera de ese contexto (ej: en hilos de background), retornará null o el
     * tenant_id de otro request si hay contaminación de ThreadLocal.
     * </p>
     *
     * @return el UUID del tenant actual, o null si no está establecido o es usuario SaaS
     */
    public static UUID getCurrentTenantId() {
        return TenantContext.getTenantId();
    }
    
    /**
     * Verifica si el usuario actual tiene un rol específico.
     * <p>
     * Los roles en el contexto de seguridad están en el formato "ROLE_{rol}",
     * por ejemplo "ROLE_admin", "ROLE_super_admin".
     * </p>
     *
     * @param role el nombre del rol a verificar (sin prefijo "ROLE_")
     * @return true si el usuario tiene el rol especificado, false en caso contrario
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        String roleWithPrefix = "ROLE_" + role;
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals(roleWithPrefix));
    }
    
    /**
     * Verifica si el usuario actual es un usuario SaaS (super_admin o support).
     * <p>
     * Los usuarios SaaS tienen tenant_id null en el token JWT y pueden acceder
     * a datos de múltiples tenants.
     * </p>
     *
     * @return true si el usuario actual es SaaS, false si es usuario de negocio
     */
    public static boolean isSaaSUser() {
        // Usuario SaaS tiene tenant_id null
        return getCurrentTenantId() == null;
    }
    
    /**
     * Verifica si el usuario actual está autenticado.
     *
     * @return true si hay un usuario autenticado, false en caso contrario
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
    
    /**
     * Verifica si el usuario actual es un usuario de negocio (no SaaS).
     *
     * @return true si el usuario es de negocio (tenant_id != null), false si es SaaS
     */
    public static boolean isBusinessUser() {
        return getCurrentTenantId() != null;
    }
    
    /**
     * Obtiene el user_id del usuario autenticado o lanza excepción si no está autenticado.
     * <p>
     * Similar a getCurrentUserId(), pero lanza una excepción en lugar de retornar null
     * si no hay usuario autenticado. Útil para casos donde se requiere un userId
     * obligatoriamente.
     * </p>
     *
     * @return el UUID del usuario autenticado
     * @throws IllegalStateException si no hay usuario autenticado
     */
    public static UUID getRequiredCurrentUserId() {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No hay usuario autenticado en el contexto de seguridad");
        }
        return userId;
    }
    
    /**
     * Obtiene el tenant_id del usuario autenticado o lanza excepción si no es usuario de negocio.
     * <p>
     * Útil para operaciones que requieren tenant_id y no aplican a usuarios SaaS.
     * </p>
     *
     * @return el UUID del tenant actual
     * @throws IllegalStateException si el usuario es SaaS (tenant_id null)
     */
    public static UUID getRequiredCurrentTenantId() {
        UUID tenantId = getCurrentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("El usuario actual es SaaS (tenant_id null)");
        }
        return tenantId;
    }
}