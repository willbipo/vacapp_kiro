package mx.vacapp.users.internal.infrastructure.controllers.mobile;

import mx.vacapp.AbstractIntegrationTest;
import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.UserStatus;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.CreateUserRequest;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.UpdateUserRequest;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.UserListResponse;
import mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos.UserResponse;
import mx.vacapp.users.internal.infrastructure.security.JwtTokenProvider;
import mx.vacapp.users.internal.infrastructure.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración para el CRUD de /api/v1/users usando Testcontainers (MySQL real).
 * <p>
 * Verifica códigos HTTP por escenario y el aislamiento multi-tenant: un usuario
 * autenticado en el tenant A no puede ver ni modificar usuarios del tenant B.
 * </p>
 */
class UserRestControllerIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private UUID tenantA;
    private UUID tenantB;

    @BeforeEach
    void setUp() {
        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private String usersUrl() {
        return "http://localhost:" + port + "/api/v1/users";
    }

    private String userUrl(UUID id) {
        return usersUrl() + "/" + id;
    }

    /** Crea un usuario directamente en el tenant dado y retorna un JWT válido para autenticar como él. */
    private String createAdminAndGetToken(UUID tenantId) {
        TenantContext.setTenantId(tenantId);
        User admin = User.create(
            "admin-" + UUID.randomUUID() + "@example.com", "Tenant Admin", "5551234567",
            passwordEncoder.encode("AdminP@ss1"), Role.ADMIN, tenantId, UUID.randomUUID()
        );
        User saved = userRepository.save(admin);
        return jwtTokenProvider.generateToken(saved);
    }

    private HttpEntity<Object> withAuth(String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void createUser_shouldReturn201WithValidData() {
        String token = createAdminAndGetToken(tenantA);
        CreateUserRequest request = new CreateUserRequest(
            "new-user-" + UUID.randomUUID() + "@example.com", "New User", "5559998888", "ValidP@ss1", "worker"
        );

        ResponseEntity<UserResponse> response = restTemplate.exchange(
            usersUrl(), HttpMethod.POST, withAuth(token, request), UserResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().role()).isEqualTo("worker");
    }

    @Test
    void createUser_shouldReturn409OnDuplicateEmailInSameTenant() {
        String token = createAdminAndGetToken(tenantA);
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        CreateUserRequest request = new CreateUserRequest(email, "Dup User", "5551112222", "ValidP@ss1", "worker");

        restTemplate.exchange(usersUrl(), HttpMethod.POST, withAuth(token, request), UserResponse.class);
        ResponseEntity<UserResponse> secondResponse = restTemplate.exchange(
            usersUrl(), HttpMethod.POST, withAuth(token, request), UserResponse.class
        );

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void getUserById_shouldReturn404WhenUserBelongsToAnotherTenant() {
        String tokenTenantA = createAdminAndGetToken(tenantA);

        TenantContext.setTenantId(tenantB);
        User userInTenantB = userRepository.save(User.create(
            "tenant-b-user-" + UUID.randomUUID() + "@example.com", "Tenant B User", "5553334444",
            passwordEncoder.encode("ValidP@ss1"), Role.WORKER, tenantB, UUID.randomUUID()
        ));

        ResponseEntity<String> response = restTemplate.exchange(
            userUrl(userInTenantB.getUserId()), HttpMethod.GET, withAuth(tokenTenantA, null), String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listUsers_shouldOnlyReturnUsersFromCurrentTenant() {
        String tokenTenantA = createAdminAndGetToken(tenantA);

        TenantContext.setTenantId(tenantB);
        userRepository.save(User.create(
            "isolated-" + UUID.randomUUID() + "@example.com", "Isolated User", "5555556666",
            passwordEncoder.encode("ValidP@ss1"), Role.WORKER, tenantB, UUID.randomUUID()
        ));

        ResponseEntity<UserListResponse> response = restTemplate.exchange(
            usersUrl() + "?page=0&size=50", HttpMethod.GET, withAuth(tokenTenantA, null), UserListResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().users())
            .allSatisfy(user -> assertThat(user.tenantId()).isEqualTo(tenantA));
    }

    @Test
    void updateUser_shouldReturn200AndNotChangeTenantId() {
        String token = createAdminAndGetToken(tenantA);
        User target = userRepository.save(User.create(
            "update-target-" + UUID.randomUUID() + "@example.com", "Original Name", "5551110000",
            passwordEncoder.encode("ValidP@ss1"), Role.WORKER, tenantA, UUID.randomUUID()
        ));

        UpdateUserRequest updateRequest = new UpdateUserRequest(null, "Updated Name", "5559990000", "manager");

        ResponseEntity<UserResponse> response = restTemplate.exchange(
            userUrl(target.getUserId()), HttpMethod.PUT, withAuth(token, updateRequest), UserResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo("Updated Name");
        assertThat(response.getBody().tenantId()).isEqualTo(tenantA);
    }

    @Test
    void deleteUser_shouldReturn204AndMarkUserInactive() {
        String token = createAdminAndGetToken(tenantA);
        User target = userRepository.save(User.create(
            "deactivate-target-" + UUID.randomUUID() + "@example.com", "To Deactivate", "5552220000",
            passwordEncoder.encode("ValidP@ss1"), Role.WORKER, tenantA, UUID.randomUUID()
        ));

        ResponseEntity<Void> response = restTemplate.exchange(
            userUrl(target.getUserId()), HttpMethod.DELETE, withAuth(token, null), Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        TenantContext.setTenantId(tenantA);
        User reloaded = userRepository.findById(target.getUserId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    void listUsers_shouldReturn401WithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(usersUrl(), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
