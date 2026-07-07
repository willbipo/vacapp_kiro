package mx.vacapp.users.internal.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para el enum Role.
 */
class RoleTest {

    @Test
    void testGetValue() {
        assertEquals("super_admin", Role.SUPER_ADMIN.getValue());
        assertEquals("support", Role.SUPPORT.getValue());
        assertEquals("admin", Role.ADMIN.getValue());
        assertEquals("manager", Role.MANAGER.getValue());
        assertEquals("veterinarian", Role.VETERINARIAN.getValue());
        assertEquals("worker", Role.WORKER.getValue());
    }

    @Test
    void testIsSaaSRole() {
        // SaaS roles
        assertTrue(Role.SUPER_ADMIN.isSaaSRole());
        assertTrue(Role.SUPPORT.isSaaSRole());
        
        // Business roles
        assertFalse(Role.ADMIN.isSaaSRole());
        assertFalse(Role.MANAGER.isSaaSRole());
        assertFalse(Role.VETERINARIAN.isSaaSRole());
        assertFalse(Role.WORKER.isSaaSRole());
    }

    @Test
    void testIsBusinessRole() {
        // SaaS roles
        assertFalse(Role.SUPER_ADMIN.isBusinessRole());
        assertFalse(Role.SUPPORT.isBusinessRole());
        
        // Business roles
        assertTrue(Role.ADMIN.isBusinessRole());
        assertTrue(Role.MANAGER.isBusinessRole());
        assertTrue(Role.VETERINARIAN.isBusinessRole());
        assertTrue(Role.WORKER.isBusinessRole());
    }

    @Test
    void testFromString() {
        assertEquals(Role.SUPER_ADMIN, Role.fromString("super_admin"));
        assertEquals(Role.SUPPORT, Role.fromString("support"));
        assertEquals(Role.ADMIN, Role.fromString("admin"));
        assertEquals(Role.MANAGER, Role.fromString("manager"));
        assertEquals(Role.VETERINARIAN, Role.fromString("veterinarian"));
        assertEquals(Role.WORKER, Role.fromString("worker"));
    }

    @Test
    void testFromStringCaseInsensitive() {
        assertEquals(Role.SUPER_ADMIN, Role.fromString("SUPER_ADMIN"));
        assertEquals(Role.ADMIN, Role.fromString("Admin"));
        assertEquals(Role.MANAGER, Role.fromString("MANAGER"));
    }

    @Test
    void testFromStringInvalidValue() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Role.fromString("invalid_role")
        );
        assertEquals("Rol inválido: invalid_role", exception.getMessage());
    }

    @Test
    void testAllRolesCount() {
        Role[] roles = Role.values();
        assertEquals(6, roles.length, "Debe haber exactamente 6 roles");
    }

    @Test
    void testSaaSAndBusinessRoleCounts() {
        long saasCount = 0;
        long businessCount = 0;
        
        for (Role role : Role.values()) {
            if (role.isSaaSRole()) {
                saasCount++;
            }
            if (role.isBusinessRole()) {
                businessCount++;
            }
        }
        
        assertEquals(2, saasCount, "Debe haber 2 roles SaaS");
        assertEquals(4, businessCount, "Debe haber 4 roles Business");
    }
}
