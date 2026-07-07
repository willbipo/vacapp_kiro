package mx.vacapp.geography.internal.infrastructure.persistence.mappers;

import lombok.RequiredArgsConstructor;
import mx.vacapp.geography.internal.domain.model.Rancho;
import mx.vacapp.geography.internal.infrastructure.persistence.entities.RanchoEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper para transformar entre entidad de dominio Rancho y entidad JPA RanchoEntity.
 * 
 * Responsabilidad: Transformación pura sin lógica de negocio.
 */
@Component
@RequiredArgsConstructor
public class RanchoMapper {
    
    /**
     * Convierte una entidad de dominio Rancho a entidad JPA RanchoEntity.
     * 
     * @param rancho entidad de dominio
     * @return entidad JPA para persistencia
     */
    public RanchoEntity toEntity(Rancho rancho) {
        if (rancho == null) {
            return null;
        }
        
        return RanchoEntity.builder()
            .ranchoId(rancho.getRanchoId())
            .nombre(rancho.getNombre())
            .superficieTotal(rancho.getSuperficieTotal())
            .descripcion(rancho.getDescripcion())
            .status(rancho.getStatus())
            .tenantId(rancho.getTenantId())
            .createdAt(rancho.getCreatedAt())
            .updatedAt(rancho.getUpdatedAt())
            .createdBy(rancho.getCreatedBy())
            .updatedBy(rancho.getUpdatedBy())
            .build();
    }
    
    /**
     * Convierte una entidad JPA RanchoEntity a entidad de dominio Rancho.
     * 
     * @param entity entidad JPA
     * @return entidad de dominio
     */
    public Rancho toDomain(RanchoEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new Rancho.Builder()
            .ranchoId(entity.getRanchoId())
            .nombre(entity.getNombre())
            .superficieTotal(entity.getSuperficieTotal())
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
