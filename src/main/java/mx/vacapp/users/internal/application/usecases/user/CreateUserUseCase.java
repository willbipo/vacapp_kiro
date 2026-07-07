package mx.vacapp.users.internal.application.usecases.user;

import mx.vacapp.users.internal.application.usecases.commands.CreateUserCommand;
import mx.vacapp.users.internal.application.usecases.commands.UserResult;
import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.exceptions.UserAlreadyExistsException;
import mx.vacapp.users.internal.domain.repository.AuditRepository;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Crear nuevo usuario.
 * <p>
 * Este caso de uso implementa la lógica de negocio para registrar un nuevo usuario
 * en el sistema, cumpliendo con los siguientes requisitos:
 * </p>
 * <ul>
 *   <li>Validar unicidad del email dentro del tenant (Requirement 2.2)</li>
 *   <li>Cifrar contraseña con BCrypt (Requirement 2.6, 1.4)</li>
 *   <li>Asignar rol WORKER por defecto si no se especifica (Requirement 2.4)</li>
 *   <li>Registrar auditoría de la creación (Requirement 2.9, 12.2)</li>
 * </ul>
 * <p>
 * El email se normaliza a minúsculas y se permite el mismo email en diferentes
 * tenants (Requirement 2.3).
 * </p>
 *
 * @see CreateUserCommand
 * @see UserResult
 */
@Service
public class CreateUserUseCase {
    
    private final UserRepository userRepository;
    private final AuditRepository auditRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Constructor con inyección de dependencias.
     *
     * @param userRepository repositorio de usuarios
     * @param auditRepository repositorio de auditoría
     * @param passwordEncoder encoder BCrypt para cifrar contraseñas
     */
    public CreateUserUseCase(
            UserRepository userRepository,
            AuditRepository auditRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Ejecuta la creación de un nuevo usuario.
     * <p>
     * Valida la unicidad del email, cifra la contraseña, crea la entidad de dominio
     * con los valores apropiados, la persiste y registra la auditoría.
     * </p>
     *
     * @param command comando con los datos del usuario a crear
     * @return UserResult con los datos del usuario creado (sin passwordHash)
     * @throws UserAlreadyExistsException si el email ya existe en el tenant (HTTP 409)
     * @throws IllegalArgumentException si el rol especificado no es válido (HTTP 400)
     */
    @Transactional
    public UserResult execute(CreateUserCommand command) {
        // 1. Validar unicidad del email en el tenant (case-insensitive)
        String email = command.email().toLowerCase();
        if (userRepository.existsByEmailAndTenantId(email, command.tenantId())) {
            throw new UserAlreadyExistsException("Email ya registrado en esta organización");
        }
        
        // 2. Cifrar contraseña con BCrypt (strength 12)
        String passwordHash = passwordEncoder.encode(command.password());
        
        // 3. Determinar rol (usar WORKER por defecto si no se especifica o es null)
        Role role = (command.role() != null && !command.role().isBlank()) 
            ? Role.fromString(command.role()) 
            : Role.WORKER;
        
        // 4. Crear entidad de dominio con factory method
        // El método User.create() genera UUID, establece estado ACTIVE,
        // normaliza email a minúsculas y asigna timestamps actuales
        User user = User.create(
            email,
            command.name(),
            command.phone(),
            passwordHash,
            role,
            command.tenantId(),
            command.createdBy()
        );
        
        // 5. Persistir usuario en la base de datos
        // El repositorio aplica filtrado automático por tenant_id
        User savedUser = userRepository.save(user);
        
        // 6. Registrar auditoría de creación
        // Registra: userId, createdBy, oldValues (null), newValues (JSON del usuario)
        auditRepository.logUserCreation(
            savedUser.getUserId(),
            command.createdBy(),
            null, // oldValues: null para creaciones
            formatUserAsJson(savedUser) // newValues
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
        // Implementación simplificada - en producción usar Jackson o biblioteca JSON
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
