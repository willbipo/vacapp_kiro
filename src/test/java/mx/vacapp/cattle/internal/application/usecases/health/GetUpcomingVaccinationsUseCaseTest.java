package mx.vacapp.cattle.internal.application.usecases.health;

import mx.vacapp.cattle.internal.application.usecases.commands.UpcomingVaccinationResult;
import mx.vacapp.cattle.internal.domain.model.*;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.HealthEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GetUpcomingVaccinationsUseCase.
 * Tests calculation of upcoming vaccinations based on proximaFecha.
 */
@ExtendWith(MockitoExtension.class)
class GetUpcomingVaccinationsUseCaseTest {
    
    @Mock
    private AnimalRepository animalRepository;
    
    @Mock
    private HealthEventRepository healthEventRepository;
    
    @InjectMocks
    private GetUpcomingVaccinationsUseCase getUpcomingVaccinationsUseCase;
    
    private UUID animalId;
    private UUID tenantId;
    private UUID ranchoId;
    private UUID recordedBy;
    private Animal animal;
    
    @BeforeEach
    void setUp() {
        animalId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        ranchoId = UUID.randomUUID();
        recordedBy = UUID.randomUUID();
        
        // Create test animal
        animal = Animal.create(
            "TEST001",
            Sex.HEMBRA,
            Breed.CHAROLAIS,
            LocalDate.of(2020, 1, 1),
            CattleType.VIENTRE,
            ranchoId,
            tenantId,
            recordedBy
        );
        
        animal = new Animal.Builder()
            .from(animal)
            .animalId(animalId)
            .build();
    }
    
    @Test
    void execute_ShouldReturnUpcomingVaccinations_WhenProximaFechaIsSet() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate lastVaccinationDate = today.minusDays(30);
        LocalDate proximaFecha = today.plusDays(15);
        
        HealthEvent vaccinationEvent = new HealthEvent.Builder()
            .eventId(UUID.randomUUID())
            .animalId(animalId)
            .tipoEvento(HealthEventType.Vacunacion)
            .fecha(lastVaccinationDate)
            .descripcion("Triple viral")
            .proximaFecha(proximaFecha)
            .recordedAt(LocalDateTime.now())
            .recordedBy(recordedBy)
            .build();
        
        List<HealthEvent> healthEvents = List.of(vaccinationEvent);
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(animal));
        when(healthEventRepository.findByAnimalId(animalId)).thenReturn(healthEvents);
        
        // When
        List<UpcomingVaccinationResult> results = getUpcomingVaccinationsUseCase.execute(animalId);
        
        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        
        UpcomingVaccinationResult result = results.get(0);
        assertEquals(vaccinationEvent.getEventId(), result.healthEventId());
        assertEquals(animalId, result.animalId());
        assertEquals("Triple viral", result.nombreVacuna());
        assertEquals(lastVaccinationDate, result.ultimaFecha());
        assertEquals(proximaFecha, result.proximaFecha());
        assertEquals(15, result.diasRestantes());
        
        verify(animalRepository).findById(animalId);
        verify(healthEventRepository).findByAnimalId(animalId);
    }
    
    @Test
    void execute_ShouldFilterPastVaccinations_WhenProximaFechaIsPast() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate pastDate = today.minusDays(10);
        
        HealthEvent pastVaccinationEvent = new HealthEvent.Builder()
            .eventId(UUID.randomUUID())
            .animalId(animalId)
            .tipoEvento(HealthEventType.Vacunacion)
            .fecha(today.minusDays(40))
            .descripcion("Vacuna vencida")
            .proximaFecha(pastDate)  // Fecha en el pasado
            .recordedAt(LocalDateTime.now())
            .recordedBy(recordedBy)
            .build();
        
        List<HealthEvent> healthEvents = List.of(pastVaccinationEvent);
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(animal));
        when(healthEventRepository.findByAnimalId(animalId)).thenReturn(healthEvents);
        
        // When
        List<UpcomingVaccinationResult> results = getUpcomingVaccinationsUseCase.execute(animalId);
        
        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty(), "Past vaccinations should be filtered out");
        
        verify(animalRepository).findById(animalId);
        verify(healthEventRepository).findByAnimalId(animalId);
    }
    
    @Test
    void execute_ShouldIgnoreVaccinationsWithoutProximaFecha() {
        // Given
        HealthEvent vaccinationWithoutProximaFecha = new HealthEvent.Builder()
            .eventId(UUID.randomUUID())
            .animalId(animalId)
            .tipoEvento(HealthEventType.Vacunacion)
            .fecha(LocalDate.now().minusDays(20))
            .descripcion("Vacuna sin fecha programada")
            .proximaFecha(null)  // No tiene próxima fecha
            .recordedAt(LocalDateTime.now())
            .recordedBy(recordedBy)
            .build();
        
        List<HealthEvent> healthEvents = List.of(vaccinationWithoutProximaFecha);
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(animal));
        when(healthEventRepository.findByAnimalId(animalId)).thenReturn(healthEvents);
        
        // When
        List<UpcomingVaccinationResult> results = getUpcomingVaccinationsUseCase.execute(animalId);
        
        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty(), "Vaccinations without proximaFecha should be ignored");
        
        verify(animalRepository).findById(animalId);
        verify(healthEventRepository).findByAnimalId(animalId);
    }
    
    @Test
    void execute_ShouldIgnoreNonVaccinationEvents() {
        // Given
        LocalDate today = LocalDate.now();
        
        HealthEvent treatmentEvent = new HealthEvent.Builder()
            .eventId(UUID.randomUUID())
            .animalId(animalId)
            .tipoEvento(HealthEventType.Tratamiento)  // Not a vaccination
            .fecha(today.minusDays(5))
            .descripcion("Tratamiento antibiótico")
            .proximaFecha(today.plusDays(10))
            .recordedAt(LocalDateTime.now())
            .recordedBy(recordedBy)
            .build();
        
        List<HealthEvent> healthEvents = List.of(treatmentEvent);
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(animal));
        when(healthEventRepository.findByAnimalId(animalId)).thenReturn(healthEvents);
        
        // When
        List<UpcomingVaccinationResult> results = getUpcomingVaccinationsUseCase.execute(animalId);
        
        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty(), "Non-vaccination events should be ignored");
        
        verify(animalRepository).findById(animalId);
        verify(healthEventRepository).findByAnimalId(animalId);
    }
    
    @Test
    void execute_ShouldSortByProximaFechaAscending() {
        // Given
        LocalDate today = LocalDate.now();
        
        HealthEvent vaccination1 = new HealthEvent.Builder()
            .eventId(UUID.randomUUID())
            .animalId(animalId)
            .tipoEvento(HealthEventType.Vacunacion)
            .fecha(today.minusDays(60))
            .descripcion("Vacuna B")
            .proximaFecha(today.plusDays(30))  // Más lejana
            .recordedAt(LocalDateTime.now())
            .recordedBy(recordedBy)
            .build();
        
        HealthEvent vaccination2 = new HealthEvent.Builder()
            .eventId(UUID.randomUUID())
            .animalId(animalId)
            .tipoEvento(HealthEventType.Vacunacion)
            .fecha(today.minusDays(30))
            .descripcion("Vacuna A")
            .proximaFecha(today.plusDays(5))  // Más cercana
            .recordedAt(LocalDateTime.now())
            .recordedBy(recordedBy)
            .build();
        
        List<HealthEvent> healthEvents = List.of(vaccination1, vaccination2);
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(animal));
        when(healthEventRepository.findByAnimalId(animalId)).thenReturn(healthEvents);
        
        // When
        List<UpcomingVaccinationResult> results = getUpcomingVaccinationsUseCase.execute(animalId);
        
        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        
        // Should be sorted by proximaFecha ascending (earliest first)
        assertEquals("Vacuna A", results.get(0).nombreVacuna());
        assertEquals(today.plusDays(5), results.get(0).proximaFecha());
        assertEquals(5, results.get(0).diasRestantes());
        
        assertEquals("Vacuna B", results.get(1).nombreVacuna());
        assertEquals(today.plusDays(30), results.get(1).proximaFecha());
        assertEquals(30, results.get(1).diasRestantes());
        
        verify(animalRepository).findById(animalId);
        verify(healthEventRepository).findByAnimalId(animalId);
    }
    
    @Test
    void execute_ShouldReturnEmptyList_WhenAnimalHasNoHealthEvents() {
        // Given
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(animal));
        when(healthEventRepository.findByAnimalId(animalId)).thenReturn(new ArrayList<>());
        
        // When
        List<UpcomingVaccinationResult> results = getUpcomingVaccinationsUseCase.execute(animalId);
        
        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        
        verify(animalRepository).findById(animalId);
        verify(healthEventRepository).findByAnimalId(animalId);
    }
    
    @Test
    void execute_ShouldThrowAnimalNotFoundException_WhenAnimalDoesNotExist() {
        // Given
        when(animalRepository.findById(animalId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(AnimalNotFoundException.class, () -> {
            getUpcomingVaccinationsUseCase.execute(animalId);
        });
        
        verify(animalRepository).findById(animalId);
        verify(healthEventRepository, never()).findByAnimalId(any());
    }
    
    @Test
    void execute_ShouldExtractVaccineNameFromDescription() {
        // Given
        LocalDate today = LocalDate.now();
        
        HealthEvent vaccinationWithLongDescription = new HealthEvent.Builder()
            .eventId(UUID.randomUUID())
            .animalId(animalId)
            .tipoEvento(HealthEventType.Vacunacion)
            .fecha(today.minusDays(20))
            .descripcion("Rabia\nDosis de refuerzo aplicada por veterinario")
            .proximaFecha(today.plusDays(10))
            .recordedAt(LocalDateTime.now())
            .recordedBy(recordedBy)
            .build();
        
        List<HealthEvent> healthEvents = List.of(vaccinationWithLongDescription);
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(animal));
        when(healthEventRepository.findByAnimalId(animalId)).thenReturn(healthEvents);
        
        // When
        List<UpcomingVaccinationResult> results = getUpcomingVaccinationsUseCase.execute(animalId);
        
        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Rabia", results.get(0).nombreVacuna(), 
                     "Should extract only first line as vaccine name");
        
        verify(animalRepository).findById(animalId);
        verify(healthEventRepository).findByAnimalId(animalId);
    }
    
    @Test
    void execute_ShouldIncludeVaccinationForToday() {
        // Given
        LocalDate today = LocalDate.now();
        
        HealthEvent vaccinationForToday = new HealthEvent.Builder()
            .eventId(UUID.randomUUID())
            .animalId(animalId)
            .tipoEvento(HealthEventType.Vacunacion)
            .fecha(today.minusDays(30))
            .descripcion("Vacuna programada hoy")
            .proximaFecha(today)  // Fecha exactamente hoy
            .recordedAt(LocalDateTime.now())
            .recordedBy(recordedBy)
            .build();
        
        List<HealthEvent> healthEvents = List.of(vaccinationForToday);
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(animal));
        when(healthEventRepository.findByAnimalId(animalId)).thenReturn(healthEvents);
        
        // When
        List<UpcomingVaccinationResult> results = getUpcomingVaccinationsUseCase.execute(animalId);
        
        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(0, results.get(0).diasRestantes(), 
                     "Vaccination scheduled for today should have 0 days remaining");
        assertTrue(results.get(0).isHoy());
        
        verify(animalRepository).findById(animalId);
        verify(healthEventRepository).findByAnimalId(animalId);
    }
}
