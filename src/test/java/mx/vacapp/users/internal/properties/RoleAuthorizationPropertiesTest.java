package mx.vacapp.users.internal.properties;

import mx.vacapp.users.internal.application.usecases.commands.UserResult;
import mx.vacapp.users.internal.application.usecases.user.ChangeUserRoleUseCase;
import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.UserStatus;
import mx.vacapp.users.internal.domain.repository.AuditRepository;
import mx.vacapp.users.internal.domain.repository.UserRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.AfterProperty;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Property 6: Role-Based Authorization Hierarchy.
 * <p>
 * super_admin puede asignar cualquier rol (incluyendo roles SaaS); admin puede
 * asignar solo roles de negocio (Business_Role); ningún otro rol autenticado
 * puede cambiar roles. Esto forma una jerarquía donde los permisos de
 * super_admin estrictamente contienen los de admin.
 * </p>
 */
class RoleAuthorizationPropertiesTest {

    @AfterProperty
    void cleanup() {
        mx.vacapp.users.internal.infrastructure.security.TenantContext.clear();
    }

    @Property
    void superAdminCanAssignAnyRole(@ForAll("roles") Role targetRole) {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        AuditRepository auditRepository = Mockito.mock(AuditRepository.class);
        ChangeUserRoleUseCase useCase = new ChangeUserRoleUseCase(userRepository, auditRepository);

        User existingUser = buildUser(Role.WORKER, targetRole.isSaaSRole() ? null : UUID.randomUUID());
        when(userRepository.findById(existingUser.getUserId())).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResult result = useCase.execute(
            existingUser.getUserId(), targetRole.getValue(), UUID.randomUUID(), Role.SUPER_ADMIN
        );

        assertThat(result.role()).isEqualTo(targetRole.getValue());
    }

    @Property
    void adminCanOnlyAssignBusinessRoles(@ForAll("roles") Role targetRole) {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        AuditRepository auditRepository = Mockito.mock(AuditRepository.class);
        ChangeUserRoleUseCase useCase = new ChangeUserRoleUseCase(userRepository, auditRepository);

        UUID sharedTenant = UUID.randomUUID();
        User existingUser = buildUser(Role.WORKER, sharedTenant);

        mx.vacapp.users.internal.infrastructure.security.TenantContext.setTenantId(sharedTenant);

        if (targetRole.isSaaSRole()) {
            // admin nunca debe poder asignar roles SaaS, sin importar si el usuario existe
            assertThatThrownBy(() ->
                useCase.execute(existingUser.getUserId(), targetRole.getValue(), UUID.randomUUID(), Role.ADMIN)
            ).isInstanceOf(AccessDeniedException.class);
        } else {
            when(userRepository.findById(existingUser.getUserId())).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserResult result = useCase.execute(
                existingUser.getUserId(), targetRole.getValue(), UUID.randomUUID(), Role.ADMIN
            );
            assertThat(result.role()).isEqualTo(targetRole.getValue());
        }
    }

    @Property
    void nonPrivilegedRolesCanNeverChangeRoles(
        @ForAll("nonPrivilegedRoles") Role actingRole,
        @ForAll("roles") Role targetRole
    ) {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        AuditRepository auditRepository = Mockito.mock(AuditRepository.class);
        ChangeUserRoleUseCase useCase = new ChangeUserRoleUseCase(userRepository, auditRepository);

        assertThatThrownBy(() ->
            useCase.execute(UUID.randomUUID(), targetRole.getValue(), UUID.randomUUID(), actingRole)
        ).isInstanceOf(AccessDeniedException.class);
    }

    private User buildUser(Role role, UUID tenantId) {
        return new User.Builder()
            .userId(UUID.randomUUID())
            .email("hierarchy-test@example.com")
            .name("Hierarchy Test")
            .phone("5551234567")
            .passwordHash("hash")
            .role(role)
            .status(UserStatus.ACTIVE)
            .tenantId(tenantId)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID())
            .updatedBy(UUID.randomUUID())
            .build();
    }

    @Provide
    Arbitrary<Role> roles() {
        return Arbitraries.of(Role.values());
    }

    @Provide
    Arbitrary<Role> nonPrivilegedRoles() {
        return Arbitraries.of(Role.SUPPORT, Role.MANAGER, Role.VETERINARIAN, Role.WORKER);
    }
}
