package mx.vacapp.cattle.internal.infrastructure.persistence.mappers;

import mx.vacapp.cattle.internal.domain.model.PastureHistory;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.PastureHistoryEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para PastureHistoryMapper.
 * 
 * Verifica transformación bidireccional correcta entre PastureHistory (dominio)
 * y PastureHistoryEntity (JPA) incluyendo el campo calculado diasPermanencia.
 */
class PastureHistoryMapperTest {
    
    @Test
    void toEntity_shouldMapAllFieldsCorrectly() {
        // Given: entidad de dominio completa
        UUID historyId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID potreroId = UUID.randomUUID();
        Instant fechaEntrada = Instant.now().minusSeconds(86400 * 5); // 5 días atrás
        Instant fechaSalida = Instant.now();
        Integer diasPermanencia = 5;
        Instant createdAt = Instant.now().minusSeconds(86400 * 5);
        UUID createdBy = UUID.randomUUID();
        
        PastureHistory domain = PastureHistory.restore(
                historyId, animalId, potreroId,
                fechaEntrada, fechaSalida, diasPermanencia,
                createdAt, createdBy
        );
        
        // When: convertir a entidad JPA
        PastureHistoryEntity entity = PastureHistoryMapper.toEntity(domain);
        
        // Then: todos los campos deben estar mapeados correctamente
        assertNotNull(entity);
        assertEquals(historyId, entity.getHistoryId());
        assertEquals(animalId, entity.getAnimalId());
        assertEquals(potreroId, entity.getPotreroId());
        assertEquals(fechaEntrada, entity.getFechaEntrada());
        assertEquals(fechaSalida, entity.getFechaSalida());
        assertEquals(diasPermanencia, entity.getDiasPermanencia());
        assertEquals(createdAt, entity.getCreatedAt());
        assertEquals(createdBy, entity.getCreatedBy());
    }
    
    @Test
    void toEntity_withNullFechaSalida_shouldMapCorrectly() {
        // Given: entidad de dominio con fecha_salida = null (ubicación actual)
        UUID historyId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID potreroId = UUID.randomUUID();
        Instant fechaEntrada = Instant.now().minusSeconds(86400 * 3);
        UUID createdBy = UUID.randomUUID();
        
        PastureHistory domain = PastureHistory.restore(
                historyId, animalId, potreroId,
                fechaEntrada, null, null,
                Instant.now().minusSeconds(86400 * 3), createdBy
        );
        
        // When: convertir a entidad JPA
        PastureHistoryEntity entity = PastureHistoryMapper.toEntity(domain);
        
        // Then: fecha_salida debe ser null
        assertNotNull(entity);
        assertEquals(historyId, entity.getHistoryId());
        assertNull(entity.getFechaSalida());
        assertNull(entity.getDiasPermanencia());
    }
    
    @Test
    void toEntity_withNullInput_shouldReturnNull() {
        // Given: input null
        PastureHistory domain = null;
        
        // When: intentar convertir
        PastureHistoryEntity entity = PastureHistoryMapper.toEntity(domain);
        
        // Then: debe retornar null
        assertNull(entity);
    }
    
    @Test
    void toDomain_shouldMapAllFieldsCorrectly() {
        // Given: entidad JPA completa
        UUID historyId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID potreroId = UUID.randomUUID();
        Instant fechaEntrada = Instant.now().minusSeconds(86400 * 5);
        Instant fechaSalida = Instant.now();
        Integer diasPermanencia = 5;
        Instant createdAt = Instant.now().minusSeconds(86400 * 5);
        UUID createdBy = UUID.randomUUID();
        
        PastureHistoryEntity entity = PastureHistoryEntity.builder()
                .historyId(historyId)
                .animalId(animalId)
                .potreroId(potreroId)
                .fechaEntrada(fechaEntrada)
                .fechaSalida(fechaSalida)
                .diasPermanencia(diasPermanencia)
                .createdAt(createdAt)
                .createdBy(createdBy)
                .build();
        
        // When: convertir a entidad de dominio
        PastureHistory domain = PastureHistoryMapper.toDomain(entity);
        
        // Then: todos los campos deben estar mapeados correctamente
        assertNotNull(domain);
        assertEquals(historyId, domain.getHistoryId());
        assertEquals(animalId, domain.getAnimalId());
        assertEquals(potreroId, domain.getPotreroId());
        assertEquals(fechaEntrada, domain.getFechaEntrada());
        assertEquals(fechaSalida, domain.getFechaSalida());
        assertEquals(diasPermanencia, domain.getDiasPermanencia());
        assertEquals(createdAt, domain.getCreatedAt());
        assertEquals(createdBy, domain.getCreatedBy());
    }
    
    @Test
    void toDomain_withNullFechaSalida_shouldMapCorrectly() {
        // Given: entidad JPA con fecha_salida = null (ubicación actual)
        UUID historyId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID potreroId = UUID.randomUUID();
        Instant fechaEntrada = Instant.now().minusSeconds(86400 * 3);
        UUID createdBy = UUID.randomUUID();
        
        PastureHistoryEntity entity = PastureHistoryEntity.builder()
                .historyId(historyId)
                .animalId(animalId)
                .potreroId(potreroId)
                .fechaEntrada(fechaEntrada)
                .fechaSalida(null)
                .diasPermanencia(null)
                .createdAt(Instant.now().minusSeconds(86400 * 3))
                .createdBy(createdBy)
                .build();
        
        // When: convertir a entidad de dominio
        PastureHistory domain = PastureHistoryMapper.toDomain(entity);
        
        // Then: fecha_salida debe ser null y debe ser ubicación actual
        assertNotNull(domain);
        assertEquals(historyId, domain.getHistoryId());
        assertNull(domain.getFechaSalida());
        assertNull(domain.getDiasPermanencia());
        assertTrue(domain.isCurrent());
    }
    
    @Test
    void toDomain_withNullInput_shouldReturnNull() {
        // Given: input null
        PastureHistoryEntity entity = null;
        
        // When: intentar convertir
        PastureHistory domain = PastureHistoryMapper.toDomain(entity);
        
        // Then: debe retornar null
        assertNull(domain);
    }
    
    @Test
    void roundTrip_shouldPreserveAllData() {
        // Given: entidad de dominio original
        UUID historyId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID potreroId = UUID.randomUUID();
        Instant fechaEntrada = Instant.now().minusSeconds(86400 * 7);
        Instant fechaSalida = Instant.now();
        Integer diasPermanencia = 7;
        Instant createdAt = Instant.now().minusSeconds(86400 * 7);
        UUID createdBy = UUID.randomUUID();
        
        PastureHistory original = PastureHistory.restore(
                historyId, animalId, potreroId,
                fechaEntrada, fechaSalida, diasPermanencia,
                createdAt, createdBy
        );
        
        // When: convertir domain → entity → domain
        PastureHistoryEntity entity = PastureHistoryMapper.toEntity(original);
        PastureHistory restored = PastureHistoryMapper.toDomain(entity);
        
        // Then: los datos deben ser idénticos
        assertNotNull(restored);
        assertEquals(original.getHistoryId(), restored.getHistoryId());
        assertEquals(original.getAnimalId(), restored.getAnimalId());
        assertEquals(original.getPotreroId(), restored.getPotreroId());
        assertEquals(original.getFechaEntrada(), restored.getFechaEntrada());
        assertEquals(original.getFechaSalida(), restored.getFechaSalida());
        assertEquals(original.getDiasPermanencia(), restored.getDiasPermanencia());
        assertEquals(original.getCreatedAt(), restored.getCreatedAt());
        assertEquals(original.getCreatedBy(), restored.getCreatedBy());
        assertEquals(original.isCurrent(), restored.isCurrent());
    }
    
    @Test
    void mapperConstructor_shouldThrowException() {
        // Given/When/Then: intentar instanciar mapper debe lanzar excepción
        var exception = assertThrows(Exception.class, () -> {
            // Usar reflexión para intentar instanciar
            var constructor = PastureHistoryMapper.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
        
        // Verificar que la causa raíz es UnsupportedOperationException
        Throwable cause = exception.getCause();
        assertNotNull(cause);
        assertTrue(cause instanceof UnsupportedOperationException);
        assertEquals("Utility class - no se debe instanciar", cause.getMessage());
    }
}
