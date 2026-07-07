package mx.vacapp.users.internal.application.usecases.user;

import mx.vacapp.users.internal.application.usecases.commands.UserResult;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.exceptions.UserNotFoundException;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso: Obtener usuario por ID.
 * <p>
 * Este caso de uso implementa la lógica de negocio para recuperar los datos
 * de un usuario específico por su identificador UUID, cumpliendo con:
 * </p>
 * <ul>
 *   <li>Filtrado automático por tenant_id (Requirement 5.2)</li>
 *   <li>Retornar datos completos excepto passwordHash (Requirement 6.2)</li>
 *   <li>Lanzar excepción si el usuario no existe o no pertenece al tenant (Requirement 6.3)</li>
 * </ul>
 * <p>
 * El repositorio aplica filtrado automático por tenant_id del contexto de seguridad,
 * garantizando que un usuario solo pueda acceder a datos de usuarios de su mismo tenant
 * (o a todos los tenants si tiene rol SaaS).
 * </p>
 *
 * @see UserResult
 */
@Service
public class GetUserUseCase {
    
    private final UserRepository userRepository;
    
    /**
     * Constructor con inyección de dependencias.
     *
     * @param userRepository repositorio de usuarios
     */
    public GetUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Ejecuta la obtención de un usuario por su ID.
     * <p>
     * El repositorio aplica automáticamente el filtro WHERE tenant_id = :currentTenantId
     * basado en el contexto de seguridad establecido por el JwtAuthenticationFilter.
     * </p>
     * <p>
     * Si el usuario solicitado existe pero pertenece a otro tenant, se lanza
     * UserNotFoundException para prevenir revelación de información.
     * </p>
     *
     * @param userId el UUID del usuario a buscar (no debe ser null)
     * @return UserResult con los datos del usuario (sin passwordHash)
     * @throws UserNotFoundException si el usuario no existe o no pertenece al tenant actual (HTTP 404)
     * @throws IllegalArgumentException si userId es null
     */
    @Transactional(readOnly = true)
    public UserResult execute(UUID userId) {
        // 1. Validar parámetro de entrada
        if (userId == null) {
            throw new IllegalArgumentException("userId no puede ser null");
        }
        
        // 2. Buscar usuario por ID
        // El repositorio aplica filtrado automático por tenant_id del TenantContext
        // Si el usuario existe pero está en otro tenant, retorna Optional.empty()
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
        
        // 3. Retornar resultado (DTO sin passwordHash)
        // UserResult.fromDomain() convierte la entidad de dominio en DTO inmutable
        // Campos incluidos: userId, email, name, phone, role, status, tenantId, createdAt, updatedAt
        return UserResult.fromDomain(user);
    }
}
