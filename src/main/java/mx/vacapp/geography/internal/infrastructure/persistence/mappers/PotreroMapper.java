package mx.vacapp.geography.internal.infrastructure.persistence.mappers;

import lombok.RequiredArgsConstructor;
import mx.vacapp.geography.internal.domain.model.Potrero;
import mx.vacapp.geography.internal.infrastructure.persistence.entities.PotreroEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper para transformar entre entidad de dominio Potrero y entidad JPA PotreroEntity.
 * 
 * Responsabilidad: Transformación pura sin lógica de negocio.
 */
@Component
@RequiredArgsConstructor
public class PotreroMapper {
    
    /**
     * Convierte una entidad de dominio Potrero a entidad JPA PotreroEntity.
     * 
     * @param potrero entidad de dominio
     * @return entidad JPA para persistencia
     */
    public PotreroEntity toEntity(Potrero potrero) {
        if (potrero == null) {
            return null;
        }
        
        return PotreroEntity.builder()
            .potreroId(potrero.getPotreroId())
            .nombre(potrero.getNombre())
            .superficie(potrero.getSuperficie())
            .ranchoId(potrero.getRanchoId())
            .seccionId(potrero.getSeccionId())
            .cattleCount(potrero.getCattleCount())
            .descripcion(potrero.getDescripcion())
            .status(potrero.getStatus())
            .tenantId(potrero.getTenantId())
            .createdAt(potrero.getCreatedAt())
            .updatedAt(potrero.getUpdatedAt())
            .createdBy(potrero.getCreatedBy())
            .updatedBy(potrero.getUpdatedBy())
            .build();
    }
    
    /**
     * Convierte una entidad JPA PotreroEntity a entidad de dominio Potrero.
     * 
     * @param entity entidad JPA
     * @return entidad de dominio
     */
    public Potrero toDomain(PotreroEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new Potrero.Builder()
            .potreroId(entity.getPotreroId())
            .nombre(entity.getNombre())
            .superficie(entity.getSuperficie())
            .ranchoId(entity.getRanchoId())
            .seccionId(entity.getSeccionId())
            .cattleCount(entity.getCattleCount())
            .descripcion(entity.getDescripcion())
            .status(entity.getStatus())
            .tenantId(entity.getTenantId())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
