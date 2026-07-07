package mx.vacapp.users.internal.domain.repository;

import mx.vacapp.users.internal.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para operaciones de persistencia de usuarios.
 * <p>
 * Esta interfaz define el contrato para todas las operaciones de acceso a datos
 * relacionadas con usuarios, siguiendo los principios de Clean Architecture.
 * Es una interfaz pura de Java sin anotaciones de Spring/JPA.
 * </p>
 * <p>
 * La implementación de esta interfaz se encuentra en la capa de infraestructura
 * (UserRepositoryImpl) y debe aplicar automáticamente el filtrado por tenant_id
 * según el contexto de seguridad actual, garantizando el aislamiento multi-tenant.
 * </p>
 * <p>
 * <strong>Nota sobre Multi-tenancy:</strong> Todas las operaciones de lectura y escritura
 * aplican filtrado automático por tenant_id extraído del contexto de seguridad, excepto
 * para usuarios con roles SaaS (super_admin, support) que pueden operar sin restricción
 * de tenant cuando sea necesario.
 * </p>
 *
 * @see mx.vacapp.users.internal.domain.model.User
 * @see mx.vacapp.users.internal.infrastructure.persistence.impl.UserRepositoryImpl
 */
public interface UserRepository {
    
    /**
     * Guarda un nuevo usuario o actualiza uno existente en la base de datos.
     * <p>
     * Si el usuario tiene un userId existente, se actualiza el registro.
     * Si es un nuevo usuario (userId generado), se inserta un nuevo registro.
     * </p>
     * <p>
     * Esta operación aplica filtrado automático por tenant_id del contexto actual
     * para usuarios con Business_Role, garantizando que solo se persistan usuarios
     * dentro del tenant correspondiente.
     * </p>
     *
     * @param user la entidad de dominio User a persistir (no debe ser null)
     * @return el usuario guardado con todos los campos actualizados (timestamps, etc.)
     * @throws IllegalArgumentException si user es null
     * @throws SecurityException si se intenta guardar un usuario con tenant_id diferente al contexto actual
     */
    User save(User user);
    
    /**
     * Busca un usuario por su identificador único (UUID).
     * <p>
     * Aplica filtrado automático por tenant_id del contexto actual. Si el usuario
     * solicitado existe pero pertenece a otro tenant, se retorna Optional.empty().
     * </p>
     * <p>
     * Los usuarios con rol SaaS (super_admin, support) pueden acceder a usuarios
     * de cualquier tenant si la implementación lo permite explícitamente.
     * </p>
     *
     * @param userId el UUID del usuario a buscar (no debe ser null)
     * @return Optional conteniendo el usuario si existe y pertenece al tenant actual, 
     *         Optional.empty() en caso contrario
     * @throws IllegalArgumentException si userId es null
     */
    Optional<User> findById(UUID userId);
    
    /**
     * Busca un usuario por su dirección de email.
     * <p>
     * La búsqueda es case-insensitive (el email se normaliza a minúsculas antes de buscar).
     * Aplica filtrado automático por tenant_id del contexto actual.
     * </p>
     * <p>
     * Dado que el email es único dentro de cada tenant (no globalmente), esta operación
     * siempre debe ejecutarse en el contexto de un tenant específico o sin restricción
     * para usuarios SaaS.
     * </p>
     *
     * @param email la dirección de email a buscar (no debe ser null ni vacío)
     * @return Optional conteniendo el usuario si existe con ese email en el tenant actual,
     *         Optional.empty() en caso contrario
     * @throws IllegalArgumentException si email es null o vacío
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Verifica si existe un usuario con el email dado en el tenant especificado.
     * <p>
     * Este método se utiliza principalmente para validar la unicidad del email
     * durante el registro de nuevos usuarios. La verificación es case-insensitive.
     * </p>
     * <p>
     * A diferencia de otros métodos, este recibe explícitamente el tenantId como
     * parámetro para validar unicidad en el tenant de destino, que puede diferir
     * del tenant del contexto actual (por ejemplo, cuando un super_admin crea
     * un usuario para otro tenant).
     * </p>
     *
     * @param email el email a verificar (no debe ser null ni vacío)
     * @param tenantId el UUID del tenant donde verificar unicidad (puede ser null para usuarios SaaS)
     * @return true si existe un usuario con ese email en el tenant especificado,
     *         false en caso contrario
     * @throws IllegalArgumentException si email es null o vacío
     */
    boolean existsByEmailAndTenantId(String email, UUID tenantId);
    
    /**
     * Lista usuarios con paginación.
     * <p>
     * Aplica filtrado automático por tenant_id del contexto actual. Solo retorna
     * usuarios del tenant en sesión, excepto para usuarios con roles SaaS que pueden
     * ver usuarios de todos los tenants si tienen los permisos adecuados.
     * </p>
     * <p>
     * La paginación es 0-indexed (la primera página es 0). El tamaño máximo recomendado
     * es 100 registros por página para mantener el rendimiento.
     * </p>
     *
     * @param page número de página a recuperar (0-indexed, debe ser >= 0)
     * @param size número de usuarios por página (debe ser > 0 y <= 100)
     * @return lista de usuarios de la página solicitada, puede estar vacía si no hay resultados
     * @throws IllegalArgumentException si page < 0 o size <= 0 o size > 100
     */
    List<User> findAll(int page, int size);
    
    /**
     * Cuenta el número total de usuarios en el tenant actual.
     * <p>
     * Aplica filtrado automático por tenant_id del contexto actual. El conteo
     * incluye usuarios en todos los estados (ACTIVE, INACTIVE, LOCKED).
     * </p>
     * <p>
     * Este método es útil para cálculos de paginación y métricas del sistema.
     * </p>
     *
     * @return el número total de usuarios en el tenant actual (>= 0)
     */
    long count();
    
    /**
     * Desactiva lógicamente un usuario (soft delete).
     * <p>
     * Marca el usuario como INACTIVE sin eliminar físicamente el registro de la base de datos.
     * Esta operación es irreversible a través de la API pública (requiere intervención manual
     * de un administrador para reactivar).
     * </p>
     * <p>
     * Aplica filtrado automático por tenant_id. Solo se puede desactivar un usuario
     * si pertenece al tenant del contexto actual.
     * </p>
     * <p>
     * La operación actualiza los campos status (a INACTIVE), updated_at (timestamp actual)
     * y updated_by (userId del responsable), y registra la acción en la tabla de auditoría.
     * </p>
     *
     * @param userId el UUID del usuario a desactivar (no debe ser null)
     * @param deactivatedBy el UUID del usuario que realiza la desactivación (no debe ser null)
     * @return el usuario desactivado con el estado actualizado
     * @throws IllegalArgumentException si userId o deactivatedBy son null
     * @throws mx.vacapp.users.internal.domain.exceptions.UserNotFoundException si el usuario no existe
     *         o no pertenece al tenant actual
     */
    User deactivate(UUID userId, UUID deactivatedBy);
}
