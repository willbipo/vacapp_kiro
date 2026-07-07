package mx.vacapp.users.internal.infrastructure.persistence.repositories;

import mx.vacapp.users.internal.infrastructure.persistence.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para operaciones de persistencia de usuarios.
 * <p>
 * Esta interfaz extiende JpaRepository para proporcionar operaciones CRUD básicas
 * sobre la entidad UserEntity. Spring Data JPA genera automáticamente la implementación
 * en tiempo de ejecución basándose en las convenciones de nombres de métodos.
 * </p>
 * <p>
 * Los métodos personalizados (query methods) siguen la convención de nomenclatura de Spring Data:
 * <ul>
 *   <li>findBy{Campo}And{Campo} genera WHERE campo1 = ? AND campo2 = ?</li>
 *   <li>existsBy{Campo}And{Campo} genera SELECT COUNT(*) WHERE campo1 = ? AND campo2 = ?</li>
 * </ul>
 * </p>
 * <p>
 * Esta interfaz es parte de la capa de infraestructura y NO debe ser expuesta fuera del módulo.
 * Se utiliza exclusivamente por UserRepositoryImpl para implementar el puerto UserRepository.
 * </p>
 *
 * @see UserEntity
 * @see org.springframework.data.jpa.repository.JpaRepository
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    
    /**
     * Busca un usuario por su email y tenant_id.
     * <p>
     * Este método permite encontrar un usuario específico dentro del contexto de un tenant,
     * garantizando el aislamiento de datos multi-tenancy. El email debe estar normalizado
     * a minúsculas antes de la búsqueda.
     * </p>
     * <p>
     * Query generada automáticamente:
     * <pre>
     * SELECT * FROM users WHERE email = ? AND tenant_id = ?
     * </pre>
     * </p>
     *
     * @param email correo electrónico del usuario (normalizado a minúsculas)
     * @param tenantId UUID del tenant al que pertenece el usuario
     * @return Optional conteniendo el UserEntity si se encuentra, Optional.empty() si no existe
     */
    Optional<UserEntity> findByEmailAndTenantId(String email, UUID tenantId);
    
    /**
     * Busca un usuario por su email, sin restricción de tenant.
     * <p>
     * Se utiliza exclusivamente durante el flujo de login, momento en el cual
     * el tenant del usuario todavía no se conoce (el JWT aún no existe). Retorna
     * el primer usuario encontrado con ese email si existiera más de una coincidencia
     * entre tenants distintos.
     * </p>
     *
     * @param email correo electrónico del usuario (normalizado a minúsculas)
     * @return Optional conteniendo el primer UserEntity encontrado con ese email
     */
    Optional<UserEntity> findFirstByEmail(String email);
    
    /**
     * Verifica si existe un usuario con el email dado en el tenant especificado.
     * <p>
     * Este método es más eficiente que findByEmailAndTenantId cuando solo necesitamos
     * validar existencia, ya que ejecuta un COUNT(*) en lugar de traer todos los campos.
     * </p>
     * <p>
     * Se utiliza principalmente para validar unicidad del email antes de crear un nuevo
     * usuario, evitando violaciones de constraint de base de datos.
     * </p>
     * <p>
     * Query generada automáticamente:
     * <pre>
     * SELECT COUNT(*) > 0 FROM users WHERE email = ? AND tenant_id = ?
     * </pre>
     * </p>
     *
     * @param email correo electrónico a verificar (normalizado a minúsculas)
     * @param tenantId UUID del tenant donde verificar la unicidad
     * @return true si existe un usuario con ese email en ese tenant, false en caso contrario
     */
    boolean existsByEmailAndTenantId(String email, UUID tenantId);
}
