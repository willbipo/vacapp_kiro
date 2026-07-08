package mx.vacapp.cattle.internal.infrastructure.persistence.mappers;

import mx.vacapp.cattle.internal.domain.model.PastureHistory;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.PastureHistoryEntity;

/**
 * Mapper para transformar entre entidad de dominio PastureHistory y entidad JPA PastureHistoryEntity.
 * 
 * Este mapper utiliza métodos estáticos para transformación pura sin lógica de negocio.
 * No usa MapStruct - implementación manual con mapeo directo de campos.
 * 
 * Responsabilidad: Transformación bidireccional entre capas de dominio e infraestructura.
 * 
 * @see PastureHistory entidad de dominio
 * @see PastureHistoryEntity entidad JPA
 */
public class PastureHistoryMapper {
    
    /**
     * Constructor privado para prevenir instanciación.
     * Esta clase solo contiene métodos estáticos.
     */
    private PastureHistoryMapper() {
        throw new UnsupportedOperationException("Utility class - no se debe instanciar");
    }
    
    /**
     * Convierte una entidad de dominio PastureHistory a entidad JPA PastureHistoryEntity.
     * 
     * Mapea todos los campos incluyendo el campo calculado diasPermanencia.
     * El campo diasPermanencia se copia desde el dominio pero será recalculado
     * automáticamente por MySQL como GENERATED STORED column.
     * 
     * @param pastureHistory entidad de dominio
     * @return entidad JPA para persistencia, o null si el parámetro es null
     */
    public static PastureHistoryEntity toEntity(PastureHistory pastureHistory) {
        if (pastureHistory == null) {
            return null;
        }
        
        return PastureHistoryEntity.builder()
                .historyId(pastureHistory.getHistoryId())
                .animalId(pastureHistory.getAnimalId())
                .potreroId(pastureHistory.getPotreroId())
                .fechaEntrada(pastureHistory.getFechaEntrada())
                .fechaSalida(pastureHistory.getFechaSalida())
                .diasPermanencia(pastureHistory.getDiasPermanencia())
                .createdAt(pastureHistory.getCreatedAt())
                .createdBy(pastureHistory.getCreatedBy())
                .build();
    }
    
    /**
     * Convierte una entidad JPA PastureHistoryEntity a entidad de dominio PastureHistory.
     * 
     * Mapea todos los campos incluyendo el campo calculado diasPermanencia que
     * es calculado automáticamente por MySQL como GENERATED STORED column.
     * 
     * Utiliza el factory method restore() de PastureHistory para reconstruir
     * el objeto de dominio desde persistencia.
     * 
     * @param entity entidad JPA
     * @return entidad de dominio, o null si el parámetro es null
     */
    public static PastureHistory toDomain(PastureHistoryEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return PastureHistory.restore(
                entity.getHistoryId(),
                entity.getAnimalId(),
                entity.getPotreroId(),
                entity.getFechaEntrada(),
                entity.getFechaSalida(),
                entity.getDiasPermanencia(),
                entity.getCreatedAt(),
                entity.getCreatedBy()
        );
    }
}
