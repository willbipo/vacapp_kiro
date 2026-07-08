package mx.vacapp.cattle.internal.infrastructure.persistence.mappers;

import lombok.RequiredArgsConstructor;
import mx.vacapp.cattle.internal.domain.model.WeightRecord;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.WeightEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper para transformar entre entidad de dominio WeightRecord y entidad JPA WeightEntity.
 * 
 * Responsabilidad: Transformación pura sin lógica de negocio.
 */
@Component
@RequiredArgsConstructor
public class WeightMapper {
    
    /**
     * Convierte una entidad de dominio WeightRecord a entidad JPA WeightEntity.
     * 
     * @param weightRecord entidad de dominio
     * @return entidad JPA para persistencia
     */
    public static WeightEntity toEntity(WeightRecord weightRecord) {
        if (weightRecord == null) {
            return null;
        }
        
        return WeightEntity.builder()
            .weightId(weightRecord.getWeightId())
            .animalId(weightRecord.getAnimalId())
            .pesoKg(weightRecord.getPesoKg())
            .fechaPesaje(weightRecord.getFechaPesaje())
            .notas(weightRecord.getNotas())
            .createdAt(weightRecord.getCreatedAt())
            .createdBy(weightRecord.getCreatedBy())
            .build();
    }
    
    /**
     * Convierte una entidad JPA WeightEntity a entidad de dominio WeightRecord.
     * 
     * @param entity entidad JPA
     * @return entidad de dominio
     */
    public static WeightRecord toDomain(WeightEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new WeightRecord.Builder()
            .weightId(entity.getWeightId())
            .animalId(entity.getAnimalId())
            .pesoKg(entity.getPesoKg())
            .fechaPesaje(entity.getFechaPesaje())
            .notas(entity.getNotas())
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .build();
    }
}
