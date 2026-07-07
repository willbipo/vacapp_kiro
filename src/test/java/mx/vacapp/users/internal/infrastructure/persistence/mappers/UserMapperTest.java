package mx.vacapp.users.internal.infrastructure.persistence.mappers;

import mx.vacapp.users.internal.domain.model.Role;
import mx.vacapp.users.internal.domain.model.User;
import mx.vacapp.users.internal.domain.model.UserStatus;
import mx.vacapp.users.internal.infrastructure.persistence.entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para UserMapper.
 * <p>
 * Verifica las transformaciones bidireccionales entre User (dominio) y UserEntity (JPA),
 * incluyendo la conversión correcta de enums a String y viceversa.
 * </p>
 */
class UserMapperTest {
    
    private UserMapper mapper;
    
    @BeforeEach
    void setUp() {
        mapper = new UserMapper();
    }
    
    @Test
    void toEntity_shouldConvertUserToUserEntity() {
        // Given: un usuario de dominio con todos los campos
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        Instant now = Instant.now();
        
        User user = new User.Builder()
            .userId(userId)
            .email("test@example.com")
            .name("Test User")
            .phone("1234567890")
            .passwordHash("$2a$10$hashedpassword")
            .role(Role.ADMIN)
            .status(UserStatus.ACTIVE)
            .tenantId(tenantId)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();
        
        // When: convertimos a entidad JPA
        UserEntity entity = mapper.toEntity(user);
        
        // Then: todos los campos deben estar correctamente mapeados
        assertNotNull(entity);
        assertEquals(userId, entity.getUserId());
        assertEquals("test@example.com", entity.getEmail());
        assertEquals("Test User", entity.getName());
        assertEquals("1234567890", entity.getPhone());
        assertEquals("$2a$10$hashedpassword", entity.getPasswordHash());
        assertEquals("admin", entity.getRole());              // Enum → String
        assertEquals("active", entity.getStatus());            // Enum → String
        assertEquals(tenantId, entity.getTenantId());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
        assertEquals(createdBy, entity.getCreatedBy());
        assertEquals(createdBy, entity.getUpdatedBy());
    }
    
    @Test
    void toEntity_shouldHandleSaaSUser() {
        // Given: un usuario SaaS con tenantId null
        User user = new User.Builder()
            .userId(UUID.randomUUID())
            .email("superadmin@vacapp.mx")
            .name("Super Admin")
            .phone("9999999999")
            .passwordHash("$2a$10$hashedpassword")
            .role(Role.SUPER_ADMIN)
            .status(UserStatus.ACTIVE)
            .tenantId(null)  // Usuario SaaS sin tenant
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID())
            .updatedBy(UUID.randomUUID())
            .build();
        
        // When: convertimos a entidad JPA
        UserEntity entity = mapper.toEntity(user);
        
        // Then: tenantId debe ser null y rol debe ser super_admin
        assertNotNull(entity);
        assertNull(entity.getTenantId());
        assertEquals("super_admin", entity.getRole());
    }
    
    @Test
    void toEntity_shouldThrowExceptionWhenUserIsNull() {
        // When & Then: debe lanzar excepción si user es null
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> mapper.toEntity(null)
        );
        
        assertEquals("User no puede ser null", exception.getMessage());
    }
    
    @Test
    void toDomain_shouldConvertUserEntityToUser() {
        // Given: una entidad JPA con todos los campos
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        Instant now = Instant.now();
        
        UserEntity entity = new UserEntity(
            userId,
            "test@example.com",
            "Test User",
            "1234567890",
            "$2a$10$hashedpassword",
            "manager",      // String
            "active",       // String
            tenantId,
            now,
            now,
            createdBy,
            createdBy
        );
        
        // When: convertimos a entidad de dominio
        User user = mapper.toDomain(entity);
        
        // Then: todos los campos deben estar correctamente mapeados
        assertNotNull(user);
        assertEquals(userId, user.getUserId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getName());
        assertEquals("1234567890", user.getPhone());
        assertEquals("$2a$10$hashedpassword", user.getPasswordHash());
        assertEquals(Role.MANAGER, user.getRole());           // String → Enum
        assertEquals(UserStatus.ACTIVE, user.getStatus());    // String → Enum
        assertEquals(tenantId, user.getTenantId());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
        assertEquals(createdBy, user.getCreatedBy());
        assertEquals(createdBy, user.getUpdatedBy());
    }
    
    @Test
    void toDomain_shouldHandleAllRoles() {
        // Given: entidades con diferentes roles
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        
        String[] roleValues = {"super_admin", "support", "admin", "manager", "veterinarian", "worker"};
        Role[] expectedRoles = {Role.SUPER_ADMIN, Role.SUPPORT, Role.ADMIN, Role.MANAGER, Role.VETERINARIAN, Role.WORKER};
        
        for (int i = 0; i < roleValues.length; i++) {
            // When: convertimos entidad con rol específico
            UserEntity entity = new UserEntity(
                userId, "test@example.com", "Test", "123", "hash",
                roleValues[i], "active", null, now, now, userId, userId
            );
            
            User user = mapper.toDomain(entity);
            
            // Then: el enum debe ser correcto
            assertEquals(expectedRoles[i], user.getRole(), 
                "Rol " + roleValues[i] + " debe convertirse a " + expectedRoles[i]);
        }
    }
    
    @Test
    void toDomain_shouldHandleAllStatuses() {
        // Given: entidades con diferentes estados
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        
        String[] statusValues = {"active", "inactive", "locked"};
        UserStatus[] expectedStatuses = {UserStatus.ACTIVE, UserStatus.INACTIVE, UserStatus.LOCKED};
        
        for (int i = 0; i < statusValues.length; i++) {
            // When: convertimos entidad con estado específico
            UserEntity entity = new UserEntity(
                userId, "test@example.com", "Test", "123", "hash",
                "worker", statusValues[i], null, now, now, userId, userId
            );
            
            User user = mapper.toDomain(entity);
            
            // Then: el enum debe ser correcto
            assertEquals(expectedStatuses[i], user.getStatus(),
                "Estado " + statusValues[i] + " debe convertirse a " + expectedStatuses[i]);
        }
    }
    
    @Test
    void toDomain_shouldThrowExceptionWhenEntityIsNull() {
        // When & Then: debe lanzar excepción si entity es null
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> mapper.toDomain(null)
        );
        
        assertEquals("UserEntity no puede ser null", exception.getMessage());
    }
    
    @Test
    void toDomain_shouldThrowExceptionWhenRoleIsInvalid() {
        // Given: entidad con rol inválido
        UserEntity entity = new UserEntity(
            UUID.randomUUID(), "test@example.com", "Test", "123", "hash",
            "invalid_role", "active", null, Instant.now(), Instant.now(),
            UUID.randomUUID(), UUID.randomUUID()
        );
        
        // When & Then: debe lanzar excepción
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(entity));
    }
    
    @Test
    void toDomain_shouldThrowExceptionWhenStatusIsInvalid() {
        // Given: entidad con estado inválido
        UserEntity entity = new UserEntity(
            UUID.randomUUID(), "test@example.com", "Test", "123", "hash",
            "worker", "invalid_status", null, Instant.now(), Instant.now(),
            UUID.randomUUID(), UUID.randomUUID()
        );
        
        // When & Then: debe lanzar excepción
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(entity));
    }
    
    @Test
    void roundTrip_shouldPreserveAllFields() {
        // Given: un usuario de dominio original
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        Instant createdAt = Instant.now();
        Instant updatedAt = createdAt.plusSeconds(3600);
        
        User originalUser = new User.Builder()
            .userId(userId)
            .email("roundtrip@test.com")
            .name("Round Trip User")
            .phone("5551234567")
            .passwordHash("$2a$12$somehashedpassword")
            .role(Role.VETERINARIAN)
            .status(UserStatus.LOCKED)
            .tenantId(tenantId)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();
        
        // When: convertimos a entity y de vuelta a domain
        UserEntity entity = mapper.toEntity(originalUser);
        User reconstructedUser = mapper.toDomain(entity);
        
        // Then: todos los campos deben ser idénticos
        assertEquals(originalUser.getUserId(), reconstructedUser.getUserId());
        assertEquals(originalUser.getEmail(), reconstructedUser.getEmail());
        assertEquals(originalUser.getName(), reconstructedUser.getName());
        assertEquals(originalUser.getPhone(), reconstructedUser.getPhone());
        assertEquals(originalUser.getPasswordHash(), reconstructedUser.getPasswordHash());
        assertEquals(originalUser.getRole(), reconstructedUser.getRole());
        assertEquals(originalUser.getStatus(), reconstructedUser.getStatus());
        assertEquals(originalUser.getTenantId(), reconstructedUser.getTenantId());
        assertEquals(originalUser.getCreatedAt(), reconstructedUser.getCreatedAt());
        assertEquals(originalUser.getUpdatedAt(), reconstructedUser.getUpdatedAt());
        assertEquals(originalUser.getCreatedBy(), reconstructedUser.getCreatedBy());
        assertEquals(originalUser.getUpdatedBy(), reconstructedUser.getUpdatedBy());
    }
}
