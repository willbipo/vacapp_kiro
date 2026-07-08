package mx.vacapp.cattle.internal.infrastructure.persistence.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.vacapp.cattle.internal.domain.model.*;
import mx.vacapp.cattle.internal.domain.repository.CattleAuditRepository;
import mx.vacapp.cattle.internal.domain.repository.CattleAuditRepository.AuditOperationType;
import mx.vacapp.cattle.internal.domain.repository.CattleAuditRepository.CattleAudit;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.AnimalEntity;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.CattleAuditEntity;
import mx.vacapp.cattle.internal.infrastructure.persistence.repositories.AnimalJpaRepository;
import mx.vacapp.cattle.internal.infrastructure.persistence.repositories.CattleAuditJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test unitario para CattleAuditRepositoryImpl.
 * 
 * Verifica:
 * - Guardado de registros de auditoría genéricos
 * - Registro de creación de animales con JSON completo
 * - Registro de actualizaciones con oldValues/newValues
 * - Registro de cambios de estado
 * - Registro de movimientos entre potreros
 * - Búsqueda por animal_id con paginación
 * - Búsqueda por tipo de operación y tenant
 * - Conversión correcta entre enums de dominio y entidad
 * - Validaciones de argumentos null
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CattleAuditRepositoryImpl Tests")
class CattleAuditRepositoryImplTest {
    
    @Mock
    private CattleAuditJpaRepository jpaRepository;
    
    @Mock
    private AnimalJpaRepository animalJpaRepository;
    
    @Spy
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private CattleAuditRepositoryImpl repository;
    
    private UUID animalId;
    private UUID tenantId;
    private UUID createdBy;
    private UUID ranchoId;
    
    @BeforeEach
    void setUp() {
        animalId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        createdBy = UUID.randomUUID();
        ranchoId = UUID.randomUUID();
    }
    
    @Test
    @DisplayName("log() debe guardar un registro de auditoría")
    void log_shouldPersistAuditRecord() {
        // Given
        CattleAudit audit = new CattleAudit(
            UUID.randomUUID(),
            animalId,
            AuditOperationType.CREATE,
            Instant.now(),
            createdBy,
            tenantId,
            null,
            "{\"arete\":\"TEST001\"}",
            null
        );
        
        when(jpaRepository.save(any(CattleAuditEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        repository.log(audit);
        
        // Then
        verify(jpaRepository, times(1)).save(any(CattleAuditEntity.class));
    }
    
    @Test
    @DisplayName("log() debe lanzar excepción si audit es null")
    void log_shouldThrowExceptionWhenAuditIsNull() {
        // When & Then
        assertThatThrownBy(() -> repository.log(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Audit cannot be null");
        
        verify(jpaRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("logAnimalCreation() debe registrar creación con datos completos en JSON")
    void logAnimalCreation_shouldLogCreationWithCompleteDataInJson() {
        // Given
        Animal animal = Animal.create(
            "TEST001",
            Sex.HEMBRA,
            Breed.CHAROLAIS,
            LocalDate.now().minusYears(2),
            CattleType.VENTA,
            ranchoId,
            tenantId,
            createdBy
        );
        
        when(jpaRepository.save(any(CattleAuditEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        repository.logAnimalCreation(animalId, animal, createdBy);
        
        // Then
        verify(jpaRepository, times(1)).save(any(CattleAuditEntity.class));
    }
    
    @Test
    @DisplayName("logAnimalCreation() debe lanzar excepción si animalId es null")
    void logAnimalCreation_shouldThrowExceptionWhenAnimalIdIsNull() {
        // Given
        Animal animal = Animal.create(
            "TEST001",
            Sex.HEMBRA,
            Breed.CHAROLAIS,
            LocalDate.now().minusYears(2),
            CattleType.VENTA,
            ranchoId,
            tenantId,
            createdBy
        );
        
        // When & Then
        assertThatThrownBy(() -> repository.logAnimalCreation(null, animal, createdBy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Animal ID cannot be null");
        
        verify(jpaRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("logAnimalUpdate() debe registrar actualización con oldValues y newValues")
    void logAnimalUpdate_shouldLogUpdateWithOldAndNewValues() {
        // Given
        Map<String, Object> oldValues = Map.of("tipo", "venta");
        Map<String, Object> newValues = Map.of("tipo", "cria");
        
        when(jpaRepository.save(any(CattleAuditEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        repository.logAnimalUpdate(animalId, oldValues, newValues, createdBy, tenantId);
        
        // Then
        verify(jpaRepository, times(1)).save(any(CattleAuditEntity.class));
    }
    
    @Test
    @DisplayName("logStatusChange() debe registrar cambio de estado y obtener tenantId del animal")
    void logStatusChange_shouldLogStatusChangeAndRetrieveTenantId() {
        // Given
        AnimalEntity animalEntity = AnimalEntity.builder()
            .animalId(animalId)
            .tenantId(tenantId)
            .build();
        
        when(animalJpaRepository.findById(animalId)).thenReturn(Optional.of(animalEntity));
        when(jpaRepository.save(any(CattleAuditEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        repository.logStatusChange(animalId, "activa", "vendida", createdBy, "Venta completada");
        
        // Then
        verify(animalJpaRepository, times(1)).findById(animalId);
        verify(jpaRepository, times(1)).save(any(CattleAuditEntity.class));
    }
    
    @Test
    @DisplayName("logStatusChange() debe lanzar excepción si animal no existe")
    void logStatusChange_shouldThrowExceptionWhenAnimalNotFound() {
        // Given
        when(animalJpaRepository.findById(animalId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> repository.logStatusChange(animalId, "activa", "vendida", createdBy, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Animal not found");
        
        verify(jpaRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("logMovement() debe registrar movimiento entre potreros")
    void logMovement_shouldLogMovementBetweenPastures() {
        // Given
        UUID oldPotreroId = UUID.randomUUID();
        UUID newPotreroId = UUID.randomUUID();
        
        AnimalEntity animalEntity = AnimalEntity.builder()
            .animalId(animalId)
            .tenantId(tenantId)
            .build();
        
        when(animalJpaRepository.findById(animalId)).thenReturn(Optional.of(animalEntity));
        when(jpaRepository.save(any(CattleAuditEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        repository.logMovement(animalId, oldPotreroId, newPotreroId, createdBy);
        
        // Then
        verify(animalJpaRepository, times(1)).findById(animalId);
        verify(jpaRepository, times(1)).save(any(CattleAuditEntity.class));
    }
    
    @Test
    @DisplayName("findByAnimalId() debe retornar registros de auditoría paginados")
    void findByAnimalId_shouldReturnPaginatedAuditRecords() {
        // Given
        CattleAuditEntity entity = CattleAuditEntity.builder()
            .auditId(UUID.randomUUID())
            .animalId(animalId)
            .operationType(CattleAuditEntity.OperationType.CREATE)
            .timestamp(Instant.now())
            .modifiedBy(createdBy)
            .tenantId(tenantId)
            .newValues("{\"arete\":\"TEST001\"}")
            .build();
        
        when(jpaRepository.findByAnimalIdOrderByTimestampDesc(eq(animalId), any(PageRequest.class)))
            .thenReturn(List.of(entity));
        
        // When
        List<CattleAudit> audits = repository.findByAnimalId(animalId, 0, 10);
        
        // Then
        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).animalId()).isEqualTo(animalId);
        assertThat(audits.get(0).operationType()).isEqualTo(AuditOperationType.CREATE);
        
        verify(jpaRepository, times(1)).findByAnimalIdOrderByTimestampDesc(eq(animalId), any(PageRequest.class));
    }
    
    @Test
    @DisplayName("findByAnimalId() debe lanzar excepción si offset es negativo")
    void findByAnimalId_shouldThrowExceptionWhenOffsetIsNegative() {
        // When & Then
        assertThatThrownBy(() -> repository.findByAnimalId(animalId, -1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Offset must be non-negative");
        
        verify(jpaRepository, never()).findByAnimalIdOrderByTimestampDesc(any(), any());
    }
    
    @Test
    @DisplayName("findByOperationType() debe retornar registros filtrados por tipo de operación")
    void findByOperationType_shouldReturnRecordsFilteredByOperationType() {
        // Given
        CattleAuditEntity entity = CattleAuditEntity.builder()
            .auditId(UUID.randomUUID())
            .animalId(animalId)
            .operationType(CattleAuditEntity.OperationType.MOVE_PASTURE)
            .timestamp(Instant.now())
            .modifiedBy(createdBy)
            .tenantId(tenantId)
            .newValues("{\"potrero_id\":\"" + UUID.randomUUID() + "\"}")
            .build();
        
        when(jpaRepository.findByOperationTypeAndTenantIdOrderByTimestampDesc(
            eq(CattleAuditEntity.OperationType.MOVE_PASTURE),
            eq(tenantId),
            any(PageRequest.class)
        )).thenReturn(List.of(entity));
        
        // When
        List<CattleAudit> audits = repository.findByOperationType(
            AuditOperationType.MOVE_PASTURE,
            tenantId,
            0,
            10
        );
        
        // Then
        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).operationType()).isEqualTo(AuditOperationType.MOVE_PASTURE);
        
        verify(jpaRepository, times(1)).findByOperationTypeAndTenantIdOrderByTimestampDesc(
            any(), eq(tenantId), any(PageRequest.class)
        );
    }
    
    @Test
    @DisplayName("findByOperationType() debe lanzar excepción si operationType es null")
    void findByOperationType_shouldThrowExceptionWhenOperationTypeIsNull() {
        // When & Then
        assertThatThrownBy(() -> repository.findByOperationType(null, tenantId, 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Operation type cannot be null");
        
        verify(jpaRepository, never()).findByOperationTypeAndTenantIdOrderByTimestampDesc(any(), any(), any());
    }
}
