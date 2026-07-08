package mx.vacapp.cattle.internal.application.usecases.movement;

import mx.vacapp.cattle.internal.application.usecases.commands.PastureHistoryResult;
import mx.vacapp.cattle.internal.domain.model.*;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.PastureHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para GetMovementHistoryUseCase.
 * Valida la recuperación del historial completo de movimientos de un animal entre potreros.
 */
@ExtendWith(MockitoExtension.class)
class GetMovementHistoryUseCaseTest {
    
    @Mock
    private AnimalRepository animalRepository;
    
    @Mock
    private PastureHistoryRepository pastureHistoryRepository;
    
    @InjectMocks
    private GetMovementHistoryUseCase getMovementHistoryUseCase;
    
    private UUID animalId;
    private UUID tenantId;
    private UUID ranchoId;
    private UUID createdBy;
    private Animal testAnimal;
    
    @BeforeEach
    void setUp() {
        animalId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        ranchoId = UUID.randomUUID();
        createdBy = UUID.randomUUID();
        
        // Crear animal de prueba
        testAnimal = Animal.create(
            "TEST001",
            Sex.HEMBRA,
            Breed.ANGUS,
            LocalDate.now().minusMonths(12),
            CattleType.VENTA,
            ranchoId,
            tenantId,
            createdBy
        );
        
        testAnimal = new Animal.Builder()
            .from(testAnimal)
            .animalId(animalId)
            .build();
    }
    
    @Test
    void execute_shouldReturnEmptyList_whenAnimalHasNoMovementHistory() {
        // Given
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(pastureHistoryRepository.findHistoryByAnimalId(animalId))
            .thenReturn(new ArrayList<>());
        
        // When
        List<PastureHistoryResult> result = getMovementHistoryUseCase.execute(animalId);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Historial debe estar vacío si el animal nunca estuvo en un potrero");
        
        verify(animalRepository).findById(animalId);
        verify(pastureHistoryRepository).findHistoryByAnimalId(animalId);
    }
    
    @Test
    void execute_shouldReturnSingleRecord_whenAnimalIsInCurrentLocation() {
        // Given
        UUID potreroId = UUID.randomUUID();
        Instant fechaEntrada = Instant.now().minus(30, ChronoUnit.DAYS);
        
        PastureHistory currentLocation = PastureHistory.create(
            animalId,
            potreroId,
            fechaEntrada,
            createdBy
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(pastureHistoryRepository.findHistoryByAnimalId(animalId))
            .thenReturn(List.of(currentLocation));
        
        // When
        List<PastureHistoryResult> result = getMovementHistoryUseCase.execute(animalId);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size(), "Debe haber un solo registro");
        
        PastureHistoryResult record = result.get(0);
        assertEquals(animalId, record.animalId());
        assertEquals(potreroId, record.potreroId());
        assertEquals(fechaEntrada, record.fechaEntrada());
        assertNull(record.fechaSalida(), "Debe ser null porque es ubicación actual");
        assertTrue(record.isCurrent(), "Debe indicar que es ubicación actual");
        
        // Verificar que diasPermanencia se calcula hasta NOW
        assertNotNull(record.diasPermanencia());
        assertTrue(record.diasPermanencia() >= 29 && record.diasPermanencia() <= 31,
                   "Días de permanencia deben ser ~30, fue: " + record.diasPermanencia());
        
        verify(animalRepository).findById(animalId);
        verify(pastureHistoryRepository).findHistoryByAnimalId(animalId);
    }
    
    @Test
    void execute_shouldReturnMultipleRecords_whenAnimalHasMovementHistory() {
        // Given
        UUID potrero1 = UUID.randomUUID();
        UUID potrero2 = UUID.randomUUID();
        UUID potrero3 = UUID.randomUUID();
        
        Instant entrada1 = Instant.now().minus(90, ChronoUnit.DAYS);
        Instant salida1 = Instant.now().minus(60, ChronoUnit.DAYS);
        
        Instant entrada2 = Instant.now().minus(60, ChronoUnit.DAYS);
        Instant salida2 = Instant.now().minus(15, ChronoUnit.DAYS);
        
        Instant entrada3 = Instant.now().minus(15, ChronoUnit.DAYS);
        
        // Historial ordenado por fecha_entrada DESC (más reciente primero)
        PastureHistory record3 = PastureHistory.create(animalId, potrero3, entrada3, createdBy); // Actual
        PastureHistory record2 = PastureHistory.restore(
            UUID.randomUUID(), animalId, potrero2, entrada2, salida2, 45, entrada2, createdBy
        );
        PastureHistory record1 = PastureHistory.restore(
            UUID.randomUUID(), animalId, potrero1, entrada1, salida1, 30, entrada1, createdBy
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(pastureHistoryRepository.findHistoryByAnimalId(animalId))
            .thenReturn(List.of(record3, record2, record1)); // Ya ordenado DESC
        
        // When
        List<PastureHistoryResult> result = getMovementHistoryUseCase.execute(animalId);
        
        // Then
        assertNotNull(result);
        assertEquals(3, result.size(), "Debe haber 3 registros de historial");
        
        // Verificar orden cronológico descendente (más reciente primero)
        PastureHistoryResult first = result.get(0);
        PastureHistoryResult second = result.get(1);
        PastureHistoryResult third = result.get(2);
        
        // Registro más reciente (actual)
        assertEquals(potrero3, first.potreroId());
        assertNull(first.fechaSalida());
        assertTrue(first.isCurrent());
        assertTrue(first.diasPermanencia() >= 14 && first.diasPermanencia() <= 16);
        
        // Registro intermedio
        assertEquals(potrero2, second.potreroId());
        assertEquals(salida2, second.fechaSalida());
        assertFalse(second.isCurrent());
        assertTrue(second.diasPermanencia() >= 44 && second.diasPermanencia() <= 46);
        
        // Registro más antiguo
        assertEquals(potrero1, third.potreroId());
        assertEquals(salida1, third.fechaSalida());
        assertFalse(third.isCurrent());
        assertTrue(third.diasPermanencia() >= 29 && third.diasPermanencia() <= 31);
        
        verify(animalRepository).findById(animalId);
        verify(pastureHistoryRepository).findHistoryByAnimalId(animalId);
    }
    
    @Test
    void execute_shouldCalculateDiasPermanenciaCorrectly_forHistoricalRecords() {
        // Given
        UUID potreroId = UUID.randomUUID();
        
        // Animal estuvo en potrero del día 1 al día 30 (29 días completos)
        Instant fechaEntrada = Instant.now().minus(100, ChronoUnit.DAYS);
        Instant fechaSalida = Instant.now().minus(71, ChronoUnit.DAYS);
        
        PastureHistory historicalRecord = PastureHistory.restore(
            UUID.randomUUID(),
            animalId,
            potreroId,
            fechaEntrada,
            fechaSalida,
            null, // diasPermanencia será calculado por PastureHistoryResult.fromDomain
            fechaEntrada,
            createdBy
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(pastureHistoryRepository.findHistoryByAnimalId(animalId))
            .thenReturn(List.of(historicalRecord));
        
        // When
        List<PastureHistoryResult> result = getMovementHistoryUseCase.execute(animalId);
        
        // Then
        assertEquals(1, result.size());
        PastureHistoryResult record = result.get(0);
        
        assertNotNull(record.diasPermanencia());
        // Debe ser exactamente 29 días entre fechaEntrada y fechaSalida
        assertEquals(29, record.diasPermanencia(),
                     "Días de permanencia deben ser 29 (100 - 71)");
        
        assertFalse(record.isCurrent(), "No debe ser ubicación actual");
        verify(animalRepository).findById(animalId);
        verify(pastureHistoryRepository).findHistoryByAnimalId(animalId);
    }
    
    @Test
    void execute_shouldCalculateDiasPermanenciaUntilNow_forCurrentLocation() {
        // Given
        UUID potreroId = UUID.randomUUID();
        
        // Animal entró hace 45 días y aún está ahí
        Instant fechaEntrada = Instant.now().minus(45, ChronoUnit.DAYS);
        
        PastureHistory currentLocation = PastureHistory.create(
            animalId,
            potreroId,
            fechaEntrada,
            createdBy
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(pastureHistoryRepository.findHistoryByAnimalId(animalId))
            .thenReturn(List.of(currentLocation));
        
        // When
        List<PastureHistoryResult> result = getMovementHistoryUseCase.execute(animalId);
        
        // Then
        assertEquals(1, result.size());
        PastureHistoryResult record = result.get(0);
        
        assertNull(record.fechaSalida(), "fechaSalida debe ser null");
        assertTrue(record.isCurrent(), "Debe ser ubicación actual");
        assertNotNull(record.diasPermanencia());
        
        // Debe calcular hasta NOW (aproximadamente 45 días)
        assertTrue(record.diasPermanencia() >= 44 && record.diasPermanencia() <= 46,
                   "Días de permanencia deben ser ~45, fue: " + record.diasPermanencia());
        
        verify(animalRepository).findById(animalId);
        verify(pastureHistoryRepository).findHistoryByAnimalId(animalId);
    }
    
    @Test
    void execute_shouldThrowAnimalNotFoundException_whenAnimalDoesNotExist() {
        // Given
        when(animalRepository.findById(animalId)).thenReturn(Optional.empty());
        
        // When & Then
        AnimalNotFoundException exception = assertThrows(
            AnimalNotFoundException.class,
            () -> getMovementHistoryUseCase.execute(animalId)
        );
        
        assertEquals("Animal no encontrado con ID: " + animalId, exception.getMessage());
        
        verify(animalRepository).findById(animalId);
        verifyNoInteractions(pastureHistoryRepository);
    }
    
    @Test
    void execute_shouldThrowIllegalArgumentException_whenAnimalIdIsNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> getMovementHistoryUseCase.execute(null)
        );
        
        assertEquals("animalId no puede ser null", exception.getMessage());
        
        verifyNoInteractions(animalRepository);
        verifyNoInteractions(pastureHistoryRepository);
    }
    
    @Test
    void execute_shouldPreserveChronologicalOrder_whenRepositoryReturnsOrderedData() {
        // Given
        UUID potrero1 = UUID.randomUUID();
        UUID potrero2 = UUID.randomUUID();
        UUID potrero3 = UUID.randomUUID();
        UUID potrero4 = UUID.randomUUID();
        
        Instant now = Instant.now();
        
        // Crear registros con timestamps ordenados DESC
        PastureHistory newest = PastureHistory.create(animalId, potrero4, now.minus(5, ChronoUnit.DAYS), createdBy);
        PastureHistory recent = PastureHistory.restore(
            UUID.randomUUID(), animalId, potrero3,
            now.minus(20, ChronoUnit.DAYS), now.minus(5, ChronoUnit.DAYS),
            15, now.minus(20, ChronoUnit.DAYS), createdBy
        );
        PastureHistory older = PastureHistory.restore(
            UUID.randomUUID(), animalId, potrero2,
            now.minus(50, ChronoUnit.DAYS), now.minus(20, ChronoUnit.DAYS),
            30, now.minus(50, ChronoUnit.DAYS), createdBy
        );
        PastureHistory oldest = PastureHistory.restore(
            UUID.randomUUID(), animalId, potrero1,
            now.minus(100, ChronoUnit.DAYS), now.minus(50, ChronoUnit.DAYS),
            50, now.minus(100, ChronoUnit.DAYS), createdBy
        );
        
        // Repositorio retorna en orden DESC (más reciente primero)
        List<PastureHistory> orderedHistory = List.of(newest, recent, older, oldest);
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(pastureHistoryRepository.findHistoryByAnimalId(animalId))
            .thenReturn(orderedHistory);
        
        // When
        List<PastureHistoryResult> result = getMovementHistoryUseCase.execute(animalId);
        
        // Then
        assertEquals(4, result.size());
        
        // Verificar que el orden se preserva (más reciente primero)
        assertEquals(potrero4, result.get(0).potreroId(), "Primero debe ser el más reciente");
        assertEquals(potrero3, result.get(1).potreroId());
        assertEquals(potrero2, result.get(2).potreroId());
        assertEquals(potrero1, result.get(3).potreroId(), "Último debe ser el más antiguo");
        
        // Verificar que solo el primero es ubicación actual
        assertTrue(result.get(0).isCurrent());
        assertFalse(result.get(1).isCurrent());
        assertFalse(result.get(2).isCurrent());
        assertFalse(result.get(3).isCurrent());
    }
    
    @Test
    void execute_shouldMapAllFields_fromDomainToResult() {
        // Given
        UUID historyId = UUID.randomUUID();
        UUID potreroId = UUID.randomUUID();
        Instant fechaEntrada = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant fechaSalida = Instant.now().minus(3, ChronoUnit.DAYS);
        Instant createdAt = fechaEntrada;
        
        PastureHistory history = PastureHistory.restore(
            historyId,
            animalId,
            potreroId,
            fechaEntrada,
            fechaSalida,
            7, // diasPermanencia
            createdAt,
            createdBy
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(pastureHistoryRepository.findHistoryByAnimalId(animalId))
            .thenReturn(List.of(history));
        
        // When
        List<PastureHistoryResult> result = getMovementHistoryUseCase.execute(animalId);
        
        // Then
        assertEquals(1, result.size());
        PastureHistoryResult record = result.get(0);
        
        // Verificar que todos los campos se mapean correctamente
        assertEquals(historyId, record.historyId());
        assertEquals(animalId, record.animalId());
        assertEquals(potreroId, record.potreroId());
        assertEquals(fechaEntrada, record.fechaEntrada());
        assertEquals(fechaSalida, record.fechaSalida());
        assertEquals(createdAt, record.createdAt());
        assertEquals(createdBy, record.createdBy());
        assertEquals(7, record.diasPermanencia());
    }
}
