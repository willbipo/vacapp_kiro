package mx.vacapp.geography.internal.infrastructure.persistence.mappers;

import lombok.RequiredArgsConstructor;
import mx.vacapp.geography.internal.domain.model.Seccion;
import mx.vacapp.geography.internal.infrastructure.persistence.entities.SeccionEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper para transformar entre entidad de dominio Seccion y entidad JPA SeccionEntity.
 * 
 * Responsabilidad: Transformación pura sin lógica de negocio.
 */
@Component
@RequiredArgsConstructor
public class SeccionMapper {
    
    /**
     * Convierte una entidad de dominio Seccion a entidad JPA SeccionEntity.
     * 
     * @param seccion entidad de dominio
     * @return entidad JPA para persistencia
     */
    public SeccionEntity toEntity(Seccion seccion) {
        if (seccion == null) {
            return null;
        }
        
        return SeccionEntity.builder()
            .seccionId(seccion.getSeccionId())
            .nombre(seccion.getNombre())
            .superficie(seccion.getSuperficie())
            .ranchoId(seccion.getRanchoId())
            .descripcion(seccion.getDescripcion())
            .status(seccion.getStatus())
            .tenantId(seccion.getTenantId())
            .createdAt(seccion.getCreatedAt())
            .updatedAt(seccion.getUpdatedAt())
            .createdBy(seccion.getCreatedBy())
            .updatedBy(seccion.getUpdatedBy())
            .build();
    }
    
    /**
     * Convierte una entidad JPA SeccionEntity a entidad de dominio Seccion.
     * 
     * @param entity entidad JPA
     * @return entidad de dominio
     */
    public Seccion toDomain(SeccionEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new Seccion.Builder()
            .seccionId(entity.getSeccionId())
            .nombre(entity.getNombre())
            .superficie(entity.getSuperficie())
            .ranchoId(entity.getRanchoId())
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
