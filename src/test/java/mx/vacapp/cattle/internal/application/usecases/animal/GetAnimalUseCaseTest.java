package mx.vacapp.cattle.internal.application.usecases.animal;

import mx.vacapp.cattle.internal.application.usecases.commands.AnimalResult;
import mx.vacapp.cattle.internal.domain.model.*;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.PastureHistoryRepository;
import mx.vacapp.cattle.internal.domain.repository.WeightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para GetAnimalUseCase.
 * Valida la recuperación de animales con cálculos en tiempo real y datos enriquecidos.
 */
@ExtendWith(MockitoExtension.class)
class GetAnimalUseCaseTest {
    
    @Mock
    private AnimalRepository animalRepository;
    
    @Mock
    private WeightRepository weightRepository;
    
    @Mock
    private PastureHistoryRepository pastureHistoryRepository;
    
    @InjectMocks
    private GetAnimalUseCase getAnimalUseCase;
    
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
        
        // Crear animal de prueba con fecha de nacimiento hace 12 meses
        LocalDate fechaNacimiento = LocalDate.now().minusMonths(12);
        testAnimal = Animal.create(
            "TEST001",
            Sex.HEMBRA,
            Breed.ANGUS,
            fechaNacimiento,
            CattleType.VENTA,
            ranchoId,
            tenantId,
            createdBy
        );
        
        // Reemplazar con instancia que tiene animalId específico
        testAnimal = new Animal.Builder()
            .from(testAnimal)
            .animalId(animalId)
            .build();
    }
    
    @Test
    void execute_shouldReturnEnrichedAnimalResult_whenAnimalExists() {
        // Given
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(weightRepository.findLatestWeight(animalId)).thenReturn(Optional.empty());
        when(pastureHistoryRepository.findCurrentByAnimalId(animalId)).thenReturn(Optional.empty());
        
        // When
        AnimalResult result = getAnimalUseCase.execute(animalId);
        
        // Then
        assertNotNull(result);
        assertEquals(animalId, result.animalId());
        assertEquals("TEST001", result.arete());
        assertEquals("hembra", result.sexo());
        assertEquals("angus", result.raza());
        
        // Verificar que meses se calcula en tiempo real (debe ser ~12)
        assertNotNull(result.meses());
        assertTrue(result.meses() >= 11 && result.meses() <= 13, 
                   "Edad debe ser aproximadamente 12 meses, fue: " + result.meses());
        
        // Verificar campos enriquecidos
        assertNull(result.pesoActual(), "No debe tener peso si no hay registros");
        assertNull(result.potreroActual(), "No debe tener potrero si no hay historial");
        assertNull(result.nombreMadre(), "No debe tener madre si madreId es null");
        assertNull(result.nombrePadre(), "No debe tener padre si padreId es null");
        
        // Verificar que se llamaron los repositorios correctos
        verify(animalRepository).findById(animalId);
        verify(weightRepository).findLatestWeight(animalId);
        verify(pastureHistoryRepository).findCurrentByAnimalId(animalId);
    }
    
    @Test
    void execute_shouldIncludePesoActual_whenWeightRecordsExist() {
        // Given
        BigDecimal pesoEsperado = new BigDecimal("450.75");
        WeightRecord latestWeight = WeightRecord.create(
            animalId,
            pesoEsperado,
            LocalDate.now(),
            "Pesaje de rutina",
            createdBy
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(weightRepository.findLatestWeight(animalId)).thenReturn(Optional.of(latestWeight));
        when(pastureHistoryRepository.findCurrentByAnimalId(animalId)).thenReturn(Optional.empty());
        
        // When
        AnimalResult result = getAnimalUseCase.execute(animalId);
        
        // Then
        assertNotNull(result.pesoActual());
        assertEquals(pesoEsperado, result.pesoActual());
        verify(weightRepository).findLatestWeight(animalId);
    }
    
    @Test
    void execute_shouldIncludePotreroActual_whenCurrentLocationExists() {
        // Given
        UUID potreroId = UUID.randomUUID();
        PastureHistory currentLocation = PastureHistory.create(
            animalId,
            potreroId,
            createdBy
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(testAnimal));
        when(weightRepository.findLatestWeight(animalId)).thenReturn(Optional.empty());
        when(pastureHistoryRepository.findCurrentByAnimalId(animalId))
            .thenReturn(Optional.of(currentLocation));
        
        // When
        AnimalResult result = getAnimalUseCase.execute(animalId);
        
        // Then
        assertNotNull(result.potreroActual());
        assertEquals(potreroId, result.potreroActual());
        verify(pastureHistoryRepository).findCurrentByAnimalId(animalId);
    }
    
    @Test
    void execute_shouldIncludeNombreMadre_whenMadreIdExists() {
        // Given
        UUID madreId = UUID.randomUUID();
        Animal madre = Animal.create(
            "MADRE001",
            Sex.HEMBRA,
            Breed.ANGUS,
            LocalDate.now().minusYears(5),
            CattleType.VIENTRE,
            ranchoId,
            tenantId,
            createdBy
        );
        madre = new Animal.Builder().from(madre).animalId(madreId).build();
        
        // Animal hijo con madreId
        Animal animalConMadre = new Animal.Builder()
            .from(testAnimal)
            .madreId(madreId)
            .build();
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(animalConMadre));
        when(animalRepository.findById(madreId)).thenReturn(Optional.of(madre));
        when(weightRepository.findLatestWeight(animalId)).thenReturn(Optional.empty());
        when(pastureHistoryRepository.findCurrentByAnimalId(animalId)).thenReturn(Optional.empty());
        
        // When
        AnimalResult result = getAnimalUseCase.execute(animalId);
        
        // Then
        assertNotNull(result.nombreMadre());
        assertEquals("MADRE001", result.nombreMadre());
        verify(animalRepository).findById(madreId);
    }
    
    @Test
    void execute_shouldIncludeNombrePadre_whenPadreIdExists() {
        // Given
        UUID padreId = UUID.randomUUID();
        Animal padre = Animal.create(
            "PADRE001",
            Sex.MACHO,
            Breed.CHAROLAIS,
            LocalDate.now().minusYears(6),
            CattleType.SEMENTAL,
            ranchoId,
            tenantId,
            createdBy
        );
        padre = new Animal.Builder().from(padre).animalId(padreId).build();
        
        // Animal hijo con padreId
        Animal animalConPadre = new Animal.Builder()
            .from(testAnimal)
            .padreId(padreId)
            .build();
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(animalConPadre));
        when(animalRepository.findById(padreId)).thenReturn(Optional.of(padre));
        when(weightRepository.findLatestWeight(animalId)).thenReturn(Optional.empty());
        when(pastureHistoryRepository.findCurrentByAnimalId(animalId)).thenReturn(Optional.empty());
        
        // When
        AnimalResult result = getAnimalUseCase.execute(animalId);
        
        // Then
        assertNotNull(result.nombrePadre());
        assertEquals("PADRE001", result.nombrePadre());
        verify(animalRepository).findById(padreId);
    }
    
    @Test
    void execute_shouldReturnFullyEnrichedResult_whenAllDataExists() {
        // Given
        UUID madreId = UUID.randomUUID();
        UUID padreId = UUID.randomUUID();
        UUID potreroId = UUID.randomUUID();
        BigDecimal peso = new BigDecimal("380.50");
        
        Animal madre = Animal.create("MADRE001", Sex.HEMBRA, Breed.ANGUS, 
                                    LocalDate.now().minusYears(5), CattleType.VIENTRE,
                                    ranchoId, tenantId, createdBy);
        madre = new Animal.Builder().from(madre).animalId(madreId).build();
        
        Animal padre = Animal.create("PADRE001", Sex.MACHO, Breed.CHAROLAIS,
                                    LocalDate.now().minusYears(6), CattleType.SEMENTAL,
                                    ranchoId, tenantId, createdBy);
        padre = new Animal.Builder().from(padre).animalId(padreId).build();
        
        Animal animalCompleto = new Animal.Builder()
            .from(testAnimal)
            .madreId(madreId)
            .padreId(padreId)
            .build();
        
        WeightRecord weight = WeightRecord.create(animalId, peso, LocalDate.now(), null, createdBy);
        PastureHistory location = PastureHistory.create(animalId, potreroId, createdBy);
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(animalCompleto));
        when(animalRepository.findById(madreId)).thenReturn(Optional.of(madre));
        when(animalRepository.findById(padreId)).thenReturn(Optional.of(padre));
        when(weightRepository.findLatestWeight(animalId)).thenReturn(Optional.of(weight));
        when(pastureHistoryRepository.findCurrentByAnimalId(animalId))
            .thenReturn(Optional.of(location));
        
        // When
        AnimalResult result = getAnimalUseCase.execute(animalId);
        
        // Then
        assertNotNull(result);
        assertEquals(animalId, result.animalId());
        assertEquals("TEST001", result.arete());
        
        // Verificar campos enriquecidos
        assertNotNull(result.meses());
        assertEquals(peso, result.pesoActual());
        assertEquals(potreroId, result.potreroActual());
        assertEquals("MADRE001", result.nombreMadre());
        assertEquals("PADRE001", result.nombrePadre());
        
        // Verificar todas las llamadas a repositorios
        verify(animalRepository).findById(animalId);
        verify(animalRepository).findById(madreId);
        verify(animalRepository).findById(padreId);
        verify(weightRepository).findLatestWeight(animalId);
        verify(pastureHistoryRepository).findCurrentByAnimalId(animalId);
    }
    
    @Test
    void execute_shouldThrowAnimalNotFoundException_whenAnimalDoesNotExist() {
        // Given
        when(animalRepository.findById(animalId)).thenReturn(Optional.empty());
        
        // When & Then
        AnimalNotFoundException exception = assertThrows(
            AnimalNotFoundException.class,
            () -> getAnimalUseCase.execute(animalId)
        );
        
        assertEquals("Animal no encontrado", exception.getMessage());
        verify(animalRepository).findById(animalId);
        verifyNoInteractions(weightRepository);
        verifyNoInteractions(pastureHistoryRepository);
    }
    
    @Test
    void execute_shouldThrowIllegalArgumentException_whenAnimalIdIsNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> getAnimalUseCase.execute(null)
        );
        
        assertEquals("animalId no puede ser null", exception.getMessage());
        verifyNoInteractions(animalRepository);
        verifyNoInteractions(weightRepository);
        verifyNoInteractions(pastureHistoryRepository);
    }
    
    @Test
    void execute_shouldCalculateMesesInRealTime_notFromStoredValue() {
        // Given: Animal con fecha de nacimiento hace exactamente 24 meses
        LocalDate fechaNacimientoExacta = LocalDate.now().minusMonths(24);
        Animal animalViejo = Animal.create(
            "OLD001",
            Sex.MACHO,
            Breed.BRAHMAN,
            fechaNacimientoExacta,
            CattleType.ENGORDA,
            ranchoId,
            tenantId,
            createdBy
        );
        animalViejo = new Animal.Builder().from(animalViejo).animalId(animalId).build();
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(animalViejo));
        when(weightRepository.findLatestWeight(animalId)).thenReturn(Optional.empty());
        when(pastureHistoryRepository.findCurrentByAnimalId(animalId)).thenReturn(Optional.empty());
        
        // When
        AnimalResult result = getAnimalUseCase.execute(animalId);
        
        // Then: El cálculo debe ser en tiempo real, no el valor almacenado
        assertNotNull(result.meses());
        // La edad debe ser ~24 meses (permitir margen de 1 mes por días del mes)
        assertTrue(result.meses() >= 23 && result.meses() <= 25,
                   "Edad debe ser aproximadamente 24 meses, fue: " + result.meses());
    }
}
