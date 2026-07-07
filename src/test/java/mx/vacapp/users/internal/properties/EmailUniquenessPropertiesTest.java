package mx.vacapp.users.internal.properties;

import mx.vacapp.users.internal.application.usecases.commands.CreateUserCommand;
import mx.vacapp.users.internal.application.usecases.user.CreateUserUseCase;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.exceptions.UserAlreadyExistsException;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property 2: Email Uniqueness Within Tenant.
 * <p>
 * Para un mismo email dentro del mismo tenant, solo el primer intento de creación
 * tiene éxito; los intentos subsecuentes deben fallar con
 * {@link UserAlreadyExistsException}. El mismo email en tenants distintos no genera
 * conflicto.
 * </p>
 */
class EmailUniquenessPropertiesTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4); // strength baja solo para tests rápidos

    /**
     * Repositorio en memoria que simula la restricción de unicidad (email, tenant_id)
     * sin requerir una base de datos real, suficiente para validar la propiedad de negocio.
     */
    private static class InMemoryUserRepository {
        private final Map<String, User> usersByEmailAndTenant = new HashMap<>();

        boolean existsByEmailAndTenantId(String email, UUID tenantId) {
            return usersByEmailAndTenant.containsKey(key(email, tenantId));
        }

        User save(User user) {
            usersByEmailAndTenant.put(key(user.getEmail(), user.getTenantId()), user);
            return user;
        }

        private String key(String email, UUID tenantId) {
            return email.toLowerCase() + "|" + tenantId;
        }
    }

    @Property
    void onlyFirstUserWithSameEmailInSameTenantSucceeds(
        @ForAll("emailLocalParts") String localPart
    ) {
        InMemoryUserRepository fakeStore = new InMemoryUserRepository();
        UUID tenantId = UUID.randomUUID();
        String email = localPart + "@example.com";

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        AuditRepository auditRepository = Mockito.mock(AuditRepository.class);

        Mockito.when(userRepository.existsByEmailAndTenantId(Mockito.anyString(), Mockito.any()))
            .thenAnswer(inv -> fakeStore.existsByEmailAndTenantId(inv.getArgument(0), inv.getArgument(1)));
        Mockito.when(userRepository.save(Mockito.any(User.class)))
            .thenAnswer(inv -> fakeStore.save(inv.getArgument(0)));

        CreateUserUseCase useCase = new CreateUserUseCase(userRepository, auditRepository, ENCODER);

        CreateUserCommand firstAttempt = new CreateUserCommand(
            email, "First User", "5551234567", "ValidP@ss1", "worker", tenantId, UUID.randomUUID()
        );
        var firstResult = useCase.execute(firstAttempt);
        assertThat(firstResult.email()).isEqualTo(email.toLowerCase());

        CreateUserCommand secondAttempt = new CreateUserCommand(
            email.toUpperCase(), "Second User", "5559876543", "ValidP@ss2", "worker", tenantId, UUID.randomUUID()
        );
        assertThatThrownBy(() -> useCase.execute(secondAttempt))
            .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Property
    void sameEmailInDifferentTenantsDoesNotConflict(
        @ForAll("emailLocalParts") String localPart
    ) {
        InMemoryUserRepository fakeStore = new InMemoryUserRepository();
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        String email = localPart + "@example.com";

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        AuditRepository auditRepository = Mockito.mock(AuditRepository.class);

        Mockito.when(userRepository.existsByEmailAndTenantId(Mockito.anyString(), Mockito.any()))
            .thenAnswer(inv -> fakeStore.existsByEmailAndTenantId(inv.getArgument(0), inv.getArgument(1)));
        Mockito.when(userRepository.save(Mockito.any(User.class)))
            .thenAnswer(inv -> fakeStore.save(inv.getArgument(0)));

        CreateUserUseCase useCase = new CreateUserUseCase(userRepository, auditRepository, ENCODER);

        useCase.execute(new CreateUserCommand(email, "Tenant A User", "5551111111", "ValidP@ss1", "worker", tenantA, UUID.randomUUID()));

        // No debe lanzar excepción al usar el mismo email en un tenant diferente
        var resultTenantB = useCase.execute(
            new CreateUserCommand(email, "Tenant B User", "5552222222", "ValidP@ss1", "worker", tenantB, UUID.randomUUID())
        );

        assertThat(resultTenantB.tenantId()).isEqualTo(tenantB);
    }

    @Provide
    Arbitrary<String> emailLocalParts() {
        return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(3).ofMaxLength(20);
    }
}
