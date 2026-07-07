package mx.vacapp.users.internal.application.usecases.auth;

import mx.vacapp.users.internal.application.usecases.commands.AuthResult;
import mx.vacapp.users.internal.application.usecases.commands.LoginCommand;
import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.UserStatus;
import mx.vacapp.users.internal.domain.model.exceptions.InactiveAccountException;
import mx.vacapp.users.internal.domain.model.exceptions.InvalidCredentialsException;
import mx.vacapp.users.internal.domain.repository.AuditRepository;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import mx.vacapp.users.internal.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para LoginUseCase con mocks de sus dependencias.
 */
@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private LoginUseCase loginUseCase;

    @BeforeEach
    void setUp() {
        loginUseCase = new LoginUseCase(userRepository, auditRepository, passwordEncoder, jwtTokenProvider);
    }

    private User buildUser(UserStatus status) {
        return new User.Builder()
            .userId(UUID.randomUUID())
            .email("test@example.com")
            .name("Test User")
            .phone("1234567890")
            .passwordHash("$2a$12$hashed")
            .role(Role.WORKER)
            .status(status)
            .tenantId(UUID.randomUUID())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID())
            .updatedBy(UUID.randomUUID())
            .build();
    }

    @Test
    void execute_shouldReturnAuthResultOnValidCredentials() {
        User user = buildUser(UserStatus.ACTIVE);
        LoginCommand command = new LoginCommand("test@example.com", "ValidP@ss1", "10.0.0.1", "junit-agent");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("ValidP@ss1", user.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(user)).thenReturn("fake-jwt-token");

        AuthResult result = loginUseCase.execute(command);

        assertThat(result.token()).isEqualTo("fake-jwt-token");
        assertThat(result.userId()).isEqualTo(user.getUserId());
        assertThat(result.email()).isEqualTo(user.getEmail());
        verify(auditRepository).logAuthentication(eq("test@example.com"), eq(true), anyString(), anyString());
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserNotFound() {
        LoginCommand command = new LoginCommand("missing@example.com", "ValidP@ss1", "10.0.0.1", "junit-agent");

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> loginUseCase.execute(command));
        verify(auditRepository).logAuthentication(eq("missing@example.com"), eq(false), anyString(), anyString());
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenPasswordDoesNotMatch() {
        User user = buildUser(UserStatus.ACTIVE);
        LoginCommand command = new LoginCommand("test@example.com", "WrongPassword1!", "10.0.0.1", "junit-agent");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword1!", user.getPasswordHash())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> loginUseCase.execute(command));
        verify(auditRepository).logAuthentication(eq("test@example.com"), eq(false), anyString(), anyString());
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    void execute_shouldThrowInactiveAccountExceptionWhenUserIsInactive() {
        User user = buildUser(UserStatus.INACTIVE);
        LoginCommand command = new LoginCommand("test@example.com", "ValidP@ss1", "10.0.0.1", "junit-agent");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("ValidP@ss1", user.getPasswordHash())).thenReturn(true);

        assertThrows(InactiveAccountException.class, () -> loginUseCase.execute(command));
        verify(auditRepository).logAuthentication(eq("test@example.com"), eq(false), anyString(), anyString());
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    void execute_shouldNormalizeEmailToLowerCaseBeforeLookup() {
        User user = buildUser(UserStatus.ACTIVE);
        LoginCommand command = new LoginCommand("Test@Example.com", "ValidP@ss1", "10.0.0.1", "junit-agent");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.generateToken(user)).thenReturn("token");

        loginUseCase.execute(command);

        verify(userRepository).findByEmail("test@example.com");
    }
}
