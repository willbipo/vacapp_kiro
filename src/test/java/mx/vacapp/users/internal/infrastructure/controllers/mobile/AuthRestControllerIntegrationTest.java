package mx.vacapp.users.internal.infrastructure.controllers.mobile;

import mx.vacapp.AbstractIntegrationTest;
import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.ErrorResponse;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.LoginRequest;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.LoginResponse;
import mx.vacapp.users.internal.infrastructure.security.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración para POST /api/v1/auth/login usando Testcontainers (MySQL real).
 * <p>
 * Cubre los escenarios: login exitoso, credenciales inválidas, cuenta inactiva
 * y cuenta bloqueada, verificando los códigos HTTP correctos en cada caso.
 * </p>
 */
class AuthRestControllerIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1/auth/login";
    }

    private User createUser(String email, String password, mx.vacapp.users.internal.domain.model.UserStatus status) {
        User user = User.create(email, "Integration Test User", "5551234567",
            passwordEncoder.encode(password), Role.WORKER, tenantId, UUID.randomUUID());
        if (status != mx.vacapp.users.internal.domain.model.UserStatus.ACTIVE) {
            user = new User.Builder().from(user).status(status).build();
        }
        return userRepository.save(user);
    }

    @Test
    void login_shouldReturn200AndTokenOnValidCredentials() {
        String email = "valid-login-" + UUID.randomUUID() + "@example.com";
        createUser(email, "ValidP@ss1", mx.vacapp.users.internal.domain.model.UserStatus.ACTIVE);

        LoginRequest request = new LoginRequest(email, "ValidP@ss1");
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(baseUrl(), request, LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().email()).isEqualTo(email.toLowerCase());
    }

    @Test
    void login_shouldReturn401OnInvalidPassword() {
        String email = "invalid-pass-" + UUID.randomUUID() + "@example.com";
        createUser(email, "ValidP@ss1", mx.vacapp.users.internal.domain.model.UserStatus.ACTIVE);

        LoginRequest request = new LoginRequest(email, "WrongPassword1!");
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(baseUrl(), request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_shouldReturn401ForNonExistentEmail() {
        LoginRequest request = new LoginRequest("does-not-exist-" + UUID.randomUUID() + "@example.com", "ValidP@ss1");
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(baseUrl(), request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_shouldReturn403ForInactiveAccount() {
        String email = "inactive-" + UUID.randomUUID() + "@example.com";
        createUser(email, "ValidP@ss1", mx.vacapp.users.internal.domain.model.UserStatus.INACTIVE);

        LoginRequest request = new LoginRequest(email, "ValidP@ss1");
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(baseUrl(), request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void login_shouldReturn403ForLockedAccount() {
        String email = "locked-" + UUID.randomUUID() + "@example.com";
        createUser(email, "ValidP@ss1", mx.vacapp.users.internal.domain.model.UserStatus.LOCKED);

        LoginRequest request = new LoginRequest(email, "ValidP@ss1");
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(baseUrl(), request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void login_shouldReturn400OnMissingPassword() {
        // password null viola @NotNull en LoginRequest -> 400
        String body = "{\"email\":\"missing-password@example.com\"}";
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        var entity = new org.springframework.http.HttpEntity<>(body, headers);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(baseUrl(), entity, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
