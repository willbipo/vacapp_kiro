package mx.vacapp.users.internal.domain.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para la entidad de dominio User.
 */
class UserTest {
    
    @Test
    void create_shouldGenerateUserWithDefaultValues() {
        // Arrange
        String email = "Test@Example.com";
        String name = "Juan Pérez";
        String phone = "1234567890";
        String passwordHash = "$2a$10$hashedPassword";
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        
        // Act
        User user = User.create(email, name, phone, passwordHash, null, tenantId, createdBy);
        
        // Assert
        assertNotNull(user.getUserId(), "userId debe ser generado automáticamente");
        assertEquals("test@example.com", user.getEmail(), "Email debe estar en minúsculas");
        assertEquals(name, user.getName());
        assertEquals(phone, user.getPhone());
        assertEquals(passwordHash, user.getPasswordHash());
        assertEquals(Role.WORKER, user.getRole(), "Rol por defecto debe ser WORKER");
        assertEquals(UserStatus.ACTIVE, user.getStatus(), "Estado inicial debe ser ACTIVE");
        assertEquals(tenantId, user.getTenantId());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertEquals(createdBy, user.getCreatedBy());
        assertEquals(createdBy, user.getUpdatedBy());
    }
    
    @Test
    void create_shouldUseProvidedRole() {
        // Arrange
        String email = "admin@example.com";
        String name = "Admin User";
        String phone = "1234567890";
        String passwordHash = "$2a$10$hashedPassword";
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        
        // Act
        User user = User.create(email, name, phone, passwordHash, Role.ADMIN, tenantId, createdBy);
        
        // Assert
        assertEquals(Role.ADMIN, user.getRole(), "Debe usar el rol proporcionado");
    }
    
    @Test
    void create_shouldAllowNullTenantForSaaSUsers() {
        // Arrange
        String email = "superadmin@vacapp.mx";
        String name = "Super Admin";
        String phone = "1234567890";
        String passwordHash = "$2a$10$hashedPassword";
        UUID createdBy = UUID.randomUUID();
        
        // Act
        User user = User.create(email, name, phone, passwordHash, Role.SUPER_ADMIN, null, createdBy);
        
        // Assert
        assertNull(user.getTenantId(), "TenantId debe ser null para usuarios SaaS");
    }
    
    @Test
    void isActive_shouldReturnTrueForActiveUser() {
        // Arrange
        User user = createTestUser(UserStatus.ACTIVE);
        
        // Act & Assert
        assertTrue(user.isActive(), "Usuario con estado ACTIVE debe retornar true");
    }
    
    @Test
    void isActive_shouldReturnFalseForInactiveUser() {
        // Arrange
        User user = createTestUser(UserStatus.INACTIVE);
        
        // Act & Assert
        assertFalse(user.isActive(), "Usuario con estado INACTIVE debe retornar false");
    }
    
    @Test
    void isActive_shouldReturnFalseForLockedUser() {
        // Arrange
        User user = createTestUser(UserStatus.LOCKED);
        
        // Act & Assert
        assertFalse(user.isActive(), "Usuario con estado LOCKED debe retornar false");
    }
    
    @Test
    void isSaaSUser_shouldReturnTrueWhenTenantIdIsNull() {
        // Arrange
        User user = new User.Builder()
            .userId(UUID.randomUUID())
            .email("saas@example.com")
            .name("SaaS User")
            .phone("1234567890")
            .passwordHash("hash")
            .role(Role.SUPER_ADMIN)
            .status(UserStatus.ACTIVE)
            .tenantId(null)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID())
            .updatedBy(UUID.randomUUID())
            .build();
        
        // Act & Assert
        assertTrue(user.isSaaSUser(), "Usuario sin tenantId debe ser usuario SaaS");
    }
    
    @Test
    void isSaaSUser_shouldReturnFalseWhenTenantIdIsPresent() {
        // Arrange
        User user = new User.Builder()
            .userId(UUID.randomUUID())
            .email("business@example.com")
            .name("Business User")
            .phone("1234567890")
            .passwordHash("hash")
            .role(Role.ADMIN)
            .status(UserStatus.ACTIVE)
            .tenantId(UUID.randomUUID())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID())
            .updatedBy(UUID.randomUUID())
            .build();
        
        // Act & Assert
        assertFalse(user.isSaaSUser(), "Usuario con tenantId debe ser usuario de negocio");
    }
    
    @Test
    void hasSaaSRole_shouldReturnTrueForSuperAdmin() {
        // Arrange
        User user = createTestUserWithRole(Role.SUPER_ADMIN);
        
        // Act & Assert
        assertTrue(user.hasSaaSRole(), "SUPER_ADMIN debe ser rol SaaS");
    }
    
    @Test
    void hasSaaSRole_shouldReturnTrueForSupport() {
        // Arrange
        User user = createTestUserWithRole(Role.SUPPORT);
        
        // Act & Assert
        assertTrue(user.hasSaaSRole(), "SUPPORT debe ser rol SaaS");
    }
    
    @Test
    void hasSaaSRole_shouldReturnFalseForBusinessRoles() {
        // Assert para cada rol de negocio
        assertFalse(createTestUserWithRole(Role.ADMIN).hasSaaSRole(), "ADMIN no debe ser rol SaaS");
        assertFalse(createTestUserWithRole(Role.MANAGER).hasSaaSRole(), "MANAGER no debe ser rol SaaS");
        assertFalse(createTestUserWithRole(Role.VETERINARIAN).hasSaaSRole(), "VETERINARIAN no debe ser rol SaaS");
        assertFalse(createTestUserWithRole(Role.WORKER).hasSaaSRole(), "WORKER no debe ser rol SaaS");
    }
    
    @Test
    void deactivate_shouldReturnNewInstanceWithInactiveStatus() {
        // Arrange
        User originalUser = User.create(
            "user@example.com", 
            "Test User", 
            "1234567890",
            "hash",
            Role.WORKER,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        UUID deactivatedBy = UUID.randomUUID();
        Instant beforeDeactivation = Instant.now();
        
        // Act
        User deactivatedUser = originalUser.deactivate(deactivatedBy);
        
        // Assert
        assertNotSame(originalUser, deactivatedUser, "Debe retornar una nueva instancia");
        assertEquals(UserStatus.INACTIVE, deactivatedUser.getStatus(), "Estado debe ser INACTIVE");
        assertEquals(deactivatedBy, deactivatedUser.getUpdatedBy(), "updatedBy debe ser quien desactivó");
        assertTrue(deactivatedUser.getUpdatedAt().isAfter(beforeDeactivation) || 
                   deactivatedUser.getUpdatedAt().equals(beforeDeactivation),
                   "updatedAt debe ser actualizado");
        
        // Verificar que otros campos permanecen iguales
        assertEquals(originalUser.getUserId(), deactivatedUser.getUserId());
        assertEquals(originalUser.getEmail(), deactivatedUser.getEmail());
        assertEquals(originalUser.getName(), deactivatedUser.getName());
        assertEquals(originalUser.getRole(), deactivatedUser.getRole());
        assertEquals(originalUser.getTenantId(), deactivatedUser.getTenantId());
        assertEquals(originalUser.getCreatedAt(), deactivatedUser.getCreatedAt());
        assertEquals(originalUser.getCreatedBy(), deactivatedUser.getCreatedBy());
        
        // Verificar que el usuario original no cambió (inmutabilidad)
        assertEquals(UserStatus.ACTIVE, originalUser.getStatus());
    }
    
    @Test
    void changeRole_shouldReturnNewInstanceWithNewRole() {
        // Arrange
        User originalUser = User.create(
            "user@example.com", 
            "Test User", 
            "1234567890",
            "hash",
            Role.WORKER,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        UUID changedBy = UUID.randomUUID();
        Instant beforeChange = Instant.now();
        
        // Act
        User updatedUser = originalUser.changeRole(Role.MANAGER, changedBy);
        
        // Assert
        assertNotSame(originalUser, updatedUser, "Debe retornar una nueva instancia");
        assertEquals(Role.MANAGER, updatedUser.getRole(), "Rol debe ser actualizado");
        assertEquals(changedBy, updatedUser.getUpdatedBy(), "updatedBy debe ser quien cambió el rol");
        assertTrue(updatedUser.getUpdatedAt().isAfter(beforeChange) || 
                   updatedUser.getUpdatedAt().equals(beforeChange),
                   "updatedAt debe ser actualizado");
        
        // Verificar que otros campos permanecen iguales
        assertEquals(originalUser.getUserId(), updatedUser.getUserId());
        assertEquals(originalUser.getEmail(), updatedUser.getEmail());
        assertEquals(originalUser.getName(), updatedUser.getName());
        assertEquals(originalUser.getStatus(), updatedUser.getStatus());
        assertEquals(originalUser.getTenantId(), updatedUser.getTenantId());
        assertEquals(originalUser.getCreatedAt(), updatedUser.getCreatedAt());
        assertEquals(originalUser.getCreatedBy(), updatedUser.getCreatedBy());
        
        // Verificar que el usuario original no cambió (inmutabilidad)
        assertEquals(Role.WORKER, originalUser.getRole());
    }
    
    @Test
    void builder_from_shouldCopyAllFields() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        Instant createdAt = Instant.now();
        
        User original = new User.Builder()
            .userId(userId)
            .email("test@example.com")
            .name("Test User")
            .phone("1234567890")
            .passwordHash("hash")
            .role(Role.ADMIN)
            .status(UserStatus.ACTIVE)
            .tenantId(tenantId)
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();
        
        // Act
        User copy = new User.Builder()
            .from(original)
            .build();
        
        // Assert
        assertNotSame(original, copy, "Debe ser una nueva instancia");
        assertEquals(original.getUserId(), copy.getUserId());
        assertEquals(original.getEmail(), copy.getEmail());
        assertEquals(original.getName(), copy.getName());
        assertEquals(original.getPhone(), copy.getPhone());
        assertEquals(original.getPasswordHash(), copy.getPasswordHash());
        assertEquals(original.getRole(), copy.getRole());
        assertEquals(original.getStatus(), copy.getStatus());
        assertEquals(original.getTenantId(), copy.getTenantId());
        assertEquals(original.getCreatedAt(), copy.getCreatedAt());
        assertEquals(original.getUpdatedAt(), copy.getUpdatedAt());
        assertEquals(original.getCreatedBy(), copy.getCreatedBy());
        assertEquals(original.getUpdatedBy(), copy.getUpdatedBy());
    }
    
    @Test
    void builder_from_shouldAllowModifications() {
        // Arrange
        User original = User.create(
            "original@example.com",
            "Original Name",
            "1234567890",
            "hash",
            Role.WORKER,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        String newName = "Modified Name";
        Role newRole = Role.MANAGER;
        
        // Act
        User modified = new User.Builder()
            .from(original)
            .name(newName)
            .role(newRole)
            .build();
        
        // Assert
        assertEquals(newName, modified.getName(), "Nombre debe estar modificado");
        assertEquals(newRole, modified.getRole(), "Rol debe estar modificado");
        assertEquals(original.getEmail(), modified.getEmail(), "Email debe permanecer igual");
        assertEquals(original.getUserId(), modified.getUserId(), "userId debe permanecer igual");
        
        // Verificar que el original no cambió
        assertEquals("Original Name", original.getName());
        assertEquals(Role.WORKER, original.getRole());
    }
    
    // Métodos auxiliares para crear usuarios de prueba
    
    private User createTestUser(UserStatus status) {
        return new User.Builder()
            .userId(UUID.randomUUID())
            .email("test@example.com")
            .name("Test User")
            .phone("1234567890")
            .passwordHash("hash")
            .role(Role.WORKER)
            .status(status)
            .tenantId(UUID.randomUUID())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID())
            .updatedBy(UUID.randomUUID())
            .build();
    }
    
    private User createTestUserWithRole(Role role) {
        return new User.Builder()
            .userId(UUID.randomUUID())
            .email("test@example.com")
            .name("Test User")
            .phone("1234567890")
            .passwordHash("hash")
            .role(role)
            .status(UserStatus.ACTIVE)
            .tenantId(role.isSaaSRole() ? null : UUID.randomUUID())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID())
            .updatedBy(UUID.randomUUID())
            .build();
    }
}
