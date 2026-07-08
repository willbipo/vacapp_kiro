package mx.vacapp.cattle.internal.infrastructure.persistence.mappers;

import mx.vacapp.cattle.internal.domain.model.WeightRecord;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.WeightEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para WeightMapper.
 */
class WeightMapperTest {
    
    @Test
    void shouldMapWeightRecordToEntity() {
        // Given
        UUID weightId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        BigDecimal pesoKg = new BigDecimal("350.50");
        LocalDate fechaPesaje = LocalDate.of(2024, 1, 15);
        String notas = "Peso normal para la edad";
        Instant createdAt = Instant.now();
        
        WeightRecord weightRecord = new WeightRecord.Builder()
            .weightId(weightId)
            .animalId(animalId)
            .pesoKg(pesoKg)
            .fechaPesaje(fechaPesaje)
            .notas(notas)
            .createdAt(createdAt)
            .createdBy(createdBy)
            .build();
        
        // When
        WeightEntity entity = WeightMapper.toEntity(weightRecord);
        
        // Then
        assertNotNull(entity);
        assertEquals(weightId, entity.getWeightId());
        assertEquals(animalId, entity.getAnimalId());
        assertEquals(pesoKg, entity.getPesoKg());
        assertEquals(fechaPesaje, entity.getFechaPesaje());
        assertEquals(notas, entity.getNotas());
        assertEquals(createdAt, entity.getCreatedAt());
        assertEquals(createdBy, entity.getCreatedBy());
    }
    
    @Test
    void shouldMapEntityToWeightRecord() {
        // Given
        UUID weightId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        BigDecimal pesoKg = new BigDecimal("425.75");
        LocalDate fechaPesaje = LocalDate.of(2024, 3, 20);
        String notas = "Ganancia de peso excelente";
        Instant createdAt = Instant.now();
        
        WeightEntity entity = WeightEntity.builder()
            .weightId(weightId)
            .animalId(animalId)
            .pesoKg(pesoKg)
            .fechaPesaje(fechaPesaje)
            .notas(notas)
            .createdAt(createdAt)
            .createdBy(createdBy)
            .build();
        
        // When
        WeightRecord weightRecord = WeightMapper.toDomain(entity);
        
        // Then
        assertNotNull(weightRecord);
        assertEquals(weightId, weightRecord.getWeightId());
        assertEquals(animalId, weightRecord.getAnimalId());
        assertEquals(pesoKg, weightRecord.getPesoKg());
        assertEquals(fechaPesaje, weightRecord.getFechaPesaje());
        assertEquals(notas, weightRecord.getNotas());
        assertEquals(createdAt, weightRecord.getCreatedAt());
        assertEquals(createdBy, weightRecord.getCreatedBy());
    }
    
    @Test
    void shouldReturnNullWhenWeightRecordIsNull() {
        // When
        WeightEntity entity = WeightMapper.toEntity(null);
        
        // Then
        assertNull(entity);
    }
    
    @Test
    void shouldReturnNullWhenEntityIsNull() {
        // When
        WeightRecord weightRecord = WeightMapper.toDomain(null);
        
        // Then
        assertNull(weightRecord);
    }
    
    @Test
    void shouldMapWeightRecordWithoutNotas() {
        // Given
        UUID weightId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        BigDecimal pesoKg = new BigDecimal("300.00");
        LocalDate fechaPesaje = LocalDate.of(2024, 2, 10);
        Instant createdAt = Instant.now();
        
        WeightRecord weightRecord = new WeightRecord.Builder()
            .weightId(weightId)
            .animalId(animalId)
            .pesoKg(pesoKg)
            .fechaPesaje(fechaPesaje)
            .notas(null)
            .createdAt(createdAt)
            .createdBy(createdBy)
            .build();
        
        // When
        WeightEntity entity = WeightMapper.toEntity(weightRecord);
        
        // Then
        assertNotNull(entity);
        assertEquals(weightId, entity.getWeightId());
        assertEquals(animalId, entity.getAnimalId());
        assertEquals(pesoKg, entity.getPesoKg());
        assertEquals(fechaPesaje, entity.getFechaPesaje());
        assertNull(entity.getNotas());
        assertEquals(createdAt, entity.getCreatedAt());
        assertEquals(createdBy, entity.getCreatedBy());
    }
}
