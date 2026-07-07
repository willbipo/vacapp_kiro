package mx.vacapp.users.internal;

import mx.vacapp.users.UsersService;
import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementación de la API pública {@link UsersService}.
 * <p>
 * Delega directamente en {@link UserRepository} (puerto de dominio) sin exponer
 * ninguna clase de {@code internal/} a otros módulos. Esta clase se registra como
 * bean mediante {@code UsersModuleConfig}.
 * </p>
 */
public class UsersServiceImpl implements UsersService {

    private final UserRepository userRepository;

    public UsersServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isUserActive(UUID userId) {
        return findUser(userId)
            .map(User::isActive)
            .orElse(false);
    }

    @Override
    public UUID getUserTenantId(UUID userId) {
        return findUser(userId)
            .map(User::getTenantId)
            .orElse(null);
    }

    @Override
    public boolean hasRole(UUID userId, String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        return findUser(userId)
            .map(user -> user.getRole() == Role.fromString(role))
            .orElse(false);
    }

    private Optional<User> findUser(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId);
    }
}
