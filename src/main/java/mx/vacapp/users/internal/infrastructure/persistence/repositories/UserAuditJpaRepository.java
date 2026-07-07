package mx.vacapp.users.internal.infrastructure.persistence.repositories;

import mx.vacapp.users.internal.infrastructure.persistence.entities.UserAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositorio JPA para operaciones de persistencia de auditoría de usuarios.
 * <p>
 * Esta interfaz extiende JpaRepository para proporcionar operaciones CRUD básicas
 * sobre la entidad UserAuditEntity. Spring Data JPA genera automáticamente la implementación
 * en tiempo de ejecución.
 * </p>
 * <p>
 * Los registros de auditoría son inmutables por diseño - una vez creados, nunca se modifican
 * ni eliminan (solo purga manual tras 730 días por super_admin). Por tanto, este repositorio
 * se utiliza principalmente para operaciones de escritura (save) y consulta (find).
 * </p>
 * <p>
 * Esta interfaz es parte de la capa de infraestructura y NO debe ser expuesta fuera del módulo.
 * Se utiliza exclusivamente por AuditRepositoryImpl para implementar el puerto AuditRepository.
 * </p>
 *
 * @see UserAuditEntity
 * @see org.springframework.data.jpa.repository.JpaRepository
 */
@Repository
public interface UserAuditJpaRepository extends JpaRepository<UserAuditEntity, UUID> {
    
    // Por ahora no se requieren métodos personalizados adicionales.
    // Spring Data JPA proporciona automáticamente:
    // - save(UserAuditEntity) para crear nuevos registros de auditoría
    // - findById(UUID) para consultar auditorías específicas
    // - findAll() para listar todas las auditorías (con paginación si se requiere)
    // - count() para obtener el total de registros de auditoría
    //
    // En el futuro, se pueden agregar query methods como:
    // - List<UserAuditEntity> findByUserId(UUID userId) para historial de un usuario
    // - List<UserAuditEntity> findByOperationType(String operationType) para filtrar por tipo
    // - List<UserAuditEntity> findByTimestampBetween(Instant start, Instant end) para rangos temporales
}
