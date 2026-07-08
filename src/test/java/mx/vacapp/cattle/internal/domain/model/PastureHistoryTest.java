package mx.vacapp.cattle.internal.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitario para el Value Object PastureHistory.
 * 
 * Valida:
 * - Creación de registros de entrada a potrero
 * - Marcado de salida de potrero
 * - Cálculo de días de permanencia
 * - Validaciones de negocio
 */
class PastureHistoryTest {
    
    @Test
    void testCreate_withCurrentTimestamp_shouldCreateValidRecord() {
        // Given
        UUID animalId = UUID.randomUUID();
        UUID potreroId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        
        // When
        PastureHistory history = PastureHistory.create(animalId, potreroId, createdBy);
        
        // Then
        assertNotNull(history.getHistoryId());
        assertEquals(animalId, history.getAnimalId());
        assertEquals(potreroId, history.getPotreroId());
        assertNotNull(history.getFechaEntrada());
        assertNull(history.getFechaSalida());
        assertNull(history.getDiasPermanencia());
        assertEquals(createdBy, history.getCreatedBy());
        assertTrue(history.isCurrent());
    }
    
    @Test
    void testCreate_withSpecificTimestamp_shouldCreateValidRecord() {
        // Given
        UUID animalId = UUID.randomUUID();
        UUID potreroId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        Instant fechaEntrada = Instant.now().minus(10, ChronoUnit.DAYS);
        
        // When
        PastureHistory history = PastureHistory.create(animalId, potreroId, fechaEntrada, createdBy);
        
        // Then
        assertNotNull(history.getHistoryId());
        assertEquals(animalId, history.getAnimalId());
        assertEquals(potreroId, history.getPotreroId());
        assertEquals(fechaEntrada, history.getFechaEntrada());
        assertNull(history.getFechaSalida());
        assertTrue(history.isCurrent());
    }
    
    @Test
    void testMarkExit_withCurrentTimestamp_shouldUpdateFechaSalida() {
        // Given
        UUID animalId = UUID.randomUUID();
        UUID potreroId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        PastureHistory history = PastureHistory.create(animalId, potreroId, createdBy);
        
        // When
        PastureHistory updatedHistory = history.markExit();
        
        // Then
        assertNotNull(updatedHistory.getFechaSalida());
        assertNotNull(updatedHistory.getDiasPermanencia());
        assertFalse(updatedHistory.isCurrent());
        assertTrue(updatedHistory.getFechaSalida().isAfter(updatedHistory.getFechaEntrada()) ||
                   updatedHistory.getFechaSalida().equals(updatedHistory.getFechaEntrada()));
    }
    
    @Test
    void testMarkExit_withSpecificTimestamp_shouldCalculateDiasPermanencia() {
        // Given
        UUID animalId = UUID.randomUUID();
        UUID potreroId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        Instant fechaEntrada = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant fechaSalida = Instant.now();
        PastureHistory history = PastureHistory.create(animalId, potreroId, fechaEntrada, createdBy);
        
        // When
        PastureHistory updatedHistory = history.markExit(fechaSalida);
        
        // Then
        assertNotNull(updatedHistory.getFechaSalida());
        assertEquals(fechaSalida, updatedHistory.getFechaSalida());
        assertNotNull(updatedHistory.getDiasPermanencia());
        assertTrue(updatedHistory.getDiasPermanencia() >= 9 && updatedHistory.getDiasPermanencia() <= 10);
        assertFalse(updatedHistory.isCurrent());
    }
    
    @Test
    void testMarkExit_withEarlierDate_shouldThrowException() {
        // Given
        UUID animalId = UUID.randomUUID();
        UUID potreroId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        Instant fechaEntrada = Instant.now();
        Instant fechaSalida = fechaEntrada.minus(1, ChronoUnit.DAYS);
        PastureHistory history = PastureHistory.create(animalId, potreroId, fechaEntrada, createdBy);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> history.markExit(fechaSalida));
    }
    
    @Test
    void testRestore_shouldCreateHistoryWithAllFields() {
        // Given
        UUID historyId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID potreroId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        Instant fechaEntrada = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant fechaSalida = Instant.now().minus(2, ChronoUnit.DAYS);
        Integer diasPermanencia = 8;
        Instant createdAt = fechaEntrada;
        
        // When
        PastureHistory history = PastureHistory.restore(
                historyId, animalId, potreroId,
                fechaEntrada, fechaSalida, diasPermanencia,
                createdAt, createdBy
        );
        
        // Then
        assertEquals(historyId, history.getHistoryId());
        assertEquals(animalId, history.getAnimalId());
        assertEquals(potreroId, history.getPotreroId());
        assertEquals(fechaEntrada, history.getFechaEntrada());
        assertEquals(fechaSalida, history.getFechaSalida());
        assertEquals(diasPermanencia, history.getDiasPermanencia());
        assertEquals(createdAt, history.getCreatedAt());
        assertEquals(createdBy, history.getCreatedBy());
        assertFalse(history.isCurrent());
    }
    
    @Test
    void testBuilder_withNullAnimalId_shouldThrowException() {
        // Given
        UUID potreroId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                new PastureHistory.Builder()
                        .historyId(UUID.randomUUID())
                        .animalId(null)
                        .potreroId(potreroId)
                        .fechaEntrada(Instant.now())
                        .createdAt(Instant.now())
                        .createdBy(createdBy)
                        .build()
        );
    }
    
    @Test
    void testBuilder_withNullPotreroId_shouldThrowException() {
        // Given
        UUID animalId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                new PastureHistory.Builder()
                        .historyId(UUID.randomUUID())
                        .animalId(animalId)
                        .potreroId(null)
                        .fechaEntrada(Instant.now())
                        .createdAt(Instant.now())
                        .createdBy(createdBy)
                        .build()
        );
    }
    
    @Test
    void testBuilder_withNullFechaEntrada_shouldThrowException() {
        // Given
        UUID animalId = UUID.randomUUID();
        UUID potreroId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                new PastureHistory.Builder()
                        .historyId(UUID.randomUUID())
                        .animalId(animalId)
                        .potreroId(potreroId)
                        .fechaEntrada(null)
                        .createdAt(Instant.now())
                        .createdBy(createdBy)
                        .build()
        );
    }
    
    @Test
    void testBuilder_withNullCreatedBy_shouldThrowException() {
        // Given
        UUID animalId = UUID.randomUUID();
        UUID potreroId = UUID.randomUUID();
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                new PastureHistory.Builder()
                        .historyId(UUID.randomUUID())
                        .animalId(animalId)
                        .potreroId(potreroId)
                        .fechaEntrada(Instant.now())
                        .createdAt(Instant.now())
                        .createdBy(null)
                        .build()
        );
    }
    
    @Test
    void testIsCurrent_withNullFechaSalida_shouldReturnTrue() {
        // Given
        UUID animalId = UUID.randomUUID();
        UUID potreroId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        PastureHistory history = PastureHistory.create(animalId, potreroId, createdBy);
        
        // When & Then
        assertTrue(history.isCurrent());
    }
    
    @Test
    void testIsCurrent_withFechaSalida_shouldReturnFalse() {
        // Given
        UUID animalId = UUID.randomUUID();
        UUID potreroId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        PastureHistory history = PastureHistory.create(animalId, potreroId, createdBy);
        PastureHistory updatedHistory = history.markExit();
        
        // When & Then
        assertFalse(updatedHistory.isCurrent());
    }
}
