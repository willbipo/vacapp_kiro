package mx.vacapp.users.internal.properties;

import mx.vacapp.users.internal.application.usecases.commands.CreateUserCommand;
import mx.vacapp.users.internal.application.usecases.commands.UpdateUserCommand;
import mx.vacapp.users.internal.application.usecases.user.CreateUserUseCase;
import mx.vacapp.users.internal.application.usecases.user.DeactivateUserUseCase;
import mx.vacapp.users.internal.application.usecases.user.UpdateUserUseCase;
import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.UserStatus;
import mx.vacapp.users.internal.domain.repository.AuditRepository;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Property 7: Audit Trail Completeness.
 * <p>
 * Para toda operación CREATE, UPDATE o DEACTIVATE ejecutada exitosamente sobre un
 * usuario, el caso de uso correspondiente debe invocar exactamente una vez al
 * método de auditoría apropiado, con el {@code userId} afectado y el responsable
 * ({@code modifiedBy}) correctos.
 * </p>
 */
class AuditTrailPropertiesTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @Property
    void createAlwaysLogsCreationWithNullOldValues(@ForAll("roles") Role role) {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        AuditRepository auditRepository = Mockito.mock(AuditRepository.class);
        CreateUserUseCase useCase = new CreateUserUseCase(userRepository, auditRepository, ENCODER);

        UUID tenantId = role.isSaaSRole() ? null : UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        when(userRepository.existsByEmailAndTenantId(anyString(), any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateUserCommand command = new CreateUserCommand(
            "audit-create@example.com", "Audit User", "5551234567", "ValidP@ss1", role.getValue(), tenantId, createdBy
        );

        var result = useCase.execute(command);

        verify(auditRepository, times(1))
            .logUserCreation(eq(result.userId()), eq(createdBy), isNull(), anyString());
    }

    @Property
    void updateAlwaysLogsUpdateWithNonNullOldAndNewValues(@ForAll("roles") Role role) {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        AuditRepository auditRepository = Mockito.mock(AuditRepository.class);
        UpdateUserUseCase useCase = new UpdateUserUseCase(userRepository, auditRepository);

        User existingUser = buildUser(role);
        UUID updatedBy = UUID.randomUUID();

        when(userRepository.findById(existingUser.getUserId())).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserCommand command = new UpdateUserCommand(
            existingUser.getUserId(), "Updated Name", "5559999999", null, updatedBy
        );
        useCase.execute(command);

        verify(auditRepository, times(1))
            .logUserUpdate(eq(existingUser.getUserId()), eq(updatedBy), anyString(), anyString());
    }

    @Property
    void deactivateAlwaysLogsDeactivationExactlyOnce(@ForAll("roles") Role role) {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        AuditRepository auditRepository = Mockito.mock(AuditRepository.class);
        DeactivateUserUseCase useCase = new DeactivateUserUseCase(userRepository, auditRepository);

        User existingUser = buildUser(role);
        UUID deactivatedBy = UUID.randomUUID();

        when(userRepository.findById(existingUser.getUserId())).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(existingUser.getUserId(), deactivatedBy, "Property test");

        verify(auditRepository, times(1))
            .logUserDeactivation(eq(existingUser.getUserId()), eq(deactivatedBy), eq("Property test"));
    }

    private User buildUser(Role role) {
        return new User.Builder()
            .userId(UUID.randomUUID())
            .email("audit-test@example.com")
            .name("Audit Test User")
            .phone("5551234567")
            .passwordHash("hash")
            .role(role)
            .status(UserStatus.ACTIVE)
            .tenantId(role.isSaaSRole() ? null : UUID.randomUUID())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID())
            .updatedBy(UUID.randomUUID())
            .build();
    }

    @Provide
    Arbitrary<Role> roles() {
        return Arbitraries.of(Role.values());
    }
}
