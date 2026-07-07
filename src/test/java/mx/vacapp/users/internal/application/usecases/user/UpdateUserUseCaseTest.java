package mx.vacapp.users.internal.application.usecases.user;

import mx.vacapp.users.internal.application.usecases.commands.UpdateUserCommand;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para UpdateUserUseCase con mocks de sus dependencias.
 */
@ExtendWith(MockitoExtension.class)
class UpdateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditRepository auditRepository;

    private UpdateUserUseCase updateUserUseCase;

    @BeforeEach
    void setUp() {
        updateUserUseCase = new UpdateUserUseCase(userRepository, auditRepository);
    }

    private User buildExistingUser(UUID tenantId) {
        return new User.Builder()
            .userId(UUID.randomUUID())
            .email("existing@example.com")
            .name("Original Name")
            .phone("1111111111")
            .passwordHash("$2a$12$hash")
            .role(Role.WORKER)
            .status(UserStatus.ACTIVE)
            .tenantId(tenantId)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID())
            .updatedBy(UUID.randomUUID())
            .build();
    }

    @Test
    void execute_shouldUpdateNamePhoneAndRole() {
        User existingUser = buildExistingUser(UUID.randomUUID());
        UUID updatedBy = UUID.randomUUID();
        UpdateUserCommand command = new UpdateUserCommand(
            existingUser.getUserId(), "New Name", "9999999999", "manager", updatedBy
        );

        when(userRepository.findById(existingUser.getUserId())).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResult result = updateUserUseCase.execute(command);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.phone()).isEqualTo("9999999999");
        assertThat(result.role()).isEqualTo("manager");

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getTenantId()).isEqualTo(existingUser.getTenantId());

        verify(auditRepository).logUserUpdate(eq(existingUser.getUserId()), any(), anyString(), anyString());
    }

    @Test
    void execute_shouldNotChangeTenantId() {
        UUID originalTenantId = UUID.randomUUID();
        User existingUser = buildExistingUser(originalTenantId);
        UpdateUserCommand command = new UpdateUserCommand(
            existingUser.getUserId(), "New Name", null, null, UUID.randomUUID()
        );

        when(userRepository.findById(existingUser.getUserId())).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResult result = updateUserUseCase.execute(command);

        assertThat(result.tenantId()).isEqualTo(originalTenantId);
    }

    @Test
    void execute_shouldKeepExistingFieldsWhenNotProvided() {
        User existingUser = buildExistingUser(UUID.randomUUID());
        UpdateUserCommand command = new UpdateUserCommand(
            existingUser.getUserId(), null, null, null, UUID.randomUUID()
        );

        when(userRepository.findById(existingUser.getUserId())).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResult result = updateUserUseCase.execute(command);

        assertThat(result.name()).isEqualTo(existingUser.getName());
        assertThat(result.phone()).isEqualTo(existingUser.getPhone());
        assertThat(result.role()).isEqualTo(existingUser.getRole().getValue());
    }

    @Test
    void execute_shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        UUID missingUserId = UUID.randomUUID();
        UpdateUserCommand command = new UpdateUserCommand(missingUserId, "Name", "123", "worker", UUID.randomUUID());

        when(userRepository.findById(missingUserId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> updateUserUseCase.execute(command));
        verify(userRepository, never()).save(any());
        verify(auditRepository, never()).logUserUpdate(any(), any(), any(), any());
    }
}
