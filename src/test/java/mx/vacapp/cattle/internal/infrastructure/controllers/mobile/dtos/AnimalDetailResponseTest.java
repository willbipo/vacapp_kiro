package mx.vacapp.cattle.internal.infrastructure.controllers.mobile.dtos;

import mx.vacapp.cattle.internal.application.usecases.commands.AnimalResult;
import mx.vacapp.cattle.internal.infrastructure.controllers.mobile.dtos.AnimalDetailResponse.ParentSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitario para AnimalDetailResponse.
 * Verifica el correcto funcionamiento de los métodos factory fromResult()
 * y la creación de ParentSummary.
 */
class AnimalDetailResponseTest {
    
    @Test
    void testFromResult_WithCompleteGenealogy_ShouldCreateValidResponse() {
        // Arrange
        UUID animalId = UUID.randomUUID();
        UUID madreId = UUID.randomUUID();
        UUID padreId = UUID.randomUUID();
        Instant now = Instant.now();
        
        AnimalResult animalResult = new AnimalResult(
            animalId,
            "MX12345",
            "ABC123",
            "hembra",
            "angus",
            LocalDate.of(2023, 3, 15),
            12,
            LocalDate.of(2023, 3, 20),
            "vientre",
            "activa",
            "REEMO-2024-001",
            "Animal de buena genética",
            madreId,
            padreId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        
        ParentSummary madreDetails = ParentSummary.of(
            madreId, "MX11111", "hembra", "angus", 48
        );
        
        ParentSummary padreDetails = ParentSummary.of(
            padreId, "MX22222", "macho", "charolais", 60
        );
        
        Integer hijosCount = 3;
        
        // Act
        AnimalDetailResponse response = AnimalDetailResponse.fromResult(
            animalResult, madreDetails, padreDetails, hijosCount
        );
        
        // Assert
        assertNotNull(response);
        assertEquals(animalId, response.animalId());
        assertEquals("MX12345", response.arete());
        assertEquals("ABC123", response.areteAnterior());
        assertEquals("hembra", response.sexo());
        assertEquals("angus", response.raza());
        assertEquals(LocalDate.of(2023, 3, 15), response.fechaNacimiento());
        assertEquals(LocalDate.of(2023, 3, 20), response.fechaAretado());
        assertEquals("vientre", response.tipo());
        assertEquals("activa", response.status());
        assertEquals("REEMO-2024-001", response.folioReemo());
        assertEquals("Animal de buena genética", response.observaciones());
        
        // Verify madre details
        assertNotNull(response.madreDetails());
        assertEquals(madreId, response.madreDetails().animalId());
        assertEquals("MX11111", response.madreDetails().arete());
        assertEquals("hembra", response.madreDetails().sexo());
        assertEquals("angus", response.madreDetails().raza());
        assertEquals(48, response.madreDetails().edadMeses());
        
        // Verify padre details
        assertNotNull(response.padreDetails());
        assertEquals(padreId, response.padreDetails().animalId());
        assertEquals("MX22222", response.padreDetails().arete());
        assertEquals("macho", response.padreDetails().sexo());
        assertEquals("charolais", response.padreDetails().raza());
        assertEquals(60, response.padreDetails().edadMeses());
        
        // Verify hijos count
        assertEquals(3, response.hijosCount());
        
        // Verify timestamps conversion
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
        assertEquals(LocalDateTime.ofInstant(now, ZoneOffset.UTC), response.createdAt());
        assertEquals(LocalDateTime.ofInstant(now, ZoneOffset.UTC), response.updatedAt());
    }
    
    @Test
    void testFromResult_WithoutGenealogy_ShouldCreateValidResponse() {
        // Arrange
        UUID animalId = UUID.randomUUID();
        Instant now = Instant.now();
        
        AnimalResult animalResult = new AnimalResult(
            animalId,
            "MX99999",
            null,
            "macho",
            "charolais",
            LocalDate.of(2022, 1, 10),
            24,
            null,
            "engorda",
            "activa",
            null,
            null,
            null,  // no madre
            null,  // no padre
            UUID.randomUUID(),
            UUID.randomUUID(),
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        
        // Act
        AnimalDetailResponse response = AnimalDetailResponse.fromResult(animalResult);
        
        // Assert
        assertNotNull(response);
        assertEquals(animalId, response.animalId());
        assertEquals("MX99999", response.arete());
        assertNull(response.areteAnterior());
        assertEquals("macho", response.sexo());
        assertEquals("charolais", response.raza());
        assertNull(response.fechaAretado());
        assertNull(response.folioReemo());
        assertNull(response.observaciones());
        
        // Verify genealogy fields are null/0
        assertNull(response.madreDetails());
        assertNull(response.padreDetails());
        assertEquals(0, response.hijosCount());
    }
    
    @Test
    void testParentSummary_Of_ShouldCreateValidSummary() {
        // Arrange
        UUID parentId = UUID.randomUUID();
        
        // Act
        ParentSummary parent = ParentSummary.of(
            parentId, "MX88888", "hembra", "brahman", 36
        );
        
        // Assert
        assertNotNull(parent);
        assertEquals(parentId, parent.animalId());
        assertEquals("MX88888", parent.arete());
        assertEquals("hembra", parent.sexo());
        assertEquals("brahman", parent.raza());
        assertEquals(36, parent.edadMeses());
    }
    
    @Test
    void testFromResult_WithNullTimestamps_ShouldHandleGracefully() {
        // Arrange
        AnimalResult animalResult = new AnimalResult(
            UUID.randomUUID(),
            "MX77777",
            null,
            "hembra",
            "angus",
            LocalDate.of(2023, 6, 1),
            6,
            null,
            "cria",
            "activa",
            null,
            null,
            null,
            null,
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,  // null createdAt
            null,  // null updatedAt
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        
        // Act
        AnimalDetailResponse response = AnimalDetailResponse.fromResult(animalResult);
        
        // Assert
        assertNotNull(response);
        assertNull(response.createdAt());
        assertNull(response.updatedAt());
    }
}
