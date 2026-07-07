package mx.vacapp.users.internal.properties;

import io.jsonwebtoken.Claims;
import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.infrastructure.security.JwtTokenProvider;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 1: Round-trip JWT Token Validation.
 * <p>
 * Para todo usuario generado aleatoriamente (SaaS o de negocio), el token JWT emitido
 * debe poder validarse y sus claims extraídos deben coincidir exactamente con los
 * datos originales del usuario (round-trip sin pérdida de información).
 * </p>
 */
class JwtTokenPropertiesTest {

    private JwtTokenProvider newProvider() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret",
            "property-test-secret-key-at-least-256-bits-32-bytes-for-hmac");
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 86_400_000L);
        ReflectionTestUtils.setField(provider, "jwtIssuer", "vacapp-property-test");
        return provider;
    }

    @Property
    void tokenRoundTripPreservesUserClaims(
        @ForAll("roles") Role role,
        @ForAll @StringLength(min = 3, max = 40) @AlphaChars String localPart
    ) {
        JwtTokenProvider provider = newProvider();
        String email = (localPart + "@example.com").toLowerCase();
        UUID tenantId = role.isSaaSRole() ? null : UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        User user = User.create(email, "Property User", "5551234567", "hash", role, tenantId, createdBy);

        String token = provider.generateToken(user);

        // El token debe ser válido y con la estructura JWT esperada
        assertThat(provider.validateToken(token)).isTrue();

        Claims claims = provider.extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo(user.getUserId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo(email);

        UUID extractedTenantId = provider.extractTenantId(token);
        assertThat(extractedTenantId).isEqualTo(tenantId);

        List<String> extractedRoles = provider.extractRoles(token);
        assertThat(extractedRoles).containsExactly(role.getValue());

        UUID extractedUserId = provider.extractUserId(token);
        assertThat(extractedUserId).isEqualTo(user.getUserId());
    }

    @Provide
    Arbitrary<Role> roles() {
        return Arbitraries.of(Role.values());
    }
}
