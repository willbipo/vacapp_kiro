package mx.vacapp.users.internal.infrastructure.persistence.impl;

import mx.vacapp.users.internal.domain.repository.AuditRepository;
import mx.vacapp.users.internal.infrastructure.persistence.entities.AuthLogEntity;
import mx.vacapp.users.internal.infrastructure.persistence.entities.UserAuditEntity;
import mx.vacapp.users.internal.infrastructure.persistence.mappers.AuditMapper;
import mx.vacapp.users.internal.infrastructure.persistence.repositories.AuthLogJpaRepository;
import mx.vacapp.users.internal.infrastructure.persistence.repositories.UserAuditJpaRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementación del puerto AuditRepository utilizando Spring Data JPA.
 * <p>
 * Esta clase adapta el puerto de dominio AuditRepository a la capa de infraestructura,
 * utilizando los repositorios JPA (UserAuditJpaRepository y AuthLogJpaRepository)
 * para persistir registros de auditoría en la base de datos MySQL.
 * </p>
 * <p>
 * Responsabilidades:
 * <ul>
 *   <li>Transformar parámetros de dominio en entidades JPA mediante AuditMapper</li>
 *   <li>Persistir registros de auditoría de forma transaccional</li>
 *   <li>Garantizar que todos los eventos críticos queden registrados</li>
 * </ul>
 * </p>
 * <p>
 * Todos los métodos de auditoría son operaciones de solo escritura. Los registros
 * de auditoría son inmutables y se retienen por un mínimo de 2 años para cumplir
 * con requisitos de trazabilidad y cumplimiento normativo.
 * </p>
 * <p>
 * Esta clase es parte de la capa de infraestructura y NO debe ser expuesta fuera del módulo.
 * </p>
 */
@Service
public class AuditRepositoryImpl implements AuditRepository {
    
    private final UserAuditJpaRepository userAuditJpaRepository;
    private final AuthLogJpaRepository authLogJpaRepository;
    private final AuditMapper auditMapper;
    
    /**
     * Constructor con inyección de dependencias.
     * <p>
     * Spring automáticamente inyecta las dependencias requeridas:
     * UserAuditJpaRepository y AuthLogJpaRepository para operaciones JPA,
     * y AuditMapper para crear entidades de auditoría.
     * </p>
     *
     * @param userAuditJpaRepository repositorio JPA para auditoría de usuarios
     * @param authLogJpaRepository repositorio JPA para logs de autenticación
     * @param auditMapper mapper para crear entidades de auditoría
     */
    public AuditRepositoryImpl(
            UserAuditJpaRepository userAuditJpaRepository,
            AuthLogJpaRepository authLogJpaRepository,
            AuditMapper auditMapper) {
        this.userAuditJpaRepository = userAuditJpaRepository;
        this.authLogJpaRepository = authLogJpaRepository;
        this.auditMapper = auditMapper;
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Crea un registro de autenticación utilizando AuditMapper y lo persiste
     * en la tabla authentication_log.
     * </p>
     */
    @Override
    public void logAuthentication(String email, boolean success, String clientIp, String userAgent) {
        // Crear entidad de log de autenticación usando el mapper
        AuthLogEntity authLog = auditMapper.createAuthLog(email, success, clientIp, userAgent);
        
        // Persistir en la base de datos
        authLogJpaRepository.save(authLog);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Crea un registro de auditoría con operationType='CREATE' y lo persiste
     * en la tabla users_audit.
     * </p>
     */
    @Override
    public void logUserCreation(UUID userId, UUID createdBy, String oldValues, String newValues) {
        // Crear entidad de auditoría usando el mapper
        UserAuditEntity userAudit = auditMapper.createUserAudit(
            userId,
            createdBy,
            "CREATE",
            oldValues,
            newValues
        );
        
        // Persistir en la base de datos
        userAuditJpaRepository.save(userAudit);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Crea un registro de auditoría con operationType='UPDATE' y lo persiste
     * en la tabla users_audit.
     * </p>
     */
    @Override
    public void logUserUpdate(UUID userId, UUID updatedBy, String oldValues, String newValues) {
        // Crear entidad de auditoría usando el mapper
        UserAuditEntity userAudit = auditMapper.createUserAudit(
            userId,
            updatedBy,
            "UPDATE",
            oldValues,
            newValues
        );
        
        // Persistir en la base de datos
        userAuditJpaRepository.save(userAudit);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Crea un registro de auditoría con operationType='DEACTIVATE' y lo persiste
     * en la tabla users_audit. El motivo de desactivación se incluye en newValues.
     * </p>
     */
    @Override
    public void logUserDeactivation(UUID userId, UUID deactivatedBy, String reason) {
        // Crear JSON simple con el motivo de desactivación
        String newValues = String.format("{\"reason\":\"%s\"}", reason);
        
        // Crear entidad de auditoría usando el mapper
        UserAuditEntity userAudit = auditMapper.createUserAudit(
            userId,
            deactivatedBy,
            "DEACTIVATE",
            null, // oldValues no aplicable para desactivación
            newValues
        );
        
        // Persistir en la base de datos
        userAuditJpaRepository.save(userAudit);
    }
}
