package mx.vacapp.cattle.internal.application.usecases.animal;

import mx.vacapp.cattle.internal.application.usecases.commands.AnimalResult;
import mx.vacapp.cattle.internal.application.usecases.commands.ChangeStatusCommand;
import mx.vacapp.cattle.internal.domain.model.*;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.model.exceptions.CattleDomainException;
import mx.vacapp.cattle.internal.domain.model.exceptions.SoldOrDeadAnimalException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.CattleAuditRepository;
import mx.vacapp.cattle.internal.domain.repository.PastureHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChangeStatusUseCase.
 * Tests all status transitions, validations, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class ChangeStatusUseCaseTest {
    
    @Mock
    private AnimalRepository animalRepository;
    
    @Mock
    private PastureHistoryRepository pastureHistoryRepository;
    
    @Mock
    private CattleAuditRepository cattleAuditRepository;
    
    @InjectMocks
    private ChangeStatusUseCase changeStatusUseCase;
    
    private UUID animalId;
    private UUID tenantId;
    private UUID ranchoId;
    private UUID changedBy;
    private Animal activeAnimal;
    
    @BeforeEach
    void setUp() {
        animalId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        ranchoId = UUID.randomUUID();
        changedBy = UUID.randomUUID();
        
        // Create a basic active female animal for testing
        activeAnimal = Animal.create(
            "TEST001",
            Sex.HEMBRA,
            Breed.CHAROLAIS,
            LocalDate.of(2022, 1, 1),
            CattleType.VIENTRE,
            ranchoId,
            tenantId,
            changedBy
        );
        
        // Set the animal ID using builder
        activeAnimal = new Animal.Builder()
            .from(activeAnimal)
            .animalId(animalId)
            .build();
    }
    
    @Test
    void execute_ShouldChangeToPregnant_WhenAnimalIsFemale() {
        // Given
        ChangeStatusCommand command = new ChangeStatusCommand(
            animalId,
            CattleStatus.PRENADA,
            null,
            null,
            null,
            null,
            "Confirmación de preñez por veterinario",
            changedBy,
            tenantId
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(activeAnimal));
        when(animalRepository.save(any(Animal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        AnimalResult result = changeStatusUseCase.execute(command);
        
        // Then
        assertNotNull(result);
        assertEquals("prenada", result.status());
        verify(animalRepository).save(any(Animal.class));
        verify(cattleAuditRepository).logStatusChange(
            eq(animalId),
            eq("activa"),
            eq("prenada"),
            eq(changedBy),
            eq("Confirmación de preñez por veterinario")
        );
        verify(pastureHistoryRepository, never()).findCurrentByAnimalId(any());
    }
    
    @Test
    void execute_ShouldThrowException_WhenMaleMarkedAsPregnant() {
        // Given
        Animal maleAnimal = Animal.create(
            "MALE001",
            Sex.MACHO,
            Breed.ANGUS,
            LocalDate.of(2022, 1, 1),
            CattleType.SEMENTAL,
            ranchoId,
            tenantId,
            changedBy
        );
        maleAnimal = new Animal.Builder().from(maleAnimal).animalId(animalId).build();
        
        ChangeStatusCommand command = new ChangeStatusCommand(
            animalId,
            CattleStatus.PRENADA,
            null,
            null,
            null,
            null,
            null,
            changedBy,
            tenantId
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(maleAnimal));
        
        // When & Then
        CattleDomainException exception = assertThrows(
            CattleDomainException.class,
            () -> changeStatusUseCase.execute(command)
        );
        
        assertEquals("Solo hembras pueden estar preñadas", exception.getMessage());
        verify(animalRepository, never()).save(any(Animal.class));
        verify(cattleAuditRepository, never()).logStatusChange(any(), any(), any(), any(), any());
    }
    
    @Test
    void execute_ShouldChangeToSold_WhenValidSaleDataProvided() {
        // Given
        LocalDate saleDate = LocalDate.of(2024, 6, 15);
        BigDecimal salePrice = new BigDecimal("25000.00");
        
        ChangeStatusCommand command = new ChangeStatusCommand(
            animalId,
            CattleStatus.VENDIDA,
            saleDate,
            salePrice,
            null,
            null,
            "Venta a rancho vecino",
            changedBy,
            tenantId
        );
        
        PastureHistory currentHistory = PastureHistory.create(animalId, UUID.randomUUID(), changedBy);
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(activeAnimal));
        when(animalRepository.save(any(Animal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pastureHistoryRepository.findCurrentByAnimalId(animalId)).thenReturn(Optional.of(currentHistory));
        
        // When
        AnimalResult result = changeStatusUseCase.execute(command);
        
        // Then
        assertNotNull(result);
        assertEquals("vendida", result.status());
        assertEquals(saleDate, result.fechaVenta());
        assertEquals(salePrice, result.precioVenta());
        verify(animalRepository).save(any(Animal.class));
        verify(pastureHistoryRepository).updateFechaSalida(eq(currentHistory.getHistoryId()), any(Instant.class));
        verify(cattleAuditRepository).logStatusChange(
            eq(animalId),
            eq("activa"),
            eq("vendida"),
            eq(changedBy),
            eq("Venta a rancho vecino")
        );
    }
    
    @Test
    void execute_ShouldThrowException_WhenSaleWithoutDate() {
        // Given
        ChangeStatusCommand command = new ChangeStatusCommand(
            animalId,
            CattleStatus.VENDIDA,
            null, // Missing fechaVenta
            new BigDecimal("20000.00"),
            null,
            null,
            null,
            changedBy,
            tenantId
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(activeAnimal));
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> changeStatusUseCase.execute(command)
        );
        
        assertEquals("La fecha de venta es obligatoria", exception.getMessage());
        verify(animalRepository, never()).save(any(Animal.class));
    }
    
    @Test
    void execute_ShouldThrowException_WhenSalePriceIsZeroOrNegative() {
        // Given
        ChangeStatusCommand command = new ChangeStatusCommand(
            animalId,
            CattleStatus.VENDIDA,
            LocalDate.of(2024, 6, 15),
            BigDecimal.ZERO, // Invalid price
            null,
            null,
            null,
            changedBy,
            tenantId
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(activeAnimal));
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> changeStatusUseCase.execute(command)
        );
        
        assertEquals("El precio de venta debe ser mayor que cero", exception.getMessage());
        verify(animalRepository, never()).save(any(Animal.class));
    }
    
    @Test
    void execute_ShouldChangeToDead_WhenValidDeathDataProvided() {
        // Given
        LocalDate deathDate = LocalDate.of(2024, 7, 1);
        String deathReason = "Complicaciones respiratorias";
        
        ChangeStatusCommand command = new ChangeStatusCommand(
            animalId,
            CattleStatus.MUERTA,
            null,
            null,
            deathDate,
            deathReason,
            "Reporte veterinario",
            changedBy,
            tenantId
        );
        
        PastureHistory currentHistory = PastureHistory.create(animalId, UUID.randomUUID(), changedBy);
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(activeAnimal));
        when(animalRepository.save(any(Animal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pastureHistoryRepository.findCurrentByAnimalId(animalId)).thenReturn(Optional.of(currentHistory));
        
        // When
        AnimalResult result = changeStatusUseCase.execute(command);
        
        // Then
        assertNotNull(result);
        assertEquals("muerta", result.status());
        assertEquals(deathDate, result.fechaMuerte());
        assertEquals(deathReason, result.motivoMuerte());
        verify(animalRepository).save(any(Animal.class));
        verify(pastureHistoryRepository).updateFechaSalida(eq(currentHistory.getHistoryId()), any(Instant.class));
        verify(cattleAuditRepository).logStatusChange(
            eq(animalId),
            eq("activa"),
            eq("muerta"),
            eq(changedBy),
            eq("Reporte veterinario")
        );
    }
    
    @Test
    void execute_ShouldThrowException_WhenDeathWithoutDate() {
        // Given
        ChangeStatusCommand command = new ChangeStatusCommand(
            animalId,
            CattleStatus.MUERTA,
            null,
            null,
            null, // Missing fechaMuerte
            "Enfermedad",
            null,
            changedBy,
            tenantId
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(activeAnimal));
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> changeStatusUseCase.execute(command)
        );
        
        assertEquals("La fecha de muerte es obligatoria", exception.getMessage());
        verify(animalRepository, never()).save(any(Animal.class));
    }
    
    @Test
    void execute_ShouldChangeToSimpleStatus_WhenNoSpecialValidationsNeeded() {
        // Given
        ChangeStatusCommand command = new ChangeStatusCommand(
            animalId,
            CattleStatus.EN_REPOSO,
            null,
            null,
            null,
            null,
            "Período de descanso reproductivo",
            changedBy,
            tenantId
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(activeAnimal));
        when(animalRepository.save(any(Animal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        AnimalResult result = changeStatusUseCase.execute(command);
        
        // Then
        assertNotNull(result);
        assertEquals("en_reposo", result.status());
        verify(animalRepository).save(any(Animal.class));
        verify(pastureHistoryRepository, never()).findCurrentByAnimalId(any());
        verify(cattleAuditRepository).logStatusChange(
            eq(animalId),
            eq("activa"),
            eq("en_reposo"),
            eq(changedBy),
            eq("Período de descanso reproductivo")
        );
    }
    
    @Test
    void execute_ShouldThrowException_WhenAnimalNotFound() {
        // Given
        ChangeStatusCommand command = new ChangeStatusCommand(
            animalId,
            CattleStatus.ACTIVA,
            null,
            null,
            null,
            null,
            null,
            changedBy,
            tenantId
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.empty());
        
        // When & Then
        AnimalNotFoundException exception = assertThrows(
            AnimalNotFoundException.class,
            () -> changeStatusUseCase.execute(command)
        );
        
        assertEquals("Animal no encontrado", exception.getMessage());
        verify(animalRepository, never()).save(any(Animal.class));
    }
    
    @Test
    void execute_ShouldThrowException_WhenAnimalBelongsToOtherTenant() {
        // Given
        UUID otherTenantId = UUID.randomUUID();
        ChangeStatusCommand command = new ChangeStatusCommand(
            animalId,
            CattleStatus.ACTIVA,
            null,
            null,
            null,
            null,
            null,
            changedBy,
            otherTenantId
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(activeAnimal));
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> changeStatusUseCase.execute(command)
        );
        
        assertEquals("Acceso denegado", exception.getMessage());
        verify(animalRepository, never()).save(any(Animal.class));
    }
    
    @Test
    void execute_ShouldThrowException_WhenAnimalIsAlreadySold() {
        // Given
        Animal soldAnimal = activeAnimal.markAsSold(
            LocalDate.of(2024, 1, 1),
            new BigDecimal("20000.00"),
            changedBy
        );
        
        ChangeStatusCommand command = new ChangeStatusCommand(
            animalId,
            CattleStatus.ACTIVA,
            null,
            null,
            null,
            null,
            null,
            changedBy,
            tenantId
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(soldAnimal));
        
        // When & Then
        SoldOrDeadAnimalException exception = assertThrows(
            SoldOrDeadAnimalException.class,
            () -> changeStatusUseCase.execute(command)
        );
        
        assertEquals("No se puede modificar un animal vendido o muerto", exception.getMessage());
        verify(animalRepository, never()).save(any(Animal.class));
    }
    
    @Test
    void execute_ShouldThrowException_WhenAnimalIsAlreadyDead() {
        // Given
        Animal deadAnimal = activeAnimal.markAsDead(
            LocalDate.of(2024, 1, 1),
            "Causa natural",
            changedBy
        );
        
        ChangeStatusCommand command = new ChangeStatusCommand(
            animalId,
            CattleStatus.ACTIVA,
            null,
            null,
            null,
            null,
            null,
            changedBy,
            tenantId
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(deadAnimal));
        
        // When & Then
        SoldOrDeadAnimalException exception = assertThrows(
            SoldOrDeadAnimalException.class,
            () -> changeStatusUseCase.execute(command)
        );
        
        assertEquals("No se puede modificar un animal vendido o muerto", exception.getMessage());
        verify(animalRepository, never()).save(any(Animal.class));
    }
    
    @Test
    void execute_ShouldNotClosePastureHistory_WhenChangingToNonFinalStatus() {
        // Given
        ChangeStatusCommand command = new ChangeStatusCommand(
            animalId,
            CattleStatus.PRESTADA,
            null,
            null,
            null,
            null,
            "Préstamo temporal",
            changedBy,
            tenantId
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(activeAnimal));
        when(animalRepository.save(any(Animal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        AnimalResult result = changeStatusUseCase.execute(command);
        
        // Then
        assertNotNull(result);
        assertEquals("prestada", result.status());
        verify(pastureHistoryRepository, never()).findCurrentByAnimalId(any());
        verify(pastureHistoryRepository, never()).updateFechaSalida(any(), any());
    }
    
    @Test
    void execute_ShouldHandleNoPastureHistory_WhenChangingToFinalStatus() {
        // Given
        ChangeStatusCommand command = new ChangeStatusCommand(
            animalId,
            CattleStatus.VENDIDA,
            LocalDate.of(2024, 6, 15),
            new BigDecimal("30000.00"),
            null,
            null,
            null,
            changedBy,
            tenantId
        );
        
        when(animalRepository.findById(animalId)).thenReturn(Optional.of(activeAnimal));
        when(animalRepository.save(any(Animal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pastureHistoryRepository.findCurrentByAnimalId(animalId)).thenReturn(Optional.empty());
        
        // When
        AnimalResult result = changeStatusUseCase.execute(command);
        
        // Then
        assertNotNull(result);
        assertEquals("vendida", result.status());
        verify(pastureHistoryRepository).findCurrentByAnimalId(animalId);
        verify(pastureHistoryRepository, never()).updateFechaSalida(any(), any());
    }
}
