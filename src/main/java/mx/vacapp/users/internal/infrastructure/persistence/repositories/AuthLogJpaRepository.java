package mx.vacapp.users.internal.infrastructure.persistence.repositories;

import mx.vacapp.users.internal.infrastructure.persistence.entities.AuthLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositorio JPA para operaciones de persistencia de logs de autenticación.
 * <p>
 * Esta interfaz extiende JpaRepository para proporcionar operaciones CRUD básicas
 * sobre la entidad AuthLogEntity. Spring Data JPA genera automáticamente la implementación
 * en tiempo de ejecución.
 * </p>
 * <p>
 * Los logs de autenticación registran todos los intentos de login (exitosos y fallidos)
 * con información del usuario, timestamp, IP del cliente y User-Agent. Son inmutables
 * por diseño - una vez creados, nunca se modifican ni eliminan (solo purga manual tras
 * 730 días por super_admin).
 * </p>
 * <p>
 * Este repositorio se utiliza principalmente para:
 * <ul>
 *   <li>Registrar cada intento de autenticación (save)</li>
 *   <li>Consultar logs para análisis de seguridad (find)</li>
 *   <li>Implementar rate limiting y detección de ataques (count, exists)</li>
 * </ul>
 * </p>
 * <p>
 * Esta interfaz es parte de la capa de infraestructura y NO debe ser expuesta fuera del módulo.
 * Se utiliza exclusivamente por AuditRepositoryImpl para implementar el puerto AuditRepository.
 * </p>
 *
 * @see AuthLogEntity
 * @see org.springframework.data.jpa.repository.JpaRepository
 */
@Repository
public interface AuthLogJpaRepository extends JpaRepository<AuthLogEntity, UUID> {
    
    // Por ahora no se requieren métodos personalizados adicionales.
    // Spring Data JPA proporciona automáticamente:
    // - save(AuthLogEntity) para registrar intentos de autenticación
    // - findById(UUID) para consultar logs específicos
    // - findAll() para listar todos los logs (con paginación si se requiere)
    // - count() para obtener el total de intentos de autenticación
    //
    // En el futuro, se pueden agregar query methods como:
    // - List<AuthLogEntity> findByEmail(String email) para historial de un usuario
    // - List<AuthLogEntity> findByClientIp(String clientIp) para detectar ataques por IP
    // - long countByEmailAndSuccessFalseAndTimestampAfter(String email, boolean success, Instant since)
    //   para implementar bloqueo tras 5 intentos fallidos
    // - List<AuthLogEntity> findByTimestampBetween(Instant start, Instant end) para rangos temporales
}
