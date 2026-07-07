package mx.vacapp.users.internal.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para el enum UserStatus.
 */
class UserStatusTest {

    @Test
    void testGetValue() {
        assertEquals("active", UserStatus.ACTIVE.getValue());
        assertEquals("inactive", UserStatus.INACTIVE.getValue());
        assertEquals("locked", UserStatus.LOCKED.getValue());
    }

    @Test
    void testFromString() {
        assertEquals(UserStatus.ACTIVE, UserStatus.fromString("active"));
        assertEquals(UserStatus.INACTIVE, UserStatus.fromString("inactive"));
        assertEquals(UserStatus.LOCKED, UserStatus.fromString("locked"));
    }

    @Test
    void testFromStringCaseInsensitive() {
        assertEquals(UserStatus.ACTIVE, UserStatus.fromString("ACTIVE"));
        assertEquals(UserStatus.INACTIVE, UserStatus.fromString("Inactive"));
        assertEquals(UserStatus.LOCKED, UserStatus.fromString("LOCKED"));
    }

    @Test
    void testFromStringInvalidValue() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> UserStatus.fromString("invalid_status")
        );
        assertEquals("Estado inválido: invalid_status", exception.getMessage());
    }

    @Test
    void testAllStatusCount() {
        UserStatus[] statuses = UserStatus.values();
        assertEquals(3, statuses.length, "Debe haber exactamente 3 estados");
    }

    @Test
    void testStatusOrdering() {
        UserStatus[] statuses = UserStatus.values();
        assertEquals(UserStatus.ACTIVE, statuses[0]);
        assertEquals(UserStatus.INACTIVE, statuses[1]);
        assertEquals(UserStatus.LOCKED, statuses[2]);
    }
}
