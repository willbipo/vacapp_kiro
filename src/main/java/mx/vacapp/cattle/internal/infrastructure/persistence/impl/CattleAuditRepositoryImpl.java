package mx.vacapp.cattle.internal.infrastructure.persistence.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.repository.CattleAuditRepository;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.AnimalEntity;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.CattleAuditEntity;
import mx.vacapp.cattle.internal.infrastructure.persistence.repositories.AnimalJpaRepository;
import mx.vacapp.cattle.internal.infrastructure.persistence.repositories.CattleAuditJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del puerto CattleAuditRepository usando JPA.
 * 
 * Esta clase es parte de la capa de infraestructura y adapta las operaciones
 * de auditoría del dominio a operaciones JPA concretas. Transforma entre
 * entidades de dominio (CattleAudit) y entidades JPA (CattleAuditEntity).
 * 
 * Utiliza ObjectMapper de Jackson para serializar/deserializar valores
 * antiguos y nuevos a formato JSON para almacenamiento eficiente y flexible.
 * 
 * IMPORTANTE: Todos los registros de auditoría son inmutables - no se
 * pueden modificar después de su creación.
 */
@Repository
@RequiredArgsConstructor
public class CattleAuditRepositoryImpl implements CattleAuditRepository {
    
    private final CattleAuditJpaRepository jpaRepository;
    private final AnimalJpaRepository animalJpaRepository;
    private final ObjectMapper objectMapper;
    
    @Override
    public void log(CattleAudit audit) {
        if (audit == null) {
            throw new IllegalArgumentException("Audit cannot be null");
        }
        
        CattleAuditEntity entity = CattleAuditEntity.builder()
            .auditId(audit.auditId() != null ? audit.auditId() : UUID.randomUUID())
            .animalId(audit.animalId())
            .operationType(mapOperationType(audit.operationType()))
            .timestamp(audit.timestamp() != null ? audit.timestamp() : Instant.now())
            .modifiedBy(audit.modifiedBy())
            .tenantId(audit.tenantId())
            .oldValues(audit.oldValues())
            .newValues(audit.newValues())
            .reason(audit.reason())
            .build();
        
        jpaRepository.save(entity);
    }
    
    @Override
    public void logAnimalCreation(UUID animalId, Animal animal, UUID createdBy) {
        if (animalId == null) {
            throw new IllegalArgumentException("Animal ID cannot be null");
        }
        if (animal == null) {
            throw new IllegalArgumentException("Animal cannot be null");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("Created by user ID cannot be null");
        }
        
        Map<String, Object> animalData = buildAnimalDataMap(animal);
        String newValuesJson = convertToJson(animalData);
        
        CattleAudit audit = new CattleAudit(
            UUID.randomUUID(),
            animalId,
            AuditOperationType.CREATE,
            Instant.now(),
            createdBy,
            animal.getTenantId(),
            null,  // No hay valores antiguos en creación
            newValuesJson,
            null
        );
        
        log(audit);
    }
    
    @Override
    public void logAnimalUpdate(UUID animalId, Map<String, Object> oldValues, 
                                Map<String, Object> newValues, UUID modifiedBy, UUID tenantId) {
        if (animalId == null) {
            throw new IllegalArgumentException("Animal ID cannot be null");
        }
        if (oldValues == null) {
            throw new IllegalArgumentException("Old values cannot be null");
        }
        if (newValues == null) {
            throw new IllegalArgumentException("New values cannot be null");
        }
        if (modifiedBy == null) {
            throw new IllegalArgumentException("Modified by user ID cannot be null");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        
        String oldValuesJson = convertToJson(oldValues);
        String newValuesJson = convertToJson(newValues);
        
        CattleAudit audit = new CattleAudit(
            UUID.randomUUID(),
            animalId,
            AuditOperationType.UPDATE,
            Instant.now(),
            modifiedBy,
            tenantId,
            oldValuesJson,
            newValuesJson,
            null
        );
        
        log(audit);
    }
    
    @Override
    public void logStatusChange(UUID animalId, String oldStatus, String newStatus, 
                                UUID modifiedBy, String reason) {
        if (animalId == null) {
            throw new IllegalArgumentException("Animal ID cannot be null");
        }
        if (oldStatus == null) {
            throw new IllegalArgumentException("Old status cannot be null");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }
        if (modifiedBy == null) {
            throw new IllegalArgumentException("Modified by user ID cannot be null");
        }
        
        // Obtener el tenantId del animal
        UUID tenantId = getTenantIdFromAnimal(animalId);
        
        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("status", oldStatus);
        
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("status", newStatus);
        
        String oldValuesJson = convertToJson(oldValues);
        String newValuesJson = convertToJson(newValues);
        
        CattleAudit audit = new CattleAudit(
            UUID.randomUUID(),
            animalId,
            AuditOperationType.CHANGE_STATUS,
            Instant.now(),
            modifiedBy,
            tenantId,
            oldValuesJson,
            newValuesJson,
            reason
        );
        
        log(audit);
    }
    
    @Override
    public void logMovement(UUID animalId, UUID oldPotreroId, UUID newPotreroId, UUID modifiedBy) {
        if (animalId == null) {
            throw new IllegalArgumentException("Animal ID cannot be null");
        }
        if (newPotreroId == null) {
            throw new IllegalArgumentException("New potrero ID cannot be null");
        }
        if (modifiedBy == null) {
            throw new IllegalArgumentException("Modified by user ID cannot be null");
        }
        
        // Obtener el tenantId del animal
        UUID tenantId = getTenantIdFromAnimal(animalId);
        
        Map<String, Object> oldValues = new HashMap<>();
        if (oldPotreroId != null) {
            oldValues.put("potrero_id", oldPotreroId.toString());
        }
        
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("potrero_id", newPotreroId.toString());
        
        String oldValuesJson = oldPotreroId != null ? convertToJson(oldValues) : null;
        String newValuesJson = convertToJson(newValues);
        
        CattleAudit audit = new CattleAudit(
            UUID.randomUUID(),
            animalId,
            AuditOperationType.MOVE_PASTURE,
            Instant.now(),
            modifiedBy,
            tenantId,
            oldValuesJson,
            newValuesJson,
            null
        );
        
        log(audit);
    }
    
    @Override
    public List<CattleAudit> findByAnimalId(UUID animalId, int offset, int limit) {
        if (animalId == null) {
            throw new IllegalArgumentException("Animal ID cannot be null");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be non-negative");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be at least 1");
        }
        
        PageRequest pageRequest = PageRequest.of(offset / limit, limit);
        List<CattleAuditEntity> entities = jpaRepository.findByAnimalIdOrderByTimestampDesc(
            animalId, 
            pageRequest
        );
        
        return entities.stream()
            .map(this::mapToDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<CattleAudit> findByOperationType(AuditOperationType operationType, UUID tenantId, 
                                                  int offset, int limit) {
        if (operationType == null) {
            throw new IllegalArgumentException("Operation type cannot be null");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be non-negative");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be at least 1");
        }
        
        PageRequest pageRequest = PageRequest.of(offset / limit, limit);
        CattleAuditEntity.OperationType entityOperationType = mapOperationType(operationType);
        
        List<CattleAuditEntity> entities = jpaRepository.findByOperationTypeAndTenantIdOrderByTimestampDesc(
            entityOperationType,
            tenantId,
            pageRequest
        );
        
        return entities.stream()
            .map(this::mapToDomain)
            .collect(Collectors.toList());
    }
    
    /**
     * Construye un mapa con los datos completos del animal para auditoría.
     * 
     * @param animal entidad de dominio Animal
     * @return mapa con todos los campos relevantes del animal
     */
    private Map<String, Object> buildAnimalDataMap(Animal animal) {
        Map<String, Object> data = new HashMap<>();
        
        data.put("animal_id", animal.getAnimalId().toString());
        data.put("arete", animal.getArete());
        
        if (animal.getAreteAnterior() != null) {
            data.put("arete_anterior", animal.getAreteAnterior());
        }
        
        data.put("sexo", animal.getSexo().getValue());
        data.put("raza", animal.getRaza().getValue());
        data.put("fecha_nacimiento", animal.getFechaNacimiento().toString());
        data.put("meses", animal.getMeses());
        
        if (animal.getFechaAretado() != null) {
            data.put("fecha_aretado", animal.getFechaAretado().toString());
        }
        
        data.put("tipo", animal.getTipo().getValue());
        data.put("status", animal.getStatus().getValue());
        
        if (animal.getFolioReemo() != null) {
            data.put("folio_reemo", animal.getFolioReemo());
        }
        
        if (animal.getNota() != null) {
            data.put("nota", animal.getNota());
        }
        
        if (animal.getMadreId() != null) {
            data.put("madre_id", animal.getMadreId().toString());
        }
        
        if (animal.getPadreId() != null) {
            data.put("padre_id", animal.getPadreId().toString());
        }
        
        data.put("rancho_id", animal.getRanchoId().toString());
        data.put("tenant_id", animal.getTenantId().toString());
        
        if (animal.getFechaVenta() != null) {
            data.put("fecha_venta", animal.getFechaVenta().toString());
        }
        
        if (animal.getPrecioVenta() != null) {
            data.put("precio_venta", animal.getPrecioVenta().toString());
        }
        
        if (animal.getFechaMuerte() != null) {
            data.put("fecha_muerte", animal.getFechaMuerte().toString());
        }
        
        if (animal.getMotivoMuerte() != null) {
            data.put("motivo_muerte", animal.getMotivoMuerte());
        }
        
        return data;
    }
    
    /**
     * Convierte un mapa a formato JSON usando ObjectMapper.
     * 
     * @param data mapa a convertir
     * @return string JSON
     * @throws RuntimeException si la serialización falla
     */
    private String convertToJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al convertir datos a JSON para auditoría", e);
        }
    }
    
    /**
     * Mapea un tipo de operación de dominio a tipo de operación JPA.
     * 
     * @param operationType tipo de operación de dominio
     * @return tipo de operación JPA
     */
    private CattleAuditEntity.OperationType mapOperationType(AuditOperationType operationType) {
        return switch (operationType) {
            case CREATE -> CattleAuditEntity.OperationType.CREATE;
            case UPDATE -> CattleAuditEntity.OperationType.UPDATE;
            case CHANGE_STATUS -> CattleAuditEntity.OperationType.CHANGE_STATUS;
            case MOVE_PASTURE -> CattleAuditEntity.OperationType.MOVE_PASTURE;
            case DELETE -> CattleAuditEntity.OperationType.DELETE;
        };
    }
    
    /**
     * Mapea un tipo de operación JPA a tipo de operación de dominio.
     * 
     * @param operationType tipo de operación JPA
     * @return tipo de operación de dominio
     */
    private AuditOperationType mapOperationTypeToDomain(CattleAuditEntity.OperationType operationType) {
        return switch (operationType) {
            case CREATE -> AuditOperationType.CREATE;
            case UPDATE -> AuditOperationType.UPDATE;
            case CHANGE_STATUS -> AuditOperationType.CHANGE_STATUS;
            case MOVE_PASTURE -> AuditOperationType.MOVE_PASTURE;
            case DELETE -> AuditOperationType.DELETE;
        };
    }
    
    /**
     * Mapea una entidad JPA a un record de dominio.
     * 
     * @param entity entidad JPA
     * @return record de dominio CattleAudit
     */
    private CattleAudit mapToDomain(CattleAuditEntity entity) {
        return new CattleAudit(
            entity.getAuditId(),
            entity.getAnimalId(),
            mapOperationTypeToDomain(entity.getOperationType()),
            entity.getTimestamp(),
            entity.getModifiedBy(),
            entity.getTenantId(),
            entity.getOldValues(),
            entity.getNewValues(),
            entity.getReason()
        );
    }
    
    /**
     * Obtiene el tenantId de un animal específico.
     * 
     * Este método es necesario porque algunos métodos de auditoría (logStatusChange, logMovement)
     * no reciben el tenantId como parámetro en la interfaz del repositorio.
     * 
     * @param animalId UUID del animal
     * @return UUID del tenant al que pertenece el animal
     * @throws IllegalArgumentException si el animal no existe
     */
    private UUID getTenantIdFromAnimal(UUID animalId) {
        AnimalEntity animal = animalJpaRepository.findById(animalId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Animal not found with ID: " + animalId
            ));
        return animal.getTenantId();
    }
}
