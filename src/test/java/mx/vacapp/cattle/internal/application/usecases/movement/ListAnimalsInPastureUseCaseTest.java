package mx.vacapp.cattle.internal.application.usecases.movement;

import mx.vacapp.cattle.internal.application.usecases.commands.AnimalResult;
import mx.vacapp.cattle.internal.domain.model.*;
import mx.vacapp.cattle.internal.domain.model.exceptions.InvalidPastureException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.PastureHistoryRepository;
import mx.vacapp.cattle.internal.domain.repository.WeightRepository;
import mx.vacapp.cattle.internal.infrastructure.integration.GeographyServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ListAnimalsInPastureUseCase.
 * Tests cover successful listing, empty pastures, invalid pastures,
 * and enrichment with calculated fields.
 */
@ExtendWith(MockitoExtension.class)
class ListAnimalsInPastureUseCaseTest {
    
    @Mock
    private GeographyServiceClient geographyServiceClient;
    
    @Mock
    private PastureHistoryRepository pastureHistoryRepository;
    
    @Mock
    private AnimalRepository animalRepository;
    
    @Mock
    private WeightRepository weightRepository;
    
    @InjectMocks
    private ListAnimalsInPastureUseCase useCase;
    
    private UUID potreroId;
    private UUID tenantId;
    private UUID ranchoId;
    private UUID createdBy;
    
    @BeforeEach
    void setUp() {
        potreroId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        ranchoId = UUID.randomUUID();
        createdBy = UUID.randomUUID();
    }
    
    @Test
    void execute_shouldReturnEmptyList_whenPotreroHasNoAnimals() {
        // Given
        doNothing().when(geographyServiceClient).validatePotreroActive(potreroId);
        when(pastureHistoryRepository.findAnimalIdsByPotreroId(potreroId))
            .thenReturn(List.of());
        
        // When
        List<AnimalResult> results = useCase.execute(potreroId);
        
        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(geographyServiceClient, times(1)).validatePotreroActive(potreroId);
        verify(pastureHistoryRepository, times(1)).findAnimalIdsByPotreroId(potreroId);
        verifyNoInteractions(animalRepository, weightRepository);
    }
    
    @Test
    void execute_shouldReturnSortedAnimals_whenPotreroHasAnimals() {
        // Given
        UUID animal1Id = UUID.randomUUID();
        UUID animal2Id = UUID.randomUUID();
        
        Animal animal1 = createAnimal(animal1Id, "Z123", Sex.MACHO, Breed.ANGUS);
        Animal animal2 = createAnimal(animal2Id, "A456", Sex.HEMBRA, Breed.CHAROLAIS);
        
        doNothing().when(geographyServiceClient).validatePotreroActive(potreroId);
        when(pastureHistoryRepository.findAnimalIdsByPotreroId(potreroId))
            .thenReturn(List.of(animal1Id, animal2Id));
        when(animalRepository.findById(animal1Id)).thenReturn(Optional.of(animal1));
        when(animalRepository.findById(animal2Id)).thenReturn(Optional.of(animal2));
        when(weightRepository.findLatestWeight(animal1Id)).thenReturn(Optional.empty());
        when(weightRepository.findLatestWeight(animal2Id)).thenReturn(Optional.empty());
        
        // When
        List<AnimalResult> results = useCase.execute(potreroId);
        
        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        // Verify sorted by arete ASC (A456 should come before Z123)
        assertEquals("A456", results.get(0).arete());
        assertEquals("Z123", results.get(1).arete());
        verify(geographyServiceClient, times(1)).validatePotreroActive(potreroId);
        verify(pastureHistoryRepository, times(1)).findAnimalIdsByPotreroId(potreroId);
        verify(animalRepository, times(1)).findById(animal1Id);
        verify(animalRepository, times(1)).findById(animal2Id);
    }
    
    @Test
    void execute_shouldEnrichWithWeight_whenAnimalHasWeightRecords() {
        // Given
        UUID animalId = UUID.randomUUID();
        Animal animal = createAnimal(animalId, "A123", Sex.MACHO, Breed.ANGUS);
        WeightRecord weight = createWeightRecord(animalId, new BigDecimal("450.50"));
        
        doNothing().when(geographyServiceClient).validatePotreroActive(potreroId);
        when(pastureHistoryRepository.findAnimalIdsByPotreroId(potreroId))
            .thenReturn(List.of(animalId));
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(animal));
        when(weightRepository.findLatestWeight(animalId))
            .thenReturn(Optional.of(weight));
        
        // When
        List<AnimalResult> results = useCase.execute(potreroId);
        
        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        AnimalResult result = results.get(0);
        assertEquals("A123", result.arete());
        assertNotNull(result.pesoActual());
        assertEquals(new BigDecimal("450.50"), result.pesoActual());
        assertEquals(potreroId, result.potreroActual());
        verify(weightRepository, times(1)).findLatestWeight(animalId);
    }
    
    @Test
    void execute_shouldCalculateAgeInRealTime_forAllAnimals() {
        // Given
        UUID animalId = UUID.randomUUID();
        LocalDate fechaNacimiento = LocalDate.now().minusMonths(18);
        Animal animal = createAnimalWithBirthDate(animalId, "A123", fechaNacimiento);
        
        doNothing().when(geographyServiceClient).validatePotreroActive(potreroId);
        when(pastureHistoryRepository.findAnimalIdsByPotreroId(potreroId))
            .thenReturn(List.of(animalId));
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(animal));
        when(weightRepository.findLatestWeight(animalId)).thenReturn(Optional.empty());
        
        // When
        List<AnimalResult> results = useCase.execute(potreroId);
        
        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        AnimalResult result = results.get(0);
        // Age should be calculated in real-time (approximately 18 months)
        assertNotNull(result.meses());
        assertTrue(result.meses() >= 17 && result.meses() <= 19);
    }
    
    @Test
    void execute_shouldThrowInvalidPastureException_whenPotreroDoesNotExist() {
        // Given
        UUID invalidPotreroId = UUID.randomUUID();
        doThrow(new InvalidPastureException("Potrero no existe o está inactivo"))
            .when(geographyServiceClient).validatePotreroActive(invalidPotreroId);
        
        // When & Then
        InvalidPastureException exception = assertThrows(
            InvalidPastureException.class,
            () -> useCase.execute(invalidPotreroId)
        );
        
        assertEquals("Potrero no existe o está inactivo", exception.getMessage());
        verify(geographyServiceClient, times(1)).validatePotreroActive(invalidPotreroId);
        verifyNoInteractions(pastureHistoryRepository, animalRepository, weightRepository);
    }
    
    @Test
    void execute_shouldThrowIllegalArgumentException_whenPotreroIdIsNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> useCase.execute(null)
        );
        
        assertEquals("potreroId no puede ser null", exception.getMessage());
        verifyNoInteractions(geographyServiceClient, pastureHistoryRepository, animalRepository, weightRepository);
    }
    
    @Test
    void execute_shouldSkipMissingAnimals_whenAnimalNotFoundInRepository() {
        // Given
        UUID animal1Id = UUID.randomUUID();
        UUID animal2Id = UUID.randomUUID();
        UUID animal3Id = UUID.randomUUID();
        
        Animal animal1 = createAnimal(animal1Id, "A123", Sex.MACHO, Breed.ANGUS);
        Animal animal3 = createAnimal(animal3Id, "C789", Sex.HEMBRA, Breed.BRAHMAN);
        
        doNothing().when(geographyServiceClient).validatePotreroActive(potreroId);
        when(pastureHistoryRepository.findAnimalIdsByPotreroId(potreroId))
            .thenReturn(List.of(animal1Id, animal2Id, animal3Id));
        when(animalRepository.findById(animal1Id)).thenReturn(Optional.of(animal1));
        when(animalRepository.findById(animal2Id)).thenReturn(Optional.empty()); // Missing animal
        when(animalRepository.findById(animal3Id)).thenReturn(Optional.of(animal3));
        when(weightRepository.findLatestWeight(animal1Id)).thenReturn(Optional.empty());
        when(weightRepository.findLatestWeight(animal3Id)).thenReturn(Optional.empty());
        
        // When
        List<AnimalResult> results = useCase.execute(potreroId);
        
        // Then
        assertNotNull(results);
        assertEquals(2, results.size()); // Should only include animals that exist
        assertEquals("A123", results.get(0).arete());
        assertEquals("C789", results.get(1).arete());
        verify(animalRepository, times(1)).findById(animal2Id);
    }
    
    @Test
    void execute_shouldHandleMultipleAnimalsWithDifferentWeights() {
        // Given
        UUID animal1Id = UUID.randomUUID();
        UUID animal2Id = UUID.randomUUID();
        
        Animal animal1 = createAnimal(animal1Id, "A001", Sex.MACHO, Breed.ANGUS);
        Animal animal2 = createAnimal(animal2Id, "B002", Sex.HEMBRA, Breed.CHAROLAIS);
        
        WeightRecord weight1 = createWeightRecord(animal1Id, new BigDecimal("500.00"));
        
        doNothing().when(geographyServiceClient).validatePotreroActive(potreroId);
        when(pastureHistoryRepository.findAnimalIdsByPotreroId(potreroId))
            .thenReturn(List.of(animal1Id, animal2Id));
        when(animalRepository.findById(animal1Id)).thenReturn(Optional.of(animal1));
        when(animalRepository.findById(animal2Id)).thenReturn(Optional.of(animal2));
        when(weightRepository.findLatestWeight(animal1Id)).thenReturn(Optional.of(weight1));
        when(weightRepository.findLatestWeight(animal2Id)).thenReturn(Optional.empty());
        
        // When
        List<AnimalResult> results = useCase.execute(potreroId);
        
        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        
        AnimalResult result1 = results.get(0);
        assertEquals("A001", result1.arete());
        assertEquals(new BigDecimal("500.00"), result1.pesoActual());
        
        AnimalResult result2 = results.get(1);
        assertEquals("B002", result2.arete());
        assertNull(result2.pesoActual()); // No weight recorded
    }
    
    // Helper methods
    
    private Animal createAnimal(UUID animalId, String arete, Sex sexo, Breed raza) {
        return createAnimalWithBirthDate(animalId, arete, LocalDate.now().minusMonths(12));
    }
    
    private Animal createAnimalWithBirthDate(UUID animalId, String arete, LocalDate fechaNacimiento) {
        int meses = AgeCalculator.calculateMonths(fechaNacimiento, LocalDate.now());
        
        return new Animal.Builder()
            .animalId(animalId)
            .arete(arete.toUpperCase())
            .sexo(Sex.MACHO)
            .raza(Breed.ANGUS)
            .fechaNacimiento(fechaNacimiento)
            .meses(meses)
            .tipo(CattleType.VENTA)
            .status(CattleStatus.ACTIVA)
            .ranchoId(ranchoId)
            .tenantId(tenantId)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();
    }
    
    private WeightRecord createWeightRecord(UUID animalId, BigDecimal pesoKg) {
        return WeightRecord.create(
            animalId,
            pesoKg,
            LocalDate.now(),
            null,
            createdBy
        );
    }
}
