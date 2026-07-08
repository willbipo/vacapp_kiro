package mx.vacapp.cattle.internal.infrastructure.persistence.impl;

import mx.vacapp.cattle.internal.domain.model.PastureHistory;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.PastureHistoryEntity;
import mx.vacapp.cattle.internal.infrastructure.persistence.repositories.PastureHistoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitario para PastureHistoryRepositoryImpl.
 * 
 * Verifica la correcta implementación del puerto PastureHistoryRepository
 * usando mocks de PastureHistoryJpaRepository.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PastureHistoryRepositoryImpl - Unit Tests")
class PastureHistoryRepositoryImplTest {
    
    @Mock
    private PastureHistoryJpaRepository jpaRepository;
    
    @InjectMocks
    private PastureHistoryRepositoryImpl repository;
    
    private UUID animalId;
    private UUID potreroId;
    private UUID createdBy;
    private PastureHistory pastureHistory;
    private PastureHistoryEntity pastureHistoryEntity;
    
    @BeforeEach
    void setUp() {
        animalId = UUID.randomUUID();
        potreroId = UUID.randomUUID();
        createdBy = UUID.randomUUID();
        
        // Crear objeto de dominio de prueba
        pastureHistory = PastureHistory.create(animalId, potreroId, createdBy);
        
        // Crear entidad JPA de prueba
        pastureHistoryEntity = PastureHistoryEntity.builder()
                .historyId(pastureHistory.getHistoryId())
                .animalId(animalId)
                .potreroId(potreroId)
                .fechaEntrada(pastureHistory.getFechaEntrada())
                .fechaSalida(null)
                .diasPermanencia(null)
                .createdAt(pastureHistory.getCreatedAt())
                .createdBy(createdBy)
                .build();
    }
    
    @Test
    @DisplayName("insert() debe guardar un nuevo registro de historial")
    void insert_shouldSaveNewHistory() {
        // Given
        when(jpaRepository.save(any(PastureHistoryEntity.class)))
                .thenReturn(pastureHistoryEntity);
        
        // When
        PastureHistory result = repository.insert(pastureHistory);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAnimalId()).isEqualTo(animalId);
        assertThat(result.getPotreroId()).isEqualTo(potreroId);
        assertThat(result.getFechaSalida()).isNull();
        verify(jpaRepository, times(1)).save(any(PastureHistoryEntity.class));
    }
    
    @Test
    @DisplayName("insert() debe lanzar IllegalArgumentException si history es null")
    void insert_shouldThrowExceptionWhenHistoryIsNull() {
        // When & Then
        assertThatThrownBy(() -> repository.insert(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El registro de historial no puede ser null");
        
        verify(jpaRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("findCurrentByAnimalId() debe retornar la ubicación actual del animal")
    void findCurrentByAnimalId_shouldReturnCurrentLocation() {
        // Given
        when(jpaRepository.findByAnimalIdAndFechaSalidaIsNull(animalId))
                .thenReturn(Optional.of(pastureHistoryEntity));
        
        // When
        Optional<PastureHistory> result = repository.findCurrentByAnimalId(animalId);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getAnimalId()).isEqualTo(animalId);
        assertThat(result.get().getFechaSalida()).isNull();
        verify(jpaRepository, times(1)).findByAnimalIdAndFechaSalidaIsNull(animalId);
    }
    
    @Test
    @DisplayName("findCurrentByAnimalId() debe retornar empty si el animal no tiene ubicación actual")
    void findCurrentByAnimalId_shouldReturnEmptyWhenNoCurrentLocation() {
        // Given
        when(jpaRepository.findByAnimalIdAndFechaSalidaIsNull(animalId))
                .thenReturn(Optional.empty());
        
        // When
        Optional<PastureHistory> result = repository.findCurrentByAnimalId(animalId);
        
        // Then
        assertThat(result).isEmpty();
        verify(jpaRepository, times(1)).findByAnimalIdAndFechaSalidaIsNull(animalId);
    }
    
    @Test
    @DisplayName("updateFechaSalida() debe actualizar la fecha de salida del registro actual")
    void updateFechaSalida_shouldUpdateExitDate() {
        // Given
        Instant fechaSalida = Instant.now();
        when(jpaRepository.updateFechaSalidaByAnimalId(animalId, fechaSalida))
                .thenReturn(1);
        
        // When
        repository.updateFechaSalida(animalId, fechaSalida);
        
        // Then
        verify(jpaRepository, times(1)).updateFechaSalidaByAnimalId(animalId, fechaSalida);
    }
    
    @Test
    @DisplayName("updateFechaSalida() debe lanzar IllegalArgumentException si animalId es null")
    void updateFechaSalida_shouldThrowExceptionWhenAnimalIdIsNull() {
        // Given
        Instant fechaSalida = Instant.now();
        
        // When & Then
        assertThatThrownBy(() -> repository.updateFechaSalida(null, fechaSalida))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El animalId no puede ser null");
        
        verify(jpaRepository, never()).updateFechaSalidaByAnimalId(any(), any());
    }
    
    @Test
    @DisplayName("updateFechaSalida() debe lanzar IllegalArgumentException si fechaSalida es null")
    void updateFechaSalida_shouldThrowExceptionWhenFechaSalidaIsNull() {
        // When & Then
        assertThatThrownBy(() -> repository.updateFechaSalida(animalId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha de salida no puede ser null");
        
        verify(jpaRepository, never()).updateFechaSalidaByAnimalId(any(), any());
    }
    
    @Test
    @DisplayName("findHistoryByAnimalId() debe retornar el historial completo ordenado")
    void findHistoryByAnimalId_shouldReturnCompleteHistoryOrdered() {
        // Given
        PastureHistoryEntity entity1 = PastureHistoryEntity.builder()
                .historyId(UUID.randomUUID())
                .animalId(animalId)
                .potreroId(UUID.randomUUID())
                .fechaEntrada(Instant.now().minusSeconds(86400 * 2))
                .fechaSalida(Instant.now().minusSeconds(86400))
                .diasPermanencia(1)
                .createdAt(Instant.now())
                .createdBy(createdBy)
                .build();
        
        PastureHistoryEntity entity2 = PastureHistoryEntity.builder()
                .historyId(UUID.randomUUID())
                .animalId(animalId)
                .potreroId(UUID.randomUUID())
                .fechaEntrada(Instant.now())
                .fechaSalida(null)
                .diasPermanencia(null)
                .createdAt(Instant.now())
                .createdBy(createdBy)
                .build();
        
        when(jpaRepository.findByAnimalIdOrderByFechaEntradaDesc(animalId))
                .thenReturn(Arrays.asList(entity2, entity1)); // Más reciente primero
        
        // When
        List<PastureHistory> result = repository.findHistoryByAnimalId(animalId);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFechaSalida()).isNull(); // Registro actual primero
        assertThat(result.get(1).getFechaSalida()).isNotNull(); // Registro histórico
        verify(jpaRepository, times(1)).findByAnimalIdOrderByFechaEntradaDesc(animalId);
    }
    
    @Test
    @DisplayName("findAnimalIdsByPotreroId() debe retornar IDs de animales en el potrero")
    void findAnimalIdsByPotreroId_shouldReturnAnimalIds() {
        // Given
        UUID animal1 = UUID.randomUUID();
        UUID animal2 = UUID.randomUUID();
        
        PastureHistoryEntity entity1 = PastureHistoryEntity.builder()
                .historyId(UUID.randomUUID())
                .animalId(animal1)
                .potreroId(potreroId)
                .fechaEntrada(Instant.now())
                .fechaSalida(null)
                .createdAt(Instant.now())
                .createdBy(createdBy)
                .build();
        
        PastureHistoryEntity entity2 = PastureHistoryEntity.builder()
                .historyId(UUID.randomUUID())
                .animalId(animal2)
                .potreroId(potreroId)
                .fechaEntrada(Instant.now())
                .fechaSalida(null)
                .createdAt(Instant.now())
                .createdBy(createdBy)
                .build();
        
        when(jpaRepository.findByPotreroIdAndFechaSalidaIsNull(potreroId))
                .thenReturn(Arrays.asList(entity1, entity2));
        
        // When
        List<UUID> result = repository.findAnimalIdsByPotreroId(potreroId);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(animal1, animal2);
        verify(jpaRepository, times(1)).findByPotreroIdAndFechaSalidaIsNull(potreroId);
    }
    
    @Test
    @DisplayName("countAnimalsInPasture() debe retornar la cantidad de animales en el potrero")
    void countAnimalsInPasture_shouldReturnAnimalCount() {
        // Given
        when(jpaRepository.countByPotreroIdAndFechaSalidaIsNull(potreroId))
                .thenReturn(5L);
        
        // When
        int result = repository.countAnimalsInPasture(potreroId);
        
        // Then
        assertThat(result).isEqualTo(5);
        verify(jpaRepository, times(1)).countByPotreroIdAndFechaSalidaIsNull(potreroId);
    }
}
