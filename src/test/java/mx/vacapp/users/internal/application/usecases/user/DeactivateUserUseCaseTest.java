package mx.vacapp.users.internal.application.usecases.user;

import mx.vacapp.users.internal.application.usecases.commands.UserResult;
import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.UserStatus;
import mx.vacapp.users.internal.domain.model.exceptions.UserNotFoundException;
import mx.vacapp.users.internal.domain.repository.AuditRepository;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para DeactivateUserUseCase con mocks de sus dependencias.
 */
@ExtendWith(MockitoExtension.class)
class DeactivateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditRepository auditRepository;

    private DeactivateUserUseCase deactivateUserUseCase;

    @BeforeEach
    void setUp() {
        deactivateUserUseCase = new DeactivateUserUseCase(userRepository, auditRepository);
    }

    private User buildActiveUser() {
        return new User.Builder()
            .userId(UUID.randomUUID())
            .email("active@example.com")
            .name("Active User")
            .phone("1234567890")
            .passwordHash("$2a$12$hash")
            .role(Role.WORKER)
            .status(UserStatus.ACTIVE)
            .tenantId(UUID.randomUUID())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID())
            .updatedBy(UUID.randomUUID())
            .build();
    }

    @Test
    void execute_shouldMarkUserAsInactive() {
        User user = buildActiveUser();
        UUID deactivatedBy = UUID.randomUUID();

        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResult result = deactivateUserUseCase.execute(user.getUserId(), deactivatedBy, "Fin de contrato");

        assertThat(result.status()).isEqualTo(UserStatus.INACTIVE.name());

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getStatus()).isEqualTo(UserStatus.INACTIVE);

        verify(auditRepository).logUserDeactivation(eq(user.getUserId()), eq(deactivatedBy), eq("Fin de contrato"));
    }

    @Test
    void execute_shouldUseDefaultReasonWhenNotProvided() {
        User user = buildActiveUser();
        UUID deactivatedBy = UUID.randomUUID();

        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        deactivateUserUseCase.execute(user.getUserId(), deactivatedBy);

        verify(auditRepository).logUserDeactivation(eq(user.getUserId()), eq(deactivatedBy), eq("Sin motivo especificado"));
    }

    @Test
    void execute_shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        UUID missingUserId = UUID.randomUUID();
        when(userRepository.findById(missingUserId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
            () -> deactivateUserUseCase.execute(missingUserId, UUID.randomUUID()));

        verify(userRepository, never()).save(any());
        verify(auditRepository, never()).logUserDeactivation(any(), any(), any());
    }

    @Test
    void execute_shouldThrowIllegalArgumentExceptionWhenUserIdIsNull() {
        assertThrows(IllegalArgumentException.class,
            () -> deactivateUserUseCase.execute(null, UUID.randomUUID()));
    }

    @Test
    void execute_shouldThrowIllegalArgumentExceptionWhenDeactivatedByIsNull() {
        assertThrows(IllegalArgumentException.class,
            () -> deactivateUserUseCase.execute(UUID.randomUUID(), null));
    }
}
