package mx.vacapp.cattle.internal.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests para la entidad de dominio Animal.
 * Verifica comportamiento inmutable, factory method, y métodos de transición de estado.
 */
class AnimalTest {
    
    @Test
    void testCreateAnimal_shouldCreateWithDefaultValues() {
        // Given
        String arete = "abc123";
        Sex sexo = Sex.MACHO;
        Breed raza = Breed.ANGUS;
        LocalDate fechaNacimiento = LocalDate.of(2023, 1, 15);
        UUID ranchoId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        
        // When
        Animal animal = Animal.create(arete, sexo, raza, fechaNacimiento, null, ranchoId, tenantId, createdBy);
        
        // Then
        assertNotNull(animal);
        assertNotNull(animal.getAnimalId());
        assertEquals("ABC123", animal.getArete()); // Normalizado a mayúsculas
        assertEquals(sexo, animal.getSexo());
        assertEquals(raza, animal.getRaza());
        assertEquals(fechaNacimiento, animal.getFechaNacimiento());
        assertEquals(CattleType.VENTA, animal.getTipo()); // Valor por defecto
        assertEquals(CattleStatus.ACTIVA, animal.getStatus()); // Valor por defecto
        assertNotNull(animal.getMeses()); // Edad calculada automáticamente
        assertTrue(animal.getMeses() >= 0);
        assertEquals(ranchoId, animal.getRanchoId());
        assertEquals(tenantId, animal.getTenantId());
        assertEquals(createdBy, animal.getCreatedBy());
        assertNotNull(animal.getCreatedAt());
        assertNotNull(animal.getUpdatedAt());
    }
    
    @Test
    void testCreateAnimal_shouldNormalizeAreteToUpperCase() {
        // Given
        String arete = "abc123xyz";
        
        // When
        Animal animal = Animal.create(
            arete, 
            Sex.HEMBRA, 
            Breed.CHAROLAIS, 
            LocalDate.of(2022, 6, 1), 
            CattleType.VIENTRE,
            UUID.randomUUID(), 
            UUID.randomUUID(), 
            UUID.randomUUID()
        );
        
        // Then
        assertEquals("ABC123XYZ", animal.getArete());
    }
    
    @Test
    void testIsActive_shouldReturnTrueForActiveStatus() {
        // Given
        Animal animal = Animal.create(
            "TEST001", 
            Sex.HEMBRA, 
            Breed.BRAHMAN, 
            LocalDate.of(2023, 1, 1), 
            null,
            UUID.randomUUID(), 
            UUID.randomUUID(), 
            UUID.randomUUID()
        );
        
        // When & Then
        assertTrue(animal.isActive());
        assertFalse(animal.isSoldOrDead());
    }
    
    @Test
    void testMarkAsSold_shouldReturnNewInstanceWithSoldStatus() {
        // Given
        Animal original = Animal.create(
            "SOLD001", 
            Sex.MACHO, 
            Breed.HEREFORD, 
            LocalDate.of(2022, 1, 1), 
            CattleType.ENGORDA,
            UUID.randomUUID(), 
            UUID.randomUUID(), 
            UUID.randomUUID()
        );
        LocalDate fechaVenta = LocalDate.of(2024, 12, 31);
        BigDecimal precioVenta = new BigDecimal("15000.00");
        UUID updatedBy = UUID.randomUUID();
        
        // When
        Animal soldAnimal = original.markAsSold(fechaVenta, precioVenta, updatedBy);
        
        // Then
        assertNotSame(original, soldAnimal); // Inmutabilidad: diferente instancia
        assertEquals(CattleStatus.ACTIVA, original.getStatus()); // Original sin modificar
        assertEquals(CattleStatus.VENDIDA, soldAnimal.getStatus());
        assertEquals(fechaVenta, soldAnimal.getFechaVenta());
        assertEquals(precioVenta, soldAnimal.getPrecioVenta());
        assertEquals(updatedBy, soldAnimal.getUpdatedBy());
        assertTrue(soldAnimal.isSoldOrDead());
        assertFalse(soldAnimal.isActive());
    }
    
    @Test
    void testMarkAsDead_shouldReturnNewInstanceWithDeadStatus() {
        // Given
        Animal original = Animal.create(
            "DEAD001", 
            Sex.HEMBRA, 
            Breed.SIMMENTAL, 
            LocalDate.of(2020, 3, 15), 
            CattleType.VIENTRE,
            UUID.randomUUID(), 
            UUID.randomUUID(), 
            UUID.randomUUID()
        );
        LocalDate fechaMuerte = LocalDate.of(2024, 12, 1);
        String motivoMuerte = "Enfermedad respiratoria";
        UUID updatedBy = UUID.randomUUID();
        
        // When
        Animal deadAnimal = original.markAsDead(fechaMuerte, motivoMuerte, updatedBy);
        
        // Then
        assertNotSame(original, deadAnimal); // Inmutabilidad
        assertEquals(CattleStatus.ACTIVA, original.getStatus());
        assertEquals(CattleStatus.MUERTA, deadAnimal.getStatus());
        assertEquals(fechaMuerte, deadAnimal.getFechaMuerte());
        assertEquals(motivoMuerte, deadAnimal.getMotivoMuerte());
        assertEquals(updatedBy, deadAnimal.getUpdatedBy());
        assertTrue(deadAnimal.isSoldOrDead());
        assertFalse(deadAnimal.isActive());
    }
    
    @Test
    void testMarkAsPregnant_shouldReturnNewInstanceWithPregnantStatus() {
        // Given
        Animal hembra = Animal.create(
            "PREG001", 
            Sex.HEMBRA, 
            Breed.ANGUS, 
            LocalDate.of(2021, 5, 20), 
            CattleType.VIENTRE,
            UUID.randomUUID(), 
            UUID.randomUUID(), 
            UUID.randomUUID()
        );
        UUID updatedBy = UUID.randomUUID();
        
        // When
        Animal pregnant = hembra.markAsPregnant(updatedBy);
        
        // Then
        assertNotSame(hembra, pregnant);
        assertEquals(CattleStatus.ACTIVA, hembra.getStatus());
        assertEquals(CattleStatus.PRENADA, pregnant.getStatus());
        assertTrue(pregnant.isActive());
    }
    
    @Test
    void testMarkAsPregnant_shouldThrowExceptionForMale() {
        // Given
        Animal macho = Animal.create(
            "MALE001", 
            Sex.MACHO, 
            Breed.BRAHMAN, 
            LocalDate.of(2022, 8, 10), 
            CattleType.SEMENTAL,
            UUID.randomUUID(), 
            UUID.randomUUID(), 
            UUID.randomUUID()
        );
        UUID updatedBy = UUID.randomUUID();
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> macho.markAsPregnant(updatedBy)
        );
        assertEquals("Solo hembras pueden estar preñadas", exception.getMessage());
    }
    
    @Test
    void testUpdateNota_shouldReturnNewInstanceWithUpdatedNota() {
        // Given
        Animal original = Animal.create(
            "NOTE001", 
            Sex.HEMBRA, 
            Breed.CRIOLLO, 
            LocalDate.of(2023, 2, 28), 
            null,
            UUID.randomUUID(), 
            UUID.randomUUID(), 
            UUID.randomUUID()
        );
        String nota = "Animal con excelente conformación";
        UUID updatedBy = UUID.randomUUID();
        
        // When
        Animal updated = original.updateNota(nota, updatedBy);
        
        // Then
        assertNotSame(original, updated);
        assertNull(original.getNota());
        assertEquals(nota, updated.getNota());
        assertEquals(updatedBy, updated.getUpdatedBy());
    }
    
    @Test
    void testUpdateTipo_shouldReturnNewInstanceWithUpdatedTipo() {
        // Given
        Animal original = Animal.create(
            "TYPE001", 
            Sex.MACHO, 
            Breed.LIMOUSIN, 
            LocalDate.of(2023, 4, 10), 
            CattleType.VENTA,
            UUID.randomUUID(), 
            UUID.randomUUID(), 
            UUID.randomUUID()
        );
        UUID updatedBy = UUID.randomUUID();
        
        // When
        Animal updated = original.updateTipo(CattleType.ENGORDA, updatedBy);
        
        // Then
        assertNotSame(original, updated);
        assertEquals(CattleType.VENTA, original.getTipo());
        assertEquals(CattleType.ENGORDA, updated.getTipo());
    }
    
    @Test
    void testUpdateFolioReemo_shouldReturnNewInstanceWithUpdatedFolio() {
        // Given
        Animal original = Animal.create(
            "FOLIO001", 
            Sex.HEMBRA, 
            Breed.BRANGUS, 
            LocalDate.of(2022, 11, 5), 
            null,
            UUID.randomUUID(), 
            UUID.randomUUID(), 
            UUID.randomUUID()
        );
        String folioReemo = "REEMO-2024-12345";
        UUID updatedBy = UUID.randomUUID();
        
        // When
        Animal updated = original.updateFolioReemo(folioReemo, updatedBy);
        
        // Then
        assertNotSame(original, updated);
        assertNull(original.getFolioReemo());
        assertEquals(folioReemo, updated.getFolioReemo());
    }
    
    @Test
    void testBuilder_shouldCreateAnimalWithAllFields() {
        // Given
        UUID animalId = UUID.randomUUID();
        String arete = "BUILDER001";
        String areteAnterior = "OLD001";
        Sex sexo = Sex.HEMBRA;
        Breed raza = Breed.SANTA_GERTRUDIS;
        LocalDate fechaNacimiento = LocalDate.of(2021, 7, 15);
        Integer meses = 36;
        LocalDate fechaAretado = LocalDate.of(2021, 8, 1);
        CattleType tipo = CattleType.VIENTRE;
        CattleStatus status = CattleStatus.PRENADA;
        String folioReemo = "REEMO-123";
        String nota = "Nota de prueba";
        UUID madreId = UUID.randomUUID();
        UUID padreId = UUID.randomUUID();
        UUID ranchoId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        
        // When
        Animal animal = new Animal.Builder()
            .animalId(animalId)
            .arete(arete)
            .areteAnterior(areteAnterior)
            .sexo(sexo)
            .raza(raza)
            .fechaNacimiento(fechaNacimiento)
            .meses(meses)
            .fechaAretado(fechaAretado)
            .tipo(tipo)
            .status(status)
            .folioReemo(folioReemo)
            .nota(nota)
            .madreId(madreId)
            .padreId(padreId)
            .ranchoId(ranchoId)
            .tenantId(tenantId)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();
        
        // Then
        assertEquals(animalId, animal.getAnimalId());
        assertEquals(arete, animal.getArete());
        assertEquals(areteAnterior, animal.getAreteAnterior());
        assertEquals(sexo, animal.getSexo());
        assertEquals(raza, animal.getRaza());
        assertEquals(fechaNacimiento, animal.getFechaNacimiento());
        assertEquals(meses, animal.getMeses());
        assertEquals(fechaAretado, animal.getFechaAretado());
        assertEquals(tipo, animal.getTipo());
        assertEquals(status, animal.getStatus());
        assertEquals(folioReemo, animal.getFolioReemo());
        assertEquals(nota, animal.getNota());
        assertEquals(madreId, animal.getMadreId());
        assertEquals(padreId, animal.getPadreId());
        assertEquals(ranchoId, animal.getRanchoId());
        assertEquals(tenantId, animal.getTenantId());
        assertEquals(createdBy, animal.getCreatedBy());
    }
    
    @Test
    void testBuilderFrom_shouldCopyAllFieldsFromExistingAnimal() {
        // Given
        Animal original = Animal.create(
            "COPY001", 
            Sex.MACHO, 
            Breed.CRUZADA, 
            LocalDate.of(2023, 9, 1), 
            CattleType.CRIA,
            UUID.randomUUID(), 
            UUID.randomUUID(), 
            UUID.randomUUID()
        );
        
        // When
        Animal copy = new Animal.Builder()
            .from(original)
            .build();
        
        // Then
        assertNotSame(original, copy);
        assertEquals(original.getAnimalId(), copy.getAnimalId());
        assertEquals(original.getArete(), copy.getArete());
        assertEquals(original.getSexo(), copy.getSexo());
        assertEquals(original.getRaza(), copy.getRaza());
        assertEquals(original.getFechaNacimiento(), copy.getFechaNacimiento());
        assertEquals(original.getTipo(), copy.getTipo());
        assertEquals(original.getStatus(), copy.getStatus());
    }
}
