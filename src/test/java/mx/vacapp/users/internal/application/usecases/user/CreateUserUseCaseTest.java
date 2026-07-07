package mx.vacapp.users.internal.application.usecases.user;

import mx.vacapp.users.internal.application.usecases.commands.CreateUserCommand;
import mx.vacapp.users.internal.application.usecases.commands.UserResult;
import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.exceptions.UserAlreadyExistsException;
import mx.vacapp.users.internal.domain.repository.AuditRepository;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
 * Tests unitarios para CreateUserUseCase con mocks de sus dependencias.
 * <p>
 * Verifica validación de unicidad de email, cifrado de contraseña
 * y asignación de rol por defecto (WORKER).
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private CreateUserUseCase createUserUseCase;

    @BeforeEach
    void setUp() {
        createUserUseCase = new CreateUserUseCase(userRepository, auditRepository, passwordEncoder);
    }

    @Test
    void execute_shouldCreateUserWithEncodedPassword() {
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        CreateUserCommand command = new CreateUserCommand(
            "new@example.com", "New User", "5551234567", "PlainP@ss1", "worker", tenantId, createdBy
        );

        when(userRepository.existsByEmailAndTenantId("new@example.com", tenantId)).thenReturn(false);
        when(passwordEncoder.encode("PlainP@ss1")).thenReturn("$2a$12$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResult result = createUserUseCase.execute(command);

        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.role()).isEqualTo("worker");

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());
        assertThat(savedUserCaptor.getValue().getPasswordHash()).isEqualTo("$2a$12$encoded");

        verify(auditRepository).logUserCreation(any(UUID.class), eq(createdBy), eq(null), anyString());
    }

    @Test
    void execute_shouldDefaultToWorkerRoleWhenRoleNotSpecified() {
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        CreateUserCommand command = new CreateUserCommand(
            "worker@example.com", "Worker User", "5551234567", "PlainP@ss1", null, tenantId, createdBy
        );

        when(userRepository.existsByEmailAndTenantId("worker@example.com", tenantId)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResult result = createUserUseCase.execute(command);

        assertThat(result.role()).isEqualTo(Role.WORKER.getValue());
    }

    @Test
    void execute_shouldThrowUserAlreadyExistsExceptionWhenEmailTaken() {
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        CreateUserCommand command = new CreateUserCommand(
            "duplicate@example.com", "Duplicate User", "5551234567", "PlainP@ss1", "worker", tenantId, createdBy
        );

        when(userRepository.existsByEmailAndTenantId("duplicate@example.com", tenantId)).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> createUserUseCase.execute(command));
        verify(userRepository, never()).save(any());
        verify(auditRepository, never()).logUserCreation(any(), any(), any(), any());
    }

    @Test
    void execute_shouldNormalizeEmailToLowerCase() {
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        CreateUserCommand command = new CreateUserCommand(
            "Mixed.Case@Example.com", "Mixed Case", "5551234567", "PlainP@ss1", "worker", tenantId, createdBy
        );

        when(userRepository.existsByEmailAndTenantId("mixed.case@example.com", tenantId)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResult result = createUserUseCase.execute(command);

        assertThat(result.email()).isEqualTo("mixed.case@example.com");
        verify(userRepository).existsByEmailAndTenantId("mixed.case@example.com", tenantId);
    }
}
