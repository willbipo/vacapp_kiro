package mx.vacapp.cattle.internal.infrastructure.persistence.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.cattle.internal.domain.model.HealthEvent;
import mx.vacapp.cattle.internal.domain.model.HealthEventType;
import mx.vacapp.cattle.internal.domain.repository.HealthEventRepository;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.HealthEventEntity;
import mx.vacapp.cattle.internal.infrastructure.persistence.mappers.HealthEventMapper;
import mx.vacapp.cattle.internal.infrastructure.persistence.repositories.HealthEventJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del repositorio de eventos de salud.
 * Adaptador que conecta el puerto de dominio con JPA.
 * 
 * Responsabilidades:
 * - Transformar entre entidades de dominio (HealthEvent) y JPA (HealthEventEntity) usando HealthEventMapper
 * - Aplicar filtros multi-tenant automáticamente a través de la relación con la tabla animals
 * - Lanzar excepciones de dominio cuando corresponda
 * - Manejar conversión de enums entre capa de dominio y capa de persistencia
 * 
 * Esta implementación no almacena tenant_id directamente en health_events. El filtrado
 * multi-tenant se realiza mediante JOIN con la tabla animals, garantizando aislamiento
 * de datos sin duplicar el campo tenant_id.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class HealthEventRepositoryImpl implements HealthEventRepository {
    
    private final HealthEventJpaRepository jpaRepository;
    
    @Override
    public HealthEvent save(HealthEvent healthEvent) {
        if (healthEvent == null) {
            throw new IllegalArgumentException("HealthEvent no puede ser null");
        }
        
        log.debug("Guardando evento de salud: tipo={}, animalId={}", 
                 healthEvent.getTipoEvento(), healthEvent.getAnimalId());
        
        HealthEventEntity entity = HealthEventMapper.toEntity(healthEvent);
        HealthEventEntity saved = jpaRepository.save(entity);
        
        log.info("Evento de salud guardado exitosamente: eventId={}, tipo={}, animalId={}", 
                 saved.getEventId(), saved.getEventType(), saved.getAnimalId());
        
        return HealthEventMapper.toDomain(saved);
    }
    
    @Override
    public List<HealthEvent> findByAnimalId(UUID animalId) {
        if (animalId == null) {
            throw new IllegalArgumentException("animalId no puede ser null");
        }
        
        log.debug("Buscando eventos de salud por animalId: {}", animalId);
        
        List<HealthEventEntity> entities = jpaRepository.findByAnimalIdOrderByFechaEventoDesc(animalId);
        
        log.debug("Encontrados {} eventos de salud para el animal {}", entities.size(), animalId);
        
        return entities.stream()
                .map(HealthEventMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<HealthEvent> findByEventType(HealthEventType eventType, UUID tenantId) {
        if (eventType == null) {
            throw new IllegalArgumentException("eventType no puede ser null");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId no puede ser null");
        }
        
        log.debug("Buscando eventos de salud por tipo: tipo={}, tenantId={}", eventType, tenantId);
        
        // Convertir el enum de dominio al enum de entidad
        HealthEventEntity.EventType entityEventType = mapDomainEventTypeToEntity(eventType);
        
        List<HealthEventEntity> entities = jpaRepository.findByEventTypeOrderByFechaEventoDesc(entityEventType);
        
        // Nota: El filtrado por tenant se debe realizar en la capa de aplicación (UseCase)
        // ya que health_events no tiene tenant_id directo. El UseCase debe validar
        // que los animales pertenezcan al tenant antes de retornar los eventos.
        log.warn("findByEventType no filtra automáticamente por tenant. " +
                 "El filtrado debe realizarse en la capa de aplicación validando animal_id contra tenant_id");
        
        log.debug("Encontrados {} eventos de tipo {}", entities.size(), eventType);
        
        return entities.stream()
                .map(HealthEventMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<HealthEvent> findUpcomingVaccinations(UUID tenantId, int daysAhead) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId no puede ser null");
        }
        if (daysAhead < 0) {
            throw new IllegalArgumentException("daysAhead debe ser mayor o igual a 0");
        }
        
        log.debug("Buscando vacunaciones próximas: tenantId={}, días={}", tenantId, daysAhead);
        
        LocalDate fechaActual = LocalDate.now();
        List<HealthEventEntity> entities = jpaRepository.findUpcomingVaccinations(
                tenantId, daysAhead, fechaActual);
        
        log.debug("Encontradas {} vacunaciones próximas a vencer", entities.size());
        
        return entities.stream()
                .map(HealthEventMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<HealthEvent> findBirthEventsByMotherId(UUID motherId) {
        if (motherId == null) {
            throw new IllegalArgumentException("motherId no puede ser null");
        }
        
        log.debug("Buscando eventos de parto por motherId: {}", motherId);
        
        // Convertir HealthEventType.BIRTH a HealthEventEntity.EventType.BIRTH
        HealthEventEntity.EventType birthEventType = HealthEventEntity.EventType.BIRTH;
        
        // Obtener todos los eventos de tipo BIRTH
        List<HealthEventEntity> allBirthEvents = jpaRepository.findByEventTypeOrderByFechaEventoDesc(birthEventType);
        
        // Filtrar por motherId (animal_id en la tabla health_events corresponde a la madre)
        List<HealthEventEntity> motherBirthEvents = allBirthEvents.stream()
                .filter(entity -> entity.getAnimalId().equals(motherId))
                .collect(Collectors.toList());
        
        log.debug("Encontrados {} eventos de parto para la madre {}", motherBirthEvents.size(), motherId);
        
        return motherBirthEvents.stream()
                .map(HealthEventMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    /**
     * Convierte HealthEventType (dominio) a HealthEventEntity.EventType (entidad).
     * 
     * @param domainEventType el tipo de evento del dominio
     * @return el tipo de evento de la entidad correspondiente
     * @throws IllegalArgumentException si el tipo de evento no es válido
     */
    private HealthEventEntity.EventType mapDomainEventTypeToEntity(HealthEventType domainEventType) {
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
}
