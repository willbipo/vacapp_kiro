package mx.vacapp.cattle.internal.application.usecases.health;

import mx.vacapp.cattle.internal.application.usecases.animal.ChangeStatusUseCase;
import mx.vacapp.cattle.internal.application.usecases.commands.BirthEventResult;
import mx.vacapp.cattle.internal.application.usecases.commands.RecordBirthCommand;
import mx.vacapp.cattle.internal.domain.model.*;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.model.exceptions.DuplicateAreteException;
import mx.vacapp.cattle.internal.domain.model.exceptions.InvalidGenealogyException;
import mx.vacapp.cattle.internal.domain.model.exceptions.SoldOrDeadAnimalException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.HealthEventRepository;
import mx.vacapp.cattle.internal.domain.repository.PastureHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RecordBirthEventUseCase.
 * Tests birth event registration with automatic offspring creation.
 */
@ExtendWith(MockitoExtension.class)
class RecordBirthEventUseCaseTest {
    
    @Mock
    private AnimalRepository animalRepository;
    
    @Mock
    private HealthEventRepository healthEventRepository;
    
    @Mock
    private PastureHistoryRepository pastureHistoryRepository;
    
    @Mock
    private ChangeStatusUseCase changeStatusUseCase;
    
    @InjectMocks
    private RecordBirthEventUseCase recordBirthEventUseCase;
    
    private UUID madreId;
    private UUID tenantId;
    private UUID ranchoId;
    private UUID recordedBy;
    private UUID potreroId;
    private Animal madre;
    
    @BeforeEach
    void setUp() {
        madreId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        ranchoId = UUID.randomUUID();
        recordedBy = UUID.randomUUID();
        potreroId = UUID.randomUUID();
        
        // Create a female animal (mother)
        madre = Animal.create(
            "MADRE001",
            Sex.HEMBRA,
            Breed.CHAROLAIS,
            LocalDate.of(2020, 1, 1),
            CattleType.VIENTRE,
            ranchoId,
            tenantId,
            recordedBy
        );
        
        madre = new Animal.Builder()
            .from(madre)
            .animalId(madreId)
            .status(CattleStatus.PRENADA)
            .build();
    }
    
    @Test
    void execute_ShouldCreateOffspringAndBirthEvent_WhenValidMotherAndData() {
        // Given
        RecordBirthCommand command = new RecordBirthCommand(
            madreId,
            LocalDate.now(),
            Sex.HEMBRA,
            "CRIA001",
            null,  // No padre
            new BigDecimal("35.5"),  // Peso al nacer
            "Nacimiento sin complicaciones",
            recordedBy
        );
        
        PastureHistory currentPasture = new PastureHistory.Builder()
            .historyId(UUID.randomUUID())
            .animalId(madreId)
            .potreroId(potreroId)
            .fechaEntrada(java.time.Instant.now())
            .createdBy(recordedBy)
            .build();
        
        when(animalRepository.findById(madreId)).thenReturn(Optional.of(madre));
        when(animalRepository.existsByArete("CRIA001")).thenReturn(false);
        when(pastureHistoryRepository.findCurrentByAnimalId(madreId))
            .thenReturn(Optional.of(currentPasture));
        when(animalRepository.save(any(Animal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(healthEventRepository.save(any(HealthEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        BirthEventResult result = recordBirthEventUseCase.execute(command);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.healthEvent());
        assertNotNull(result.offspring());
        
        // Verify offspring data
        assertEquals("CRIA001", result.offspring().arete());
        assertEquals("hembra", result.offspring().sexo());
        assertEquals("charolais", result.offspring().raza());  // Inherited from mother
        assertEquals("cria", result.offspring().tipo());
        assertEquals("activa", result.offspring().status());
        assertEquals(madreId, result.offspring().madreId());
        assertEquals(ranchoId, result.offspring().ranchoId());
        assertEquals(tenantId, result.offspring().tenantId());
        
        // Verify health event
        assertEquals(HealthEventType.Birth, result.healthEvent().tipoEvento());
        assertEquals(madreId, result.healthEvent().animalId());
        
        // Verify repository calls
        ArgumentCaptor<Animal> animalCaptor = ArgumentCaptor.forClass(Animal.class);
        verify(animalRepository).save(animalCaptor.capture());
        Animal savedOffspring = animalCaptor.getValue();
        assertEquals("CRIA001", savedOffspring.getArete());
        
        verify(pastureHistoryRepository).insert(any(PastureHistory.class));
        verify(healthEventRepository).save(any(HealthEvent.class));
        verify(changeStatusUseCase).execute(any());  // Mother status change
    }
    
    @Test
    void execute_ShouldThrowException_WhenMotherNotFound() {
        // Given
        RecordBirthCommand command = new RecordBirthCommand(
            madreId,
            LocalDate.now(),
            Sex.MACHO,
            "CRIA002",
            null,
            null,
            null,
            recordedBy
        );
        
        when(animalRepository.findById(madreId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(AnimalNotFoundException.class, () -> recordBirthEventUseCase.execute(command));
        verify(animalRepository, never()).save(any());
    }
    
    @Test
    void execute_ShouldThrowException_WhenMotherIsMale() {
        // Given
        Animal maleAnimal = Animal.create(
            "MALE001",
            Sex.MACHO,
            Breed.ANGUS,
            LocalDate.of(2020, 1, 1),
            CattleType.SEMENTAL,
            ranchoId,
            tenantId,
            recordedBy
        );
        maleAnimal = new Animal.Builder().from(maleAnimal).animalId(madreId).build();
        
        RecordBirthCommand command = new RecordBirthCommand(
            madreId,
            LocalDate.now(),
            Sex.HEMBRA,
            "CRIA003",
            null,
            null,
            null,
            recordedBy
        );
        
        when(animalRepository.findById(madreId)).thenReturn(Optional.of(maleAnimal));
        
        // When & Then
        InvalidGenealogyException exception = assertThrows(
            InvalidGenealogyException.class,
            () -> recordBirthEventUseCase.execute(command)
        );
        assertEquals("Solo hembras pueden parir", exception.getMessage());
        verify(animalRepository, never()).save(any());
    }
    
    @Test
    void execute_ShouldThrowException_WhenMotherIsSold() {
        // Given
        Animal soldMother = new Animal.Builder()
            .from(madre)
            .status(CattleStatus.VENDIDA)
            .build();
        
        RecordBirthCommand command = new RecordBirthCommand(
            madreId,
            LocalDate.now(),
            Sex.MACHO,
            "CRIA004",
            null,
            null,
            null,
            recordedBy
        );
        
        when(animalRepository.findById(madreId)).thenReturn(Optional.of(soldMother));
        
        // When & Then
        assertThrows(SoldOrDeadAnimalException.class, () -> recordBirthEventUseCase.execute(command));
        verify(animalRepository, never()).save(any());
    }
    
    @Test
    void execute_ShouldThrowException_WhenBirthDateIsFuture() {
        // Given
        RecordBirthCommand command = new RecordBirthCommand(
            madreId,
            LocalDate.now().plusDays(1),  // Future date
            Sex.HEMBRA,
            "CRIA005",
            null,
            null,
            null,
            recordedBy
        );
        
        when(animalRepository.findById(madreId)).thenReturn(Optional.of(madre));
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> recordBirthEventUseCase.execute(command)
        );
        assertEquals("La fecha de nacimiento no puede ser futura", exception.getMessage());
        verify(animalRepository, never()).save(any());
    }
    
    @Test
    void execute_ShouldThrowException_WhenAreteAlreadyExists() {
        // Given
        RecordBirthCommand command = new RecordBirthCommand(
            madreId,
            LocalDate.now(),
            Sex.MACHO,
            "DUPLICATE001",
            null,
            null,
            null,
            recordedBy
        );
        
        when(animalRepository.findById(madreId)).thenReturn(Optional.of(madre));
        when(animalRepository.existsByArete("DUPLICATE001")).thenReturn(true);
        
        // When & Then
        assertThrows(DuplicateAreteException.class, () -> recordBirthEventUseCase.execute(command));
        verify(animalRepository, never()).save(any());
    }
    
    @Test
    void execute_ShouldNotCreatePastureHistory_WhenMotherHasNoPasture() {
        // Given
        RecordBirthCommand command = new RecordBirthCommand(
            madreId,
            LocalDate.now(),
            Sex.MACHO,
            "CRIA006",
            null,
            null,
            null,
            recordedBy
        );
        
        when(animalRepository.findById(madreId)).thenReturn(Optional.of(madre));
        when(animalRepository.existsByArete("CRIA006")).thenReturn(false);
        when(pastureHistoryRepository.findCurrentByAnimalId(madreId))
            .thenReturn(Optional.empty());  // No pasture
        when(animalRepository.save(any(Animal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(healthEventRepository.save(any(HealthEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        BirthEventResult result = recordBirthEventUseCase.execute(command);
        
        // Then
        assertNotNull(result);
        verify(pastureHistoryRepository, never()).insert(any());  // Should not create pasture history
        verify(animalRepository).save(any(Animal.class));  // But should still save offspring
        verify(healthEventRepository).save(any(HealthEvent.class));
    }
    
    @Test
    void execute_ShouldNotChangeMotherStatus_WhenMotherIsNotPregnant() {
        // Given
        Animal activeMother = new Animal.Builder()
            .from(madre)
            .status(CattleStatus.ACTIVA)  // Not pregnant
            .build();
        
        RecordBirthCommand command = new RecordBirthCommand(
            madreId,
            LocalDate.now(),
            Sex.HEMBRA,
            "CRIA007",
            null,
            null,
            null,
            recordedBy
        );
        
        when(animalRepository.findById(madreId)).thenReturn(Optional.of(activeMother));
        when(animalRepository.existsByArete("CRIA007")).thenReturn(false);
        when(pastureHistoryRepository.findCurrentByAnimalId(madreId))
            .thenReturn(Optional.empty());
        when(animalRepository.save(any(Animal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(healthEventRepository.save(any(HealthEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        BirthEventResult result = recordBirthEventUseCase.execute(command);
        
        // Then
        assertNotNull(result);
        verify(changeStatusUseCase, never()).execute(any());  // Should not change status
    }
}
