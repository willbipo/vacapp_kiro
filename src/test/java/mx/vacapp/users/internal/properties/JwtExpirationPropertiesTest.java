package mx.vacapp.users.internal.properties;

import io.jsonwebtoken.Claims;
import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.infrastructure.security.JwtTokenProvider;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 5: JWT Expiration Consistency.
 * <p>
 * Para todos los tokens JWT generados, la diferencia entre los claims {@code exp}
 * y {@code iat} debe ser exactamente 24 horas (86 400 segundos), y {@code exp} debe
 * estar en el futuro respecto al momento de generación.
 * </p>
 */
class JwtExpirationPropertiesTest {

    private static final long EXPIRATION_MS = 86_400_000L; // 24 horas

    private JwtTokenProvider newProvider() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret",
            "expiration-property-test-secret-key-at-least-256-bits-32-bytes");
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", EXPIRATION_MS);
        ReflectionTestUtils.setField(provider, "jwtIssuer", "vacapp-property-test");
        return provider;
    }

    @Property
    void expirationIsExactly24HoursAfterIssuedAt(@ForAll("roles") Role role) {
        JwtTokenProvider provider = newProvider();
        UUID tenantId = role.isSaaSRole() ? null : UUID.randomUUID();

        User user = User.create(
            "expiry-test@example.com", "Expiry User", "5551234567",
            "hash", role, tenantId, UUID.randomUUID()
        );

        Instant beforeGeneration = Instant.now();
        String token = provider.generateToken(user);
        Instant afterGeneration = Instant.now();

        Claims claims = provider.extractClaims(token);
        long iatSeconds = claims.getIssuedAt().toInstant().getEpochSecond();
        long expSeconds = claims.getExpiration().toInstant().getEpochSecond();

        assertThat(expSeconds - iatSeconds).isEqualTo(EXPIRATION_MS / 1000);
        assertThat(claims.getExpiration().toInstant()).isAfter(beforeGeneration);
        assertThat(claims.getIssuedAt().toInstant())
            .isBetween(beforeGeneration.minusSeconds(1), afterGeneration.plusSeconds(1));
    }

    @Provide
    Arbitrary<Role> roles() {
        return Arbitraries.of(Role.values());
    }
}
