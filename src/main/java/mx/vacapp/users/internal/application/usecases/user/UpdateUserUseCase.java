package mx.vacapp.users.internal.application.usecases.user;

import mx.vacapp.users.internal.application.usecases.commands.UpdateUserCommand;
import mx.vacapp.users.internal.application.usecases.commands.UserResult;
import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.exceptions.UserNotFoundException;
import mx.vacapp.users.internal.domain.repository.AuditRepository;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import mx.vacapp.users.internal.infrastructure.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso: Actualizar usuario existente.
 * <p>
 * Este caso de uso implementa la lógica de negocio para modificar los datos
 * de un usuario existente, cumpliendo con los siguientes requisitos:
 * </p>
 * <ul>
 *   <li>Validar permisos de acceso basados en tenant (Requirement 6.5)</li>
 *   <li>Prohibir cambio de tenant_id (Requirement 6.5)</li>
 *   <li>Actualizar campos permitidos: name, phone, role (Requirement 6.4)</li>
 *   <li>Registrar auditoría de la actualización (Requirement 12.2)</li>
 * </ul>
 * <p>
 * El tenant_id NO puede ser modificado bajo ninguna circunstancia para mantener
 * la integridad del aislamiento multi-tenant.
 * </p>
 *
 * @see UpdateUserCommand
 * @see UserResult
 */
@Service
public class UpdateUserUseCase {
    
    private final UserRepository userRepository;
    private final AuditRepository auditRepository;
    
    /**
     * Constructor con inyección de dependencias.
     *
     * @param userRepository repositorio de usuarios
     * @param auditRepository repositorio de auditoría
     */
    public UpdateUserUseCase(
            UserRepository userRepository,
            AuditRepository auditRepository) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
    }
    
    /**
     * Ejecuta la actualización de un usuario existente.
     * <p>
     * Valida que el usuario existe y pertenece al tenant actual (o que el usuario
     * autenticado tiene permisos SaaS), actualiza los campos modificables,
     * persiste los cambios y registra la auditoría.
     * </p>
     * <p>
     * IMPORTANTE: El tenant_id NO se puede modificar. El repositorio garantiza
     * que solo se actualicen usuarios del tenant actual mediante filtrado automático.
     * </p>
     *
     * @param command comando con los datos a actualizar
     * @return UserResult con los datos del usuario actualizado
     * @throws UserNotFoundException si el usuario no existe o no pertenece al tenant actual (HTTP 404)
     * @throws IllegalArgumentException si el rol especificado no es válido (HTTP 400)
     */
    @Transactional
    public UserResult execute(UpdateUserCommand command) {
        // 1. Buscar usuario existente
        // El repositorio aplica filtrado automático por tenant_id del contexto
        User existingUser = userRepository.findById(command.userId())
            .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
        
        // 2. Capturar valores anteriores para auditoría
        String oldValues = formatUserAsJson(existingUser);
        
        // 3. Determinar nuevo rol (mantener el actual si no se especifica)
        Role newRole = (command.role() != null && !command.role().isBlank())
            ? Role.fromString(command.role())
            : existingUser.getRole();
        
        // 4. Construir usuario actualizado usando Builder pattern (inmutabilidad)
        // IMPORTANTE: El tenant_id NO se modifica, se mantiene el original
        User updatedUser = new User.Builder()
            .from(existingUser)
            .name(command.name() != null ? command.name() : existingUser.getName())
            .phone(command.phone() != null ? command.phone() : existingUser.getPhone())
            .role(newRole)
            .updatedAt(java.time.Instant.now())
            .updatedBy(command.updatedBy())
            .build();
        
        // 5. Persistir cambios
        // El repositorio valida que el tenant_id no cambió
        User savedUser = userRepository.save(updatedUser);
        
        // 6. Registrar auditoría de actualización
        auditRepository.logUserUpdate(
            savedUser.getUserId(),
            command.updatedBy(),
            oldValues,
            formatUserAsJson(savedUser)
        );
        
        // 7. Retornar resultado (DTO sin passwordHash)
        return UserResult.fromDomain(savedUser);
    }
    
    /**
     * Formatea los datos del usuario como JSON para auditoría.
     * <p>
     * IMPORTANTE: No incluye el passwordHash por seguridad (Requirement 11.1).
     * </p>
     *
     * @param user la entidad de dominio User
     * @return representación JSON simplificada del usuario
     */
    private String formatUserAsJson(User user) {
        return String.format(
            "{\"userId\":\"%s\",\"email\":\"%s\",\"name\":\"%s\",\"phone\":\"%s\",\"role\":\"%s\",\"status\":\"%s\",\"tenantId\":\"%s\"}",
            user.getUserId(),
            user.getEmail(),
            user.getName(),
            user.getPhone() != null ? user.getPhone() : "",
            user.getRole().getValue(),
            user.getStatus().getValue(),
            user.getTenantId() != null ? user.getTenantId().toString() : "null"
        );
    }
}
