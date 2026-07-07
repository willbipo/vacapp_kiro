package mx.vacapp.users.internal.properties;

import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.infrastructure.security.JwtTokenProvider;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 9: SaaS vs Business Role Behavior.
 * <p>
 * Todo usuario con rol SaaS (super_admin, support) debe tener {@code tenantId} nulo
 * tanto en el dominio como en el JWT emitido. Todo usuario con Business_Role debe
 * tener {@code tenantId} no nulo en ambos.
 * </p>
 */
class RoleBehaviorPropertiesTest {

    private JwtTokenProvider newProvider() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret",
            "role-behavior-property-test-secret-at-least-256-bits-32-bytes");
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 86_400_000L);
        ReflectionTestUtils.setField(provider, "jwtIssuer", "vacapp-property-test");
        return provider;
    }

    @Property
    void saasRolesAlwaysHaveNullTenantInDomainAndToken(@ForAll("saasRoles") Role saasRole) {
        User user = User.create(
            "saas-test@example.com", "SaaS User", "5551234567", "hash", saasRole, null, UUID.randomUUID()
        );

        assertThat(user.getTenantId()).isNull();
        assertThat(user.isSaaSUser()).isTrue();
        assertThat(user.hasSaaSRole()).isTrue();

        JwtTokenProvider provider = newProvider();
        String token = provider.generateToken(user);
        assertThat(provider.extractTenantId(token)).isNull();
    }

    @Property
    void businessRolesAlwaysHaveNonNullTenantInDomainAndToken(
        @ForAll("businessRoles") Role businessRole,
        @ForAll("tenantIds") UUID tenantId
    ) {
        User user = User.create(
            "business-test@example.com", "Business User", "5551234567", "hash", businessRole, tenantId, UUID.randomUUID()
        );

        assertThat(user.getTenantId()).isEqualTo(tenantId);
        assertThat(user.isSaaSUser()).isFalse();
        assertThat(user.hasSaaSRole()).isFalse();

        JwtTokenProvider provider = newProvider();
        String token = provider.generateToken(user);
        assertThat(provider.extractTenantId(token)).isEqualTo(tenantId);
    }

    @Provide
    Arbitrary<Role> saasRoles() {
        return Arbitraries.of(Role.SUPER_ADMIN, Role.SUPPORT);
    }

    @Provide
    Arbitrary<Role> businessRoles() {
        return Arbitraries.of(Role.ADMIN, Role.MANAGER, Role.VETERINARIAN, Role.WORKER);
    }

    @Provide
    Arbitrary<UUID> tenantIds() {
        return Arbitraries.randoms().map(random -> UUID.randomUUID());
    }
}
