package mx.vacapp.users.internal.properties;

import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.infrastructure.persistence.entities.UserEntity;
import mx.vacapp.users.internal.infrastructure.persistence.impl.UserRepositoryImpl;
import mx.vacapp.users.internal.infrastructure.persistence.mappers.UserMapper;
import mx.vacapp.users.internal.infrastructure.persistence.repositories.UserJpaRepository;
import mx.vacapp.users.internal.infrastructure.security.TenantContext;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.lifecycle.AfterProperty;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Property 4: Multi-tenancy Isolation.
 * <p>
 * Para toda operación de lectura ejecutada con un tenant_id establecido en
 * {@link TenantContext}, el resultado retornado por {@link UserRepositoryImpl}
 * debe estar compuesto exclusivamente por usuarios de ese tenant, sin importar
 * cuántos usuarios de otros tenants existan en el conjunto subyacente.
 * </p>
 */
class MultiTenancyIsolationPropertiesTest {

    @AfterProperty
    void cleanup() {
        TenantContext.clear();
    }

    @Property
    void findAllOnlyReturnsUsersOfCurrentTenant(
        @ForAll @IntRange(min = 1, max = 5) int usersInTenantA,
        @ForAll @IntRange(min = 0, max = 5) int usersInTenantB
    ) {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        List<UserEntity> tenantAEntities = buildEntities(usersInTenantA, tenantA);
        List<UserEntity> tenantBEntities = buildEntities(usersInTenantB, tenantB);

        List<UserEntity> allEntities = java.util.stream.Stream
            .concat(tenantAEntities.stream(), tenantBEntities.stream())
            .collect(Collectors.toList());

        UserJpaRepository jpaRepository = Mockito.mock(UserJpaRepository.class);
        UserMapper mapper = new UserMapper();
        UserRepositoryImpl repository = new UserRepositoryImpl(jpaRepository, mapper);

        when(jpaRepository.findAll(any(PageRequest.class)))
            .thenReturn(new PageImpl<>(allEntities));

        TenantContext.setTenantId(tenantA);
        List<User> result = repository.findAll(0, 50);

        assertThat(result).hasSize(usersInTenantA);
        assertThat(result).allSatisfy(user -> assertThat(user.getTenantId()).isEqualTo(tenantA));
    }

    private List<UserEntity> buildEntities(int count, UUID tenantId) {
        return IntStream.range(0, count)
            .mapToObj(i -> {
                UUID id = UUID.randomUUID();
                Instant now = Instant.now();
                return new UserEntity(
                    id, "user" + i + "-" + id + "@example.com", "User " + i, "5551234567",
                    "hash", Role.WORKER.getValue(), "active", tenantId, now, now, id, id
                );
            })
            .collect(Collectors.toList());
    }
}
