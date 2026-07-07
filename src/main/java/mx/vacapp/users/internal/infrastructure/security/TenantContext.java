package mx.vacapp.users.internal.infrastructure.security;

import java.util.UUID;

/**
 * Contexto de seguridad que almacena el tenant_id del usuario autenticado actual.
 * <p>
 * Esta clase utiliza ThreadLocal para mantener el tenant_id de forma thread-safe,
 * garantizando que cada request HTTP tenga su propio contexto aislado.
 * </p>
 * <p>
 * El tenant_id se establece automáticamente por el JwtAuthenticationFilter después
 * de validar el token JWT, y se utiliza por todos los repositorios para aplicar
 * filtrado automático en operaciones de persistencia, garantizando el aislamiento
 * multi-tenant.
 * </p>
 * <p>
 * <strong>Importante:</strong> Siempre se debe llamar a clear() al finalizar el
 * procesamiento del request para evitar memory leaks en entornos con thread pooling.
 * </p>
 * <p>
 * <strong>Usuarios SaaS:</strong> Para usuarios con roles SaaS (super_admin, support),
 * el tenant_id puede ser null, lo que indica acceso sin restricción de tenant.
 * </p>
 *
 * @see java.lang.ThreadLocal
 */
public class TenantContext {
    
    /**
     * ThreadLocal que almacena el tenant_id del request actual.
     * <p>
     * Cada thread (request HTTP) tiene su propia copia del tenant_id,
     * garantizando aislamiento entre requests concurrentes.
     * </p>
     */
    private static final ThreadLocal<UUID> currentTenantId = new ThreadLocal<>();
    
    /**
     * Constructor privado para prevenir instanciación.
     * <p>
     * Esta es una clase de utilidad con solo métodos estáticos,
     * no debe ser instanciada.
     * </p>
     */
    private TenantContext() {
        throw new UnsupportedOperationException("TenantContext es una clase de utilidad y no debe ser instanciada");
    }
    
    /**
     * Establece el tenant_id para el thread actual (request HTTP actual).
     * <p>
     * Este método debe ser llamado por el JwtAuthenticationFilter después
     * de extraer el tenant_id del token JWT validado.
     * </p>
     * <p>
     * Si tenantId es null, indica que el usuario tiene un rol SaaS y puede
     * operar sin restricción de tenant.
     * </p>
     *
     * @param tenantId el UUID del tenant del usuario autenticado, o null para usuarios SaaS
     */
    public static void setTenantId(UUID tenantId) {
        currentTenantId.set(tenantId);
    }
    
    /**
     * Obtiene el tenant_id del thread actual (request HTTP actual).
     * <p>
     * Este método es utilizado por los repositorios para aplicar filtrado
     * automático por tenant_id en todas las operaciones de persistencia.
     * </p>
     * <p>
     * Si retorna null, indica que:
     * <ul>
     *   <li>El usuario tiene un rol SaaS (super_admin, support) sin restricción de tenant</li>
     *   <li>No se ha establecido el contexto (error de configuración o request no autenticado)</li>
     * </ul>
     * </p>
     *
     * @return el UUID del tenant actual, o null si no está establecido o es usuario SaaS
     */
    public static UUID getTenantId() {
        return currentTenantId.get();
    }
    
    /**
     * Limpia el tenant_id del thread actual.
     * <p>
     * <strong>CRÍTICO:</strong> Este método DEBE ser llamado al finalizar el
     * procesamiento de cada request para evitar memory leaks en servidores con
     * thread pooling (como Tomcat).
     * </p>
     * <p>
     * Si no se llama, el thread puede ser reutilizado para otro request
     * con el tenant_id anterior, causando filtrado incorrecto y violaciones
     * de seguridad multi-tenant.
     * </p>
     * <p>
     * Normalmente se invoca en un bloque finally del filtro de autenticación
     * o mediante un interceptor de Spring MVC.
     * </p>
     */
    public static void clear() {
        currentTenantId.remove();
    }
    
    /**
     * Verifica si hay un tenant_id establecido en el contexto actual.
     * <p>
     * Este método es útil para validar que el contexto está correctamente
     * configurado antes de ejecutar operaciones que requieren tenant_id.
     * </p>
     *
     * @return true si hay un tenant_id establecido (no null), false en caso contrario
     */
    public static boolean hasTenantId() {
        return currentTenantId.get() != null;
    }
}
