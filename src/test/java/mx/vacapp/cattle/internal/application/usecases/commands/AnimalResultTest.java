package mx.vacapp.cattle.internal.application.usecases.commands;

import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.model.Breed;
import mx.vacapp.cattle.internal.domain.model.CattleType;
import mx.vacapp.cattle.internal.domain.model.Sex;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AnimalResult record.
 * Tests the fromDomain static factory method to ensure proper mapping from Animal entity.
 */
class AnimalResultTest {
    
    @Test
    void fromDomain_ShouldMapAllFields_WhenAnimalHasAllData() {
        // Given
        UUID ranchoId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        
        Animal animal = Animal.create(
            "ARETE123",
            Sex.HEMBRA,
            Breed.CHAROLAIS,
            LocalDate.of(2023, 1, 15),
            CattleType.VIENTRE,
            ranchoId,
            tenantId,
            createdBy
        );
        
        // When
        AnimalResult result = AnimalResult.fromDomain(animal);
        
        // Then
        assertNotNull(result);
        assertEquals(animal.getAnimalId(), result.animalId());
        assertEquals("ARETE123", result.arete());
        assertEquals("hembra", result.sexo());
        assertEquals("charolais", result.raza());
        assertEquals(LocalDate.of(2023, 1, 15), result.fechaNacimiento());
        assertNotNull(result.meses());
        assertEquals("vientre", result.tipo());
        assertEquals("activa", result.status());
        assertEquals(ranchoId, result.ranchoId());
        assertEquals(tenantId, result.tenantId());
        assertEquals(createdBy, result.createdBy());
        assertNotNull(result.createdAt());
        assertNotNull(result.updatedAt());
    }
    
    @Test
    void fromDomain_ShouldMapEnumsToLowercase_WhenCreatingResult() {
        // Given
        Animal animal = Animal.create(
            "TEST001",
            Sex.MACHO,
            Breed.ANGUS,
            LocalDate.of(2022, 6, 1),
            CattleType.SEMENTAL,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        // When
        AnimalResult result = AnimalResult.fromDomain(animal);
        
        // Then
        assertEquals("macho", result.sexo());
        assertEquals("angus", result.raza());
        assertEquals("semental", result.tipo());
        assertEquals("activa", result.status());
    }
    
    @Test
    void fromDomain_ShouldHandleNullableFields_WhenAnimalHasNoParents() {
        // Given
        Animal animal = Animal.create(
            "ORPHAN001",
            Sex.HEMBRA,
            Breed.BRAHMAN,
            LocalDate.of(2023, 3, 10),
            CattleType.CRIA,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        // When
        AnimalResult result = AnimalResult.fromDomain(animal);
        
        // Then
        assertNull(result.madreId());
        assertNull(result.padreId());
        assertNull(result.areteAnterior());
        assertNull(result.folioReemo());
        assertNull(result.nota());
        assertNull(result.fechaAretado());
        assertNull(result.fechaVenta());
        assertNull(result.precioVenta());
        assertNull(result.fechaMuerte());
        assertNull(result.motivoMuerte());
    }
    
    @Test
    void fromDomain_ShouldIncludeSaleData_WhenAnimalIsSold() {
        // Given
        Animal animal = Animal.create(
            "SOLD001",
            Sex.MACHO,
            Breed.HEREFORD,
            LocalDate.of(2021, 1, 1),
            CattleType.VENTA,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        LocalDate saleDate = LocalDate.of(2024, 6, 15);
        BigDecimal salePrice = new BigDecimal("15000.00");
        Animal soldAnimal = animal.markAsSold(saleDate, salePrice, UUID.randomUUID());
        
        // When
        AnimalResult result = AnimalResult.fromDomain(soldAnimal);
        
        // Then
        assertEquals("vendida", result.status());
        assertEquals(saleDate, result.fechaVenta());
        assertEquals(salePrice, result.precioVenta());
    }
    
    @Test
    void fromDomain_ShouldIncludeDeathData_WhenAnimalIsDead() {
        // Given
        Animal animal = Animal.create(
            "DEAD001",
            Sex.HEMBRA,
            Breed.SIMMENTAL,
            LocalDate.of(2020, 5, 10),
            CattleType.CRIA,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        LocalDate deathDate = LocalDate.of(2024, 7, 1);
        String deathReason = "Enfermedad respiratoria";
        Animal deadAnimal = animal.markAsDead(deathDate, deathReason, UUID.randomUUID());
        
        // When
        AnimalResult result = AnimalResult.fromDomain(deadAnimal);
        
        // Then
        assertEquals("muerta", result.status());
        assertEquals(deathDate, result.fechaMuerte());
        assertEquals(deathReason, result.motivoMuerte());
    }
    
    @Test
    void fromDomain_ShouldIncludePregnantStatus_WhenFemaleAnimalIsPregnant() {
        // Given
        Animal animal = Animal.create(
            "PREG001",
            Sex.HEMBRA,
            Breed.BRANGUS,
            LocalDate.of(2021, 8, 20),
            CattleType.VIENTRE,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        Animal pregnantAnimal = animal.markAsPregnant(UUID.randomUUID());
        
        // When
        AnimalResult result = AnimalResult.fromDomain(pregnantAnimal);
        
        // Then
        assertEquals("prenada", result.status());
        assertEquals("hembra", result.sexo());
    }
    
    @Test
    void fromDomain_ShouldCalculateAgeInMonths_WhenAnimalIsCreated() {
        // Given
        LocalDate birthDate = LocalDate.now().minusMonths(18);
        Animal animal = Animal.create(
            "AGE001",
            Sex.MACHO,
            Breed.CRIOLLO,
            birthDate,
            CattleType.ENGORDA,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        // When
        AnimalResult result = AnimalResult.fromDomain(animal);
        
        // Then
        assertNotNull(result.meses());
        assertTrue(result.meses() >= 17 && result.meses() <= 18);
    }
    
    @Test
    void fromDomain_ShouldNormalizeAreteToUppercase_WhenAnimalIsCreated() {
        // Given
        Animal animal = Animal.create(
            "lowercase123",
            Sex.HEMBRA,
            Breed.LIMOUSIN,
            LocalDate.of(2022, 2, 28),
            CattleType.VENTA,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        // When
        AnimalResult result = AnimalResult.fromDomain(animal);
        
        // Then
        assertEquals("LOWERCASE123", result.arete());
    }
}
