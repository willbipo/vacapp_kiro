package mx.vacapp.cattle.internal.infrastructure.persistence.impl;

import mx.vacapp.cattle.internal.domain.model.HealthEvent;
import mx.vacapp.cattle.internal.domain.model.HealthEventType;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.HealthEventEntity;
import mx.vacapp.cattle.internal.infrastructure.persistence.repositories.HealthEventJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitario para HealthEventRepositoryImpl.
 * 
 * Verifica:
 * - Guardado de eventos de salud
 * - Búsqueda por animal_id
 * - Búsqueda por tipo de evento
 * - Búsqueda de vacunaciones próximas
 * - Búsqueda de eventos de parto por madre
 * - Conversión correcta entre enums de dominio y entidad
 * - Validaciones de argumentos null
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthEventRepositoryImpl Tests")
class HealthEventRepositoryImplTest {
    
    @Mock
    private HealthEventJpaRepository jpaRepository;
    
    @InjectMocks
    private HealthEventRepositoryImpl repository;
    
    private UUID animalId;
    private UUID tenantId;
    private UUID eventId;
    private UUID createdBy;
    
    @BeforeEach
    void setUp() {
        animalId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        createdBy = UUID.randomUUID();
    }
    
    @Test
    @DisplayName("save() debe guardar un evento de salud y retornar el evento guardado")
    void save_shouldPersistHealthEventAndReturnSaved() {
        // Given
        HealthEvent healthEvent = new HealthEvent.Builder()
                .eventId(eventId)
                .animalId(animalId)
                .tipoEvento(HealthEventType.Vacunacion)
                .fecha(LocalDate.now().minusDays(1))
                .descripcion("{\"vacuna_nombre\": \"Rabia\", \"intervalo_dias\": 365}")
                .costo(BigDecimal.valueOf(150.00))
                .recordedAt(LocalDateTime.now())
                .recordedBy(createdBy)
                .build();
        
        HealthEventEntity entityToSave = HealthEventEntity.builder()
                .eventId(eventId)
                .animalId(animalId)
                .eventType(HealthEventEntity.EventType.VACCINATION)
                .fechaEvento(LocalDate.now().minusDays(1))
                .descripcion("{\"vacuna_nombre\": \"Rabia\", \"intervalo_dias\": 365}")
                .costo(BigDecimal.valueOf(150.00))
                .createdAt(Instant.now())
                .createdBy(createdBy)
                .build();
        
        when(jpaRepository.save(any(HealthEventEntity.class))).thenReturn(entityToSave);
        
        // When
        HealthEvent saved = repository.save(healthEvent);
        
        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getEventId()).isEqualTo(eventId);
        assertThat(saved.getAnimalId()).isEqualTo(animalId);
        assertThat(saved.getTipoEvento()).isEqualTo(HealthEventType.Vacunacion);
        
        verify(jpaRepository, times(1)).save(any(HealthEventEntity.class));
    }
    
    @Test
    @DisplayName("save() debe lanzar excepción si healthEvent es null")
    void save_shouldThrowExceptionWhenHealthEventIsNull() {
        // When & Then
        assertThatThrownBy(() -> repository.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("HealthEvent no puede ser null");
        
        verify(jpaRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("findByAnimalId() debe retornar eventos ordenados por fecha descendente")
    void findByAnimalId_shouldReturnEventsOrderedByDateDesc() {
        // Given
        HealthEventEntity entity1 = HealthEventEntity.builder()
                .eventId(UUID.randomUUID())
                .animalId(animalId)
                .eventType(HealthEventEntity.EventType.VACCINATION)
                .fechaEvento(LocalDate.now().minusDays(10))
                .descripcion("Vacuna 1")
                .createdAt(Instant.now())
                .createdBy(createdBy)
                .build();
        
        HealthEventEntity entity2 = HealthEventEntity.builder()
                .eventId(UUID.randomUUID())
                .animalId(animalId)
                .eventType(HealthEventEntity.EventType.TREATMENT)
                .fechaEvento(LocalDate.now().minusDays(5))
                .descripcion("Tratamiento 1")
                .createdAt(Instant.now())
                .createdBy(createdBy)
                .build();
        
        when(jpaRepository.findByAnimalIdOrderByFechaEventoDesc(animalId))
                .thenReturn(List.of(entity2, entity1));
        
        // When
        List<HealthEvent> events = repository.findByAnimalId(animalId);
        
        // Then
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getTipoEvento()).isEqualTo(HealthEventType.Tratamiento);
        assertThat(events.get(1).getTipoEvento()).isEqualTo(HealthEventType.Vacunacion);
        
        verify(jpaRepository, times(1)).findByAnimalIdOrderByFechaEventoDesc(animalId);
    }
    
    @Test
    @DisplayName("findByAnimalId() debe lanzar excepción si animalId es null")
    void findByAnimalId_shouldThrowExceptionWhenAnimalIdIsNull() {
        // When & Then
        assertThatThrownBy(() -> repository.findByAnimalId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("animalId no puede ser null");
        
        verify(jpaRepository, never()).findByAnimalIdOrderByFechaEventoDesc(any());
    }
    
    @Test
    @DisplayName("findByEventType() debe retornar eventos del tipo especificado")
    void findByEventType_shouldReturnEventsOfSpecifiedType() {
        // Given
        HealthEventEntity entity1 = HealthEventEntity.builder()
                .eventId(UUID.randomUUID())
                .animalId(animalId)
                .eventType(HealthEventEntity.EventType.BIRTH)
                .fechaEvento(LocalDate.now().minusDays(30))
                .descripcion("{\"cria_arete\": \"CR001\"}")
                .createdAt(Instant.now())
                .createdBy(createdBy)
                .build();
        
        when(jpaRepository.findByEventTypeOrderByFechaEventoDesc(HealthEventEntity.EventType.BIRTH))
                .thenReturn(List.of(entity1));
        
        // When
        List<HealthEvent> events = repository.findByEventType(HealthEventType.Birth, tenantId);
        
        // Then
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getTipoEvento()).isEqualTo(HealthEventType.Birth);
        
        verify(jpaRepository, times(1))
                .findByEventTypeOrderByFechaEventoDesc(HealthEventEntity.EventType.BIRTH);
    }
    
    @Test
    @DisplayName("findByEventType() debe lanzar excepción si eventType es null")
    void findByEventType_shouldThrowExceptionWhenEventTypeIsNull() {
        // When & Then
        assertThatThrownBy(() -> repository.findByEventType(null, tenantId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventType no puede ser null");
        
        verify(jpaRepository, never()).findByEventTypeOrderByFechaEventoDesc(any());
    }
    
    @Test
    @DisplayName("findByEventType() debe lanzar excepción si tenantId es null")
    void findByEventType_shouldThrowExceptionWhenTenantIdIsNull() {
        // When & Then
        assertThatThrownBy(() -> repository.findByEventType(HealthEventType.Vacunacion, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tenantId no puede ser null");
        
        verify(jpaRepository, never()).findByEventTypeOrderByFechaEventoDesc(any());
    }
    
    @Test
    @DisplayName("findUpcomingVaccinations() debe retornar vacunaciones próximas a vencer")
    void findUpcomingVaccinations_shouldReturnUpcomingVaccinations() {
        // Given
        HealthEventEntity entity1 = HealthEventEntity.builder()
                .eventId(UUID.randomUUID())
                .animalId(animalId)
                .eventType(HealthEventEntity.EventType.VACCINATION)
                .fechaEvento(LocalDate.now().minusDays(340))
                .descripcion("{\"vacuna_nombre\": \"Rabia\", \"intervalo_dias\": 365}")
                .createdAt(Instant.now())
                .createdBy(createdBy)
                .build();
        
        when(jpaRepository.findUpcomingVaccinations(eq(tenantId), eq(30), any(LocalDate.class)))
                .thenReturn(List.of(entity1));
        
        // When
        List<HealthEvent> vaccinations = repository.findUpcomingVaccinations(tenantId, 30);
        
        // Then
        assertThat(vaccinations).hasSize(1);
        assertThat(vaccinations.get(0).getTipoEvento()).isEqualTo(HealthEventType.Vacunacion);
        
        verify(jpaRepository, times(1))
                .findUpcomingVaccinations(eq(tenantId), eq(30), any(LocalDate.class));
    }
    
    @Test
    @DisplayName("findUpcomingVaccinations() debe lanzar excepción si tenantId es null")
    void findUpcomingVaccinations_shouldThrowExceptionWhenTenantIdIsNull() {
        // When & Then
        assertThatThrownBy(() -> repository.findUpcomingVaccinations(null, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tenantId no puede ser null");
        
        verify(jpaRepository, never()).findUpcomingVaccinations(any(), anyInt(), any());
    }
    
    @Test
    @DisplayName("findUpcomingVaccinations() debe lanzar excepción si daysAhead es negativo")
    void findUpcomingVaccinations_shouldThrowExceptionWhenDaysAheadIsNegative() {
        // When & Then
        assertThatThrownBy(() -> repository.findUpcomingVaccinations(tenantId, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("daysAhead debe ser mayor o igual a 0");
        
        verify(jpaRepository, never()).findUpcomingVaccinations(any(), anyInt(), any());
    }
    
    @Test
    @DisplayName("findBirthEventsByMotherId() debe retornar eventos de parto de la madre")
    void findBirthEventsByMotherId_shouldReturnBirthEventsForMother() {
        // Given
        UUID motherId = UUID.randomUUID();
        
        HealthEventEntity birthEvent1 = HealthEventEntity.builder()
                .eventId(UUID.randomUUID())
                .animalId(motherId)
                .eventType(HealthEventEntity.EventType.BIRTH)
                .fechaEvento(LocalDate.now().minusDays(60))
                .descripcion("{\"cria_arete\": \"CR001\"}")
                .createdAt(Instant.now())
                .createdBy(createdBy)
                .build();
        
        HealthEventEntity birthEvent2 = HealthEventEntity.builder()
                .eventId(UUID.randomUUID())
                .animalId(motherId)
                .eventType(HealthEventEntity.EventType.BIRTH)
                .fechaEvento(LocalDate.now().minusDays(30))
                .descripcion("{\"cria_arete\": \"CR002\"}")
                .createdAt(Instant.now())
                .createdBy(createdBy)
                .build();
        
        when(jpaRepository.findByEventTypeOrderByFechaEventoDesc(HealthEventEntity.EventType.BIRTH))
                .thenReturn(List.of(birthEvent2, birthEvent1));
        
        // When
        List<HealthEvent> birthEvents = repository.findBirthEventsByMotherId(motherId);
        
        // Then
        assertThat(birthEvents).hasSize(2);
        assertThat(birthEvents).allMatch(event -> event.getTipoEvento() == HealthEventType.Birth);
        assertThat(birthEvents).allMatch(event -> event.getAnimalId().equals(motherId));
        
        verify(jpaRepository, times(1))
                .findByEventTypeOrderByFechaEventoDesc(HealthEventEntity.EventType.BIRTH);
    }
    
    @Test
    @DisplayName("findBirthEventsByMotherId() debe lanzar excepción si motherId es null")
    void findBirthEventsByMotherId_shouldThrowExceptionWhenMotherIdIsNull() {
        // When & Then
        assertThatThrownBy(() -> repository.findBirthEventsByMotherId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("motherId no puede ser null");
        
        verify(jpaRepository, never()).findByEventTypeOrderByFechaEventoDesc(any());
    }
    
    @Test
    @DisplayName("findByAnimalId() debe retornar lista vacía si no hay eventos")
    void findByAnimalId_shouldReturnEmptyListWhenNoEvents() {
        // Given
        when(jpaRepository.findByAnimalIdOrderByFechaEventoDesc(animalId))
                .thenReturn(List.of());
        
        // When
        List<HealthEvent> events = repository.findByAnimalId(animalId);
        
        // Then
        assertThat(events).isEmpty();
        
        verify(jpaRepository, times(1)).findByAnimalIdOrderByFechaEventoDesc(animalId);
    }
}
