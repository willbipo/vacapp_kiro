package mx.vacapp.cattle.internal.infrastructure.persistence.mappers;

import mx.vacapp.cattle.internal.domain.model.HealthEvent;
import mx.vacapp.cattle.internal.domain.model.HealthEventType;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.HealthEventEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper para transformar entre la capa de dominio (HealthEvent) y la capa de persistencia (HealthEventEntity).
 * <p>
 * Esta clase es responsable de convertir objetos HealthEvent del dominio puro a entidades JPA HealthEventEntity
 * y viceversa, garantizando la separación entre las capas de dominio e infraestructura
 * según los principios de Clean Architecture.
 * </p>
 * <p>
 * Las transformaciones incluyen:
 * <ul>
 *   <li>Conversión de enum HealthEventType ↔ HealthEventEntity.EventType</li>
 *   <li>Mapeo directo de UUIDs, LocalDate, BigDecimal e Instants</li>
 *   <li>Mapeo de todos los campos de auditoría</li>
 * </ul>
 * </p>
 */
@Component
public class HealthEventMapper {
    
    /**
     * Convierte una entidad de dominio HealthEvent a una entidad JPA HealthEventEntity.
     * <p>
     * Este método transforma un objeto HealthEvent del dominio (POJO puro sin anotaciones JPA)
     * a un HealthEventEntity que puede ser persistido por Spring Data JPA.
     * </p>
     * <p>
     * Transformaciones aplicadas:
     * <ul>
     *   <li>HealthEventType enum → HealthEventEntity.EventType enum usando conversión directa</li>
     *   <li>Todos los demás campos se copian directamente (UUIDs, LocalDate, BigDecimal, Instants)</li>
     * </ul>
     * </p>
     *
     * @param healthEvent la entidad de dominio HealthEvent a convertir (no debe ser null)
     * @return una nueva instancia de HealthEventEntity con todos los campos mapeados
     * @throws IllegalArgumentException si healthEvent es null
     */
    public static HealthEventEntity toEntity(HealthEvent healthEvent) {
        if (healthEvent == null) {
            throw new IllegalArgumentException("HealthEvent no puede ser null");
        }
        
        return HealthEventEntity.builder()
            .eventId(healthEvent.getEventId())
            .animalId(healthEvent.getAnimalId())
            .eventType(mapDomainEventTypeToEntity(healthEvent.getTipoEvento()))
            .fechaEvento(healthEvent.getFecha())
            .descripcion(healthEvent.getDescripcion())
            .costo(healthEvent.getCosto())
            .veterinarioId(null) // Old field, not used in new structure
            .createdAt(java.time.Instant.from(healthEvent.getRecordedAt().atZone(java.time.ZoneId.systemDefault())))
            .createdBy(healthEvent.getRecordedBy())
            .build();
    }
    
    /**
     * Convierte una entidad JPA HealthEventEntity a una entidad de dominio HealthEvent.
     * <p>
     * Este método transforma un HealthEventEntity recuperado de la base de datos
     * a un objeto HealthEvent del dominio puro, listo para ser usado por los casos de uso.
     * </p>
     * <p>
     * Transformaciones aplicadas:
     * <ul>
     *   <li>HealthEventEntity.EventType enum → HealthEventType enum usando conversión directa</li>
     *   <li>Todos los demás campos se copian directamente (UUIDs, LocalDate, BigDecimal, Instants)</li>
     * </ul>
     * </p>
     *
     * @param entity la entidad JPA HealthEventEntity a convertir (no debe ser null)
     * @return una nueva instancia de HealthEvent construida mediante el Builder
     * @throws IllegalArgumentException si entity es null o si eventType no es válido
     */
    public static HealthEvent toDomain(HealthEventEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("HealthEventEntity no puede ser null");
        }
        
        return new HealthEvent.Builder()
            .eventId(entity.getEventId())
            .animalId(entity.getAnimalId())
            .tipoEvento(mapEntityEventTypeToDomain(entity.getEventType()))
            .fecha(entity.getFechaEvento())
            .descripcion(entity.getDescripcion())
            .costo(entity.getCosto())
            .recordedAt(java.time.LocalDateTime.ofInstant(entity.getCreatedAt(), java.time.ZoneId.systemDefault()))
            .recordedBy(entity.getCreatedBy())
            .build();
    }
    
    /**
     * Convierte HealthEventType (dominio) a HealthEventEntity.EventType (entidad).
     *
     * @param domainEventType el tipo de evento del dominio
     * @return el tipo de evento de la entidad correspondiente
     * @throws IllegalArgumentException si el tipo de evento no es válido
     */
    private static HealthEventEntity.EventType mapDomainEventTypeToEntity(HealthEventType domainEventType) {
        if (domainEventType == null) {
            throw new IllegalArgumentException("HealthEventType no puede ser null");
        }
        
        return switch (domainEventType) {
            case Vacunacion -> HealthEventEntity.EventType.VACCINATION;
            case Desparasitacion -> HealthEventEntity.EventType.TREATMENT; // Mapped to TREATMENT for now
            case Tratamiento -> HealthEventEntity.EventType.TREATMENT;
            case Diagnostico -> HealthEventEntity.EventType.DIAGNOSIS;
            case Cirugia -> HealthEventEntity.EventType.TREATMENT; // Mapped to TREATMENT for now
            case Revision -> HealthEventEntity.EventType.DIAGNOSIS; // Mapped to DIAGNOSIS for now
            case Birth -> HealthEventEntity.EventType.BIRTH;
        };
    }
    
    /**
     * Convierte HealthEventEntity.EventType (entidad) a HealthEventType (dominio).
     *
     * @param entityEventType el tipo de evento de la entidad
     * @return el tipo de evento del dominio correspondiente
     * @throws IllegalArgumentException si el tipo de evento no es válido
     */
    private static HealthEventType mapEntityEventTypeToDomain(HealthEventEntity.EventType entityEventType) {
        if (entityEventType == null) {
            throw new IllegalArgumentException("EventType no puede ser null");
        }
        
        return switch (entityEventType) {
            case VACCINATION -> HealthEventType.Vacunacion;
            case TREATMENT -> HealthEventType.Tratamiento;
            case BIRTH -> HealthEventType.Birth;
            case DIAGNOSIS -> HealthEventType.Diagnostico;
        };
    }
}
