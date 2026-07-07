package mx.vacapp.users.internal.application.usecases.user;

import mx.vacapp.users.internal.application.usecases.commands.UserResult;
import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.exceptions.UserNotFoundException;
import mx.vacapp.users.internal.domain.repository.AuditRepository;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import mx.vacapp.users.internal.infrastructure.security.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso: Cambiar rol de usuario.
 * <p>
 * Este caso de uso implementa la lógica de negocio para modificar el rol
 * de un usuario existente, cumpliendo con los siguientes requisitos:
 * </p>
 * <ul>
 *   <li>Validar permisos según el rol del usuario autenticado (Requirement 3.4, 3.5)</li>
 *   <li>Verificar que el tenant_id coincide para Business_Roles (Requirement 3.4)</li>
 *   <li>Permitir a super_admin asignar cualquier rol sin restricción (Requirement 3.6)</li>
 *   <li>Registrar auditoría del cambio de rol (Requirement 3.8)</li>
 * </ul>
 * <p>
 * <strong>Matriz de permisos:</strong>
 * </p>
 * <ul>
 *   <li>super_admin: puede asignar cualquier rol a cualquier usuario en cualquier tenant</li>
 *   <li>admin: puede asignar Business_Roles (admin, manager, veterinarian, worker) solo en su tenant</li>
 *   <li>support: solo lectura, no puede cambiar roles</li>
 *   <li>manager, veterinarian, worker: no pueden cambiar roles</li>
 * </ul>
 *
 * @see UserResult
 */
@Service
public class ChangeUserRoleUseCase {
    
    private final UserRepository userRepository;
    private final AuditRepository auditRepository;
    
    /**
     * Constructor con inyección de dependencias.
     *
     * @param userRepository repositorio de usuarios
     * @param auditRepository repositorio de auditoría
     */
    public ChangeUserRoleUseCase(
            UserRepository userRepository,
            AuditRepository auditRepository) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
    }
    
    /**
     * Ejecuta el cambio de rol de un usuario existente.
     * <p>
     * Valida permisos del usuario autenticado, verifica aislamiento multi-tenant,
     * aplica el cambio de rol, persiste y registra auditoría.
     * </p>
     *
     * @param userId el UUID del usuario cuyo rol se va a cambiar (no debe ser null)
     * @param newRoleValue el nuevo rol a asignar en formato string (ej: "admin", "worker")
     * @param changedBy el UUID del usuario que realiza el cambio (no debe ser null)
     * @param authenticatedUserRole el rol del usuario autenticado (para validación de permisos)
     * @return UserResult con los datos del usuario con el rol actualizado
     * @throws UserNotFoundException si el usuario no existe o no pertenece al tenant actual (HTTP 404)
     * @throws AccessDeniedException si el usuario autenticado no tiene permisos (HTTP 403)
     * @throws IllegalArgumentException si los parámetros son inválidos o el rol no existe (HTTP 400)
     */
    @Transactional
    public UserResult execute(UUID userId, String newRoleValue, UUID changedBy, Role authenticatedUserRole) {
        // 1. Validar parámetros de entrada
        if (userId == null) {
            throw new IllegalArgumentException("userId no puede ser null");
        }
        
        if (changedBy == null) {
            throw new IllegalArgumentException("changedBy no puede ser null");
        }
        
        if (newRoleValue == null || newRoleValue.isBlank()) {
            throw new IllegalArgumentException("El nuevo rol no puede ser null o vacío");
        }
        
        if (authenticatedUserRole == null) {
            throw new IllegalArgumentException("authenticatedUserRole no puede ser null");
        }
        
        // 2. Parsear nuevo rol (lanza IllegalArgumentException si no es válido)
        Role newRole = Role.fromString(newRoleValue);
        
        // 3. Validar permisos del usuario autenticado
        validatePermissions(authenticatedUserRole, newRole);
        
        // 4. Buscar usuario existente
        // El repositorio aplica filtrado automático por tenant_id del contexto
        User existingUser = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
        
        // 5. Validar aislamiento multi-tenant para Business_Roles
        // admin solo puede asignar roles a usuarios de su mismo tenant
        if (authenticatedUserRole == Role.ADMIN) {
            UUID currentTenantId = TenantContext.getTenantId();
            if (currentTenantId == null || !currentTenantId.equals(existingUser.getTenantId())) {
                throw new AccessDeniedException("No puede asignar roles a usuarios de otro tenant");
            }
        }
        
        // 6. Capturar valores anteriores para auditoría
        String oldRoleValue = existingUser.getRole().getValue();
        
        // 7. Cambiar rol (crea copia inmutable con nuevo rol)
        // El método changeRole() del dominio actualiza role, updatedAt y updatedBy
        User updatedUser = existingUser.changeRole(newRole, changedBy);
        
        // 8. Persistir cambios
        User savedUser = userRepository.save(updatedUser);
        
        // 9. Registrar auditoría del cambio de rol
        // Formato JSON simplificado con oldValue y newValue del rol
        String oldValues = String.format("{\"role\":\"%s\"}", oldRoleValue);
        String newValues = String.format("{\"role\":\"%s\"}", newRole.getValue());
        
        auditRepository.logUserUpdate(
            savedUser.getUserId(),
            changedBy,
            oldValues,
            newValues
        );
        
        // 10. Retornar resultado (DTO sin passwordHash)
        return UserResult.fromDomain(savedUser);
    }
    
    /**
     * Valida que el usuario autenticado tiene permisos para asignar el rol solicitado.
     * <p>
     * Reglas de permisos:
     * </p>
     * <ul>
     *   <li>super_admin: puede asignar cualquier rol (sin restricción)</li>
     *   <li>admin: puede asignar Business_Roles (admin, manager, veterinarian, worker)</li>
     *   <li>support: solo lectura, NO puede asignar roles</li>
     *   <li>manager, veterinarian, worker: NO pueden asignar roles</li>
     * </ul>
     *
     * @param authenticatedUserRole el rol del usuario autenticado
     * @param newRole el nuevo rol que se intenta asignar
     * @throws AccessDeniedException si el usuario no tiene permisos para asignar el rol
     */
    private void validatePermissions(Role authenticatedUserRole, Role newRole) {
        // super_admin tiene permisos totales
        if (authenticatedUserRole == Role.SUPER_ADMIN) {
            return; // permitir cualquier asignación
        }
        
        // admin puede asignar Business_Roles (pero no SaaS_Roles)
        if (authenticatedUserRole == Role.ADMIN) {
            if (newRole.isSaaSRole()) {
                throw new AccessDeniedException(
                    "No tiene permisos para asignar roles SaaS (super_admin, support)"
                );
            }
            return; // permitir asignación de Business_Roles
        }
        
        // support, manager, veterinarian, worker NO pueden asignar roles
        throw new AccessDeniedException(
            String.format("El rol %s no tiene permisos para cambiar roles de usuarios", 
                authenticatedUserRole.getValue())
        );
    }
}
