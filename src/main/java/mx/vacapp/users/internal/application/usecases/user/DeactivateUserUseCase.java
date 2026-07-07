package mx.vacapp.users.internal.application.usecases.user;

import mx.vacapp.users.internal.application.usecases.commands.UserResult;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.exceptions.UserNotFoundException;
import mx.vacapp.users.internal.domain.repository.AuditRepository;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso: Desactivar usuario (soft delete).
 * <p>
 * Este caso de uso implementa la lógica de negocio para desactivar lógicamente
 * un usuario, cumpliendo con los siguientes requisitos:
 * </p>
 * <ul>
 *   <li>Marcar usuario como INACTIVE sin eliminar el registro (Requirement 6.6)</li>
 *   <li>Validar permisos de acceso basados en tenant (Requirement 5.2)</li>
 *   <li>Registrar auditoría de la desactivación con motivo (Requirement 12.5)</li>
 *   <li>Usuario inactivo no puede autenticarse (Requirement 6.7)</li>
 * </ul>
 * <p>
 * La desactivación es una operación de eliminación lógica (soft delete). El registro
 * permanece en la base de datos para trazabilidad y cumplimiento normativo, pero
 * el usuario no puede autenticarse ni usar el sistema.
 * </p>
 * <p>
 * NO se realizan deletes físicos (DELETE FROM users) bajo ninguna circunstancia.
 * </p>
 *
 * @see UserResult
 */
@Service
public class DeactivateUserUseCase {
    
    private final UserRepository userRepository;
    private final AuditRepository auditRepository;
    
    /**
     * Constructor con inyección de dependencias.
     *
     * @param userRepository repositorio de usuarios
     * @param auditRepository repositorio de auditoría
     */
    public DeactivateUserUseCase(
            UserRepository userRepository,
            AuditRepository auditRepository) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
    }
    
    /**
     * Ejecuta la desactivación lógica de un usuario.
     * <p>
     * Valida que el usuario existe y pertenece al tenant actual, marca su estado
     * como INACTIVE, persiste el cambio y registra la auditoría con el motivo.
     * </p>
     * <p>
     * IMPORTANTE: El registro NO se elimina de la base de datos. Solo se actualiza
     * el campo status a 'INACTIVE' y se registra la operación en users_audit.
     * </p>
     *
     * @param userId el UUID del usuario a desactivar (no debe ser null)
     * @param deactivatedBy el UUID del usuario que realiza la desactivación (no debe ser null)
     * @param reason motivo textual de la desactivación (opcional, max 500 caracteres)
     * @return UserResult con los datos del usuario desactivado
     * @throws UserNotFoundException si el usuario no existe o no pertenece al tenant actual (HTTP 404)
     * @throws IllegalArgumentException si userId o deactivatedBy son null
     */
    @Transactional
    public UserResult execute(UUID userId, UUID deactivatedBy, String reason) {
        // 1. Validar parámetros de entrada
        if (userId == null) {
            throw new IllegalArgumentException("userId no puede ser null");
        }
        
        if (deactivatedBy == null) {
            throw new IllegalArgumentException("deactivatedBy no puede ser null");
        }
        
        // 2. Buscar usuario existente
        // El repositorio aplica filtrado automático por tenant_id del contexto
        User existingUser = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
        
        // 3. Desactivar usuario (crea copia inmutable con status INACTIVE)
        // El método deactivate() del dominio actualiza status, updatedAt y updatedBy
        User deactivatedUser = existingUser.deactivate(deactivatedBy);
        
        // 4. Persistir cambios
        // El repositorio actualiza el registro existente (UPDATE, NO DELETE)
        User savedUser = userRepository.save(deactivatedUser);
        
        // 5. Registrar auditoría de desactivación con motivo
        // El motivo permite rastrear el contexto de la decisión (ej: "Fin de contrato")
        String auditReason = (reason != null && !reason.isBlank()) 
            ? reason 
            : "Sin motivo especificado";
        
        auditRepository.logUserDeactivation(
            savedUser.getUserId(),
            deactivatedBy,
            auditReason
        );
        
        // 6. Retornar resultado (DTO sin passwordHash)
        return UserResult.fromDomain(savedUser);
    }
    
    /**
     * Sobrecarga del método execute sin motivo explícito.
     * <p>
     * Registra la desactivación con motivo por defecto "Sin motivo especificado".
     * </p>
     *
     * @param userId el UUID del usuario a desactivar
     * @param deactivatedBy el UUID del usuario que realiza la desactivación
     * @return UserResult con los datos del usuario desactivado
     * @throws UserNotFoundException si el usuario no existe o no pertenece al tenant actual
     * @throws IllegalArgumentException si userId o deactivatedBy son null
     */
    @Transactional
    public UserResult execute(UUID userId, UUID deactivatedBy) {
        return execute(userId, deactivatedBy, null);
    }
}
